import pandas as pd
import glob
import matplotlib.pyplot as plt
import numpy as np
import os

# =========================
# Load and merge CSV files
# =========================

files = glob.glob("*.csv")

df = pd.concat(
    [pd.read_csv(f) for f in files],
    ignore_index=True
)

df["correct"] = df["correct"].astype(str).str.lower() == "true"
df["timeout"] = df["timeout"].astype(str).str.lower() == "true"

df["correct_within_limit"] = df["correct"] & ~df["timeout"]
df["incorrect_within_limit"] = ~df["correct"] & ~df["timeout"]

solver_order = ["Verifier", "Lazy", "SingleLoop", "DS"]
solver_display_names = {
    "Verifier": "CP-SV",
    "Lazy": "CP-Lazy",
    "SingleLoop": "CP-Normal",
    "DS": "DS"
}

# =========================
# Correctness table
# =========================

correctness = (
    df.groupby("solver")
      .agg(
          total=("instance", "count"),
          correct=("correct_within_limit", "sum"),
          incorrect=("incorrect_within_limit", "sum"),
          timeout=("timeout", "sum")
      )
)

correctness["unsolved"] = (
    correctness["total"]
    - correctness["correct"]
    - correctness["incorrect"]
    - correctness["timeout"]
)

correctness["solved_percent"] = (
    correctness["correct"] / correctness["total"] * 100
)

correctness = correctness.reindex(
    [s for s in solver_order if s in correctness.index]
)

correctness = correctness[
    ["solved_percent", "correct", "incorrect", "unsolved", "timeout"]
]

correctness = correctness.rename(index=solver_display_names)

correctness_latex = correctness.rename(columns={
    "solved_percent": "Solved (\\%)",
    "correct": "Correct",
    "incorrect": "Incorrect",
    "unsolved": "Unsolved",
    "timeout": "Timeout (60s)"
})

print("\nCorrectness table:")
print(correctness_latex)

print("\nCorrectness LaTeX:")
print(
    correctness_latex.to_latex(
        float_format="%.2f",
        column_format="lrrrrr"
    )
)

# =========================
# Runtime performance table
# =========================

solved = df[
    (df["correct"] == True) &
    (df["timeout"] == False)
].copy()

performance = (
    solved.groupby("solver")["runtime_ms"]
          .agg(
              solved_count="count",
              mean="mean",
              median="median",
              p90=lambda x: x.quantile(0.90),
              p99=lambda x: x.quantile(0.99),
              max="max"
          )
)

timeouts = df.groupby("solver")["timeout"].sum()
performance["timeout"] = timeouts

# Convert runtime columns from milliseconds to seconds
for col in ["mean", "median", "p90", "p99", "max"]:
    performance[col] = performance[col] / 1000.0

performance = performance.reindex(
    [s for s in solver_order if s in performance.index]
)

performance = performance.rename(index=solver_display_names)

performance_latex = performance.rename(columns={
    "solved_count": "Solved",
    "mean": "Mean",
    "median": "Median",
    "p90": "P90",
    "p99": "P99",
    "max": "Max",
    "timeout": "Timeout"
})

print("\nRuntime performance table:")
print(performance_latex)

print("\nRuntime performance LaTeX:")
print(
    performance_latex.to_latex(
        float_format="%.3f",
        column_format="lrrrrrrr"
    )
)

# =========================
# Runtime profile / plot
# x-axis: runtime
# y-axis: solved instances
# =========================

plt.figure(figsize=(8, 5))

for solver in solver_order:

    solver_data = solved[solved["solver"] == solver].copy()

    if solver_data.empty:
        continue

    runtimes = solver_data["runtime_ms"].sort_values().to_numpy() / 1000.0
    solved_indices = range(1, len(runtimes) + 1)

    label = solver_display_names.get(solver, solver)

    plt.plot(
        runtimes,
        solved_indices,
        label=label,
        linewidth=1.8
    )

plt.xlabel("Runtime (s)")
plt.ylabel("Solved instances")
plt.title("Runtime profile over solved non-timeout instances")
plt.xscale("log")
plt.grid(True, which="both", linestyle="--", linewidth=0.5)
plt.legend()
plt.tight_layout()

plt.savefig("runtime_profile.pdf")
plt.savefig("runtime_profile.png", dpi=300)

plt.show()


# =========================
# Scalability table
# =========================

df["size_group"] = pd.cut(
    df["cells"],
    bins=[0, 100, 400, 900, float("inf")],
    labels=["<=100", "101-400", "401-900", ">900"]
)

solved = df[
    (df["correct"] == True) &
    (df["timeout"] == False)
].copy()

scalability_median = (
    solved.groupby(["solver", "size_group"], observed=True)["runtime_ms"]
          .median()
          .unstack()
)

# Convert from ms to seconds
scalability_median = scalability_median / 1000.0

scalability_median = scalability_median.reindex(
    [s for s in solver_order if s in scalability_median.index]
)

scalability_median = scalability_median.rename(index=solver_display_names)

print("\nScalability table: median runtime by size group")
print(scalability_median)

print("\nScalability LaTeX:")
print(
    scalability_median.to_latex(
        float_format="%.3f",
        column_format="lrrrr"
    )
)


# =========================
# Scalability 4-panel plot
# =========================

solver_colors = {
    "Verifier": "tab:blue",
    "Lazy": "tab:orange",
    "SingleLoop": "tab:green",
    "DS": "tab:red"
}

fig, axes = plt.subplots(2, 2, figsize=(12, 9))
axes = axes.flatten()

for ax, solver in zip(axes, solver_order):

    solver_data = solved[solved["solver"] == solver].copy()

    if solver_data.empty:
        ax.set_visible(False)
        continue

    label = solver_display_names.get(solver, solver)
    color = solver_colors.get(solver, "tab:blue")

    trend = (
        solver_data.groupby("cells")["runtime_ms"]
                   .median()
                   .reset_index()
                   .sort_values("cells")
    )

    ax.scatter(
        solver_data["cells"],
        solver_data["runtime_ms"] / 1000.0,
        alpha=0.25,
        s=16,
        color=color
    )

    ax.plot(
        trend["cells"],
        trend["runtime_ms"] / 1000.0,
        color=color,
        linewidth=2.5
    )

    ax.set_title(label)
    ax.set_xlabel("Puzzle size (cells)")
    ax.set_ylabel("Runtime (s)")
    ax.set_yscale("log")
    ax.grid(True, which="both", linestyle="--", linewidth=0.5)

plt.tight_layout()
plt.savefig("scalability_4panel.png", dpi=300)
plt.savefig("scalability_4panel.pdf")
plt.show()


# =========================
# DS max LoE bar chart with values
# =========================

import matplotlib.pyplot as plt
import pandas as pd

ds = df[df["solver"] == "DS"].copy()

ds_solved = ds[
    (ds["correct"] == True) &
    (ds["timeout"] == False)
].copy()

def compute_max_loe(row):
    if row["S2"] + row["V2"] + row["A2"] > 0:
        return 2
    elif row["S1"] + row["V1"] + row["A1"] > 0:
        return 1
    else:
        return 0

ds_solved["max_loe"] = ds_solved.apply(compute_max_loe, axis=1)

loe_counts = (
    ds_solved.groupby(["dataset", "max_loe"])
             .size()
             .unstack(fill_value=0)
)

# Make sure all columns exist
for col in [0, 1, 2]:
    if col not in loe_counts.columns:
        loe_counts[col] = 0

loe_counts = loe_counts[[0, 1, 2]]

loe_counts = loe_counts.rename(columns={
    0: "0-LoE",
    1: "1-LoE",
    2: "2-LoE"
})

print("\nMax LoE counts:")
print(loe_counts)

print("\nLaTeX:")
print(loe_counts.to_latex(column_format="lrrr"))

fig, ax = plt.subplots(figsize=(8, 5))

loe_counts.plot(
    kind="bar",
    ax=ax,
    width=0.75
)

ax.set_xlabel("Dataset")
ax.set_ylabel("Number of solved instances")
ax.set_title("Maximum LoE required by DS")
ax.set_xticklabels(ax.get_xticklabels(), rotation=0)
ax.grid(axis="y", linestyle="--", linewidth=0.5)

# Add numbers above bars
for container in ax.containers:
    ax.bar_label(
        container,
        fmt="%d",
        padding=3,
        fontsize=8
    )

# Add a bit of space above labels
max_value = loe_counts.to_numpy().max()
ax.set_ylim(0, max_value * 1.12)

plt.tight_layout()

plt.savefig("ds_max_loe.png", dpi=300)
plt.savefig("ds_max_loe.pdf")

plt.show()


# =========================
# DS shaving and agreement operations per instance
# =========================

# Make sure the relevant columns are numeric
for col in ["V1", "V2", "A1", "A2"]:
    ds_solved[col] = pd.to_numeric(ds_solved[col], errors="coerce").fillna(0)

# Aggregate operations across LoE levels
ds_solved["Shaving"] = ds_solved["V1"] + ds_solved["V2"]
ds_solved["Agreement"] = ds_solved["A1"] + ds_solved["A2"]
ds_solved["HypotheticalOperations"] = ds_solved["Shaving"] + ds_solved["Agreement"]

# Sort instances by total amount of shaving/agreement operations
operation_plot_data = (
    ds_solved.sort_values("HypotheticalOperations")
             .reset_index(drop=True)
             .copy()
)

operation_plot_data["instance_rank"] = range(1, len(operation_plot_data) + 1)

# Summary table
operation_summary = operation_plot_data[["Shaving", "Agreement", "HypotheticalOperations"]].agg(
    ["mean", "median", lambda x: x.quantile(0.90), lambda x: x.quantile(0.99), "max"]
)

operation_summary.index = ["Mean", "Median", "P90", "P99", "Max"]

print("\nDS shaving/agreement operation summary:")
print(operation_summary)

print("\nDS shaving/agreement operation summary LaTeX:")
print(
    operation_summary.to_latex(
        float_format="%.2f",
        column_format="lrrr"
    )
)

# Scatter plot
plt.figure(figsize=(10, 5))

plt.scatter(
    operation_plot_data["instance_rank"],
    operation_plot_data["Shaving"],
    s=12,
    alpha=0.45,
    label="Shaving"
)

plt.scatter(
    operation_plot_data["instance_rank"],
    operation_plot_data["Agreement"],
    s=12,
    alpha=0.45,
    label="Agreement"
)

plt.xlabel("Solved DS instances sorted by shaving/agreement operations")
plt.ylabel("Number of operations")
plt.title("DS shaving and agreement operations per instance")
plt.yscale("symlog", linthresh=1)
plt.grid(True, linestyle="--", linewidth=0.5)
plt.legend()
plt.tight_layout()

plt.savefig("ds_shaving_agreement_operations.png", dpi=300)
plt.savefig("ds_shaving_agreement_operations.pdf")

plt.show()


# =========================
# DS difficulty analysis
# =========================

# Use only DS rows that were solved within the time limit
difficulty_df = df[
    (df["solver"] == "DS") &
    (df["correct"] == True) &
    (df["timeout"] == False)
].copy()

# Convert relevant columns to numeric
difficulty_df["official_difficulty"] = pd.to_numeric(
    difficulty_df["official_difficulty"],
    errors="coerce"
)

difficulty_df["computed_difficulty"] = pd.to_numeric(
    difficulty_df["computed_difficulty"],
    errors="coerce"
)

# Exclude instances without an official difficulty label
# Janko 1221-1230 have -1 and should be excluded here
difficulty_df = difficulty_df[
    difficulty_df["official_difficulty"] >= 0
].copy()

dataset_order = ["Janko", "Fresh", "Tokoton"]

# =========================
# Spearman correlation
# =========================

try:
    from scipy.stats import spearmanr
    scipy_available = True
except ImportError:
    scipy_available = False

spearman_rows = []

for dataset in dataset_order:

    g = difficulty_df[difficulty_df["dataset"] == dataset].copy()

    if g.empty:
        continue

    if scipy_available:
        rho, p_value = spearmanr(
            g["official_difficulty"],
            g["computed_difficulty"]
        )
    else:
        rho = g[["official_difficulty", "computed_difficulty"]].corr(
            method="spearman"
        ).iloc[0, 1]
        p_value = np.nan

    spearman_rows.append({
        "Dataset": dataset,
        "Instances": len(g),
        "Official min": g["official_difficulty"].min(),
        "Official max": g["official_difficulty"].max(),
        "DS min": g["computed_difficulty"].min(),
        "DS max": g["computed_difficulty"].max(),
        "Spearman rho": rho,
        "p-value": p_value
    })

spearman_table = pd.DataFrame(spearman_rows)

print("\nSpearman difficulty correlation:")
print(spearman_table)

print("\nSpearman LaTeX:")
print(
    spearman_table.to_latex(
        index=False,
        float_format="%.3f",
        column_format="lrrrrrrr"
    )
)

# =========================
# DS difficulty boxplots - one figure per dataset
# =========================

ds = df[
    (df["solver"] == "DS") &
    (df["correct"] == True) &
    (df["timeout"] == False)
].copy()

# keep only rows with valid difficulties
ds = ds[
    (ds["official_difficulty"] >= 0) &
    (ds["computed_difficulty"] >= 0)
].copy()


def make_boxplot(dataset_name, figsize=(8, 5)):
    data = ds[ds["dataset"] == dataset_name].copy()

    if data.empty:
        print(f"No data for {dataset_name}")
        return

    # -------------------------
    # Janko and Fresh:
    # one box per exact difficulty
    # -------------------------
    if dataset_name in ["Janko", "Fresh"]:

        difficulties = sorted(data["official_difficulty"].dropna().unique())

        box_data = []
        labels = []

        for d in difficulties:
            values = data.loc[data["official_difficulty"] == d, "computed_difficulty"].dropna()
            if len(values) > 0:
                box_data.append(values.to_list())
                labels.append(str(int(d)))

        plt.figure(figsize=figsize)
        plt.boxplot(box_data, labels=labels)

        plt.xlabel("Official difficulty")
        plt.ylabel("Computed DS difficulty")
        plt.title(f"Computed DS difficulty by official difficulty ({dataset_name})")
        plt.grid(True, axis="y", linestyle="--", linewidth=0.5)
        plt.tight_layout()

        file_base = f"ds_difficulty_{dataset_name.lower()}"
        plt.savefig(file_base + ".png", dpi=300)
        plt.savefig(file_base + ".pdf")
        plt.show()

    # -------------------------
    # Tokoton:
    # group every 3 difficulties together
    # -------------------------
    elif dataset_name == "Tokoton":

        def tokoton_group(d):
            start = ((int(d) - 1) // 3) * 3 + 1
            end = min(start + 2, 32)
            return f"{start}-{end}"

        data["difficulty_group"] = data["official_difficulty"].apply(tokoton_group)

        ordered_labels = []
        start = 1
        while start <= 32:
            end = min(start + 2, 32)
            ordered_labels.append(f"{start}-{end}")
            start += 3

        box_data = []
        labels = []

        for label in ordered_labels:
            values = data.loc[data["difficulty_group"] == label, "computed_difficulty"].dropna()
            if len(values) > 0:
                box_data.append(values.to_list())
                labels.append(label)

        plt.figure(figsize=figsize)
        plt.boxplot(box_data, labels=labels)

        plt.xlabel("Official difficulty")
        plt.ylabel("Computed DS difficulty")
        plt.title("Computed DS difficulty by official difficulty (Tokoton)")
        plt.xticks(rotation=45)
        plt.grid(True, axis="y", linestyle="--", linewidth=0.5)
        plt.tight_layout()

        file_base = "ds_difficulty_tokoton"
        plt.savefig(file_base + ".png", dpi=300)
        plt.savefig(file_base + ".pdf")
        plt.show()


# generate all three figures
make_boxplot("Janko")
make_boxplot("Fresh")
make_boxplot("Tokoton")