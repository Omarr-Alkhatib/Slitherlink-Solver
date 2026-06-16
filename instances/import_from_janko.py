from urllib.request import urlopen
from html.parser import HTMLParser
import html as html_lib
import os
import re
import time


BASE_URL = "https://www.janko.at/Raetsel/Slitherlink"
N_INSTANCES = 1230

PROBLEM_DIR = "instances/janko/problem"
SOLUTION_DIR = "instances/janko/solution"

os.makedirs(PROBLEM_DIR, exist_ok=True)
os.makedirs(SOLUTION_DIR, exist_ok=True)


# ---------------------------------------------------------
# Parse difficulty index
# ---------------------------------------------------------

class DifficultyIndexParser(HTMLParser):

    def __init__(self):
        super().__init__()
        self.tokens = []
        self.in_link = False
        self.current_href = None

    def handle_starttag(self, tag, attrs):
        if tag == "a":
            self.in_link = True
            self.current_href = dict(attrs).get("href")

    def handle_endtag(self, tag):
        if tag == "a":
            self.in_link = False
            self.current_href = None

    def handle_data(self, data):
        text = html_lib.unescape(data).strip()

        if not text:
            return

        if self.in_link and self.current_href:
            if re.fullmatch(r"\d+", text):
                self.tokens.append(("link", int(text), self.current_href))
        else:
            self.tokens.append(("text", text, None))


def download_html(url):
    with urlopen(url) as page:
        return page.read().decode("utf-8")


def parse_difficulty_index():
    index_url = f"{BASE_URL}/index-3.htm"
    html = download_html(index_url)

    parser = DifficultyIndexParser()
    parser.feed(html)

    difficulty = {}
    current_difficulty = None

    for kind, value, href in parser.tokens:

        if kind == "text":

            text = value.strip()

            if text == "leicht":
                current_difficulty = 0

            elif text == "schwer":
                current_difficulty = 9

            elif re.fullmatch(r"[1-9]", text):
                current_difficulty = int(text)

        elif kind == "link" and current_difficulty is not None:

            puzzle_number = value

            # only store real puzzle links
            if href.endswith(".a.htm"):
                difficulty[puzzle_number] = current_difficulty

    return difficulty


# ---------------------------------------------------------
# Parse individual puzzle pages
# ---------------------------------------------------------

def parse_size(html):
    if "rows " in html:
        rows = int(html.split("rows ")[1].split("\n")[0])
        cols = int(html.split("cols ")[1].split("\n")[0])
    else:
        size = int(html.split("size ")[1].split("\n")[0])
        rows = size
        cols = size

    return rows, cols


def extract_section(html, section_name, rows):
    marker = f"[{section_name}]"

    pos = html.rfind(marker)

    if pos == -1:
        raise ValueError(f"Section {marker} not found")

    lines = html[pos:].splitlines()

    return lines[1:rows + 1]


def normalize_problem_line(line):
    tokens = line.strip().split()

    normalized = []

    for token in tokens:
        if token in {"0", "1", "2", "3"}:
            normalized.append(token)
        else:
            normalized.append("-")

    return " ".join(normalized)


def import_puzzle(num, difficulty_map):
    url = f"{BASE_URL}/{num:04}.a.htm"

    html = download_html(url)

    rows, cols = parse_size(html)

    problem_lines = extract_section(html, "problem", rows)
    solution_lines = extract_section(html, "solution", rows)

    problem_lines = [
        normalize_problem_line(line)
        for line in problem_lines
    ]

    difficulty = difficulty_map.get(num, -1)

    problem_path = f"{PROBLEM_DIR}/janko{num:04}.txt"
    solution_path = f"{SOLUTION_DIR}/solution{num:04}.txt"

    with open(problem_path, "w", encoding="utf-8") as file:
        file.write("[size]\n")
        file.write(f"{rows} {cols}\n")
        file.write("[cells]\n")
        file.write("\n".join(problem_lines))
        file.write("\n[difficulty]\n")
        file.write(f"{difficulty}\n")

    with open(solution_path, "w", encoding="utf-8") as file:
        file.write(f"{rows} {cols}\n")
        file.write("\n".join(solution_lines))
        file.write("\n")

    return difficulty


# ---------------------------------------------------------
# Main import
# ---------------------------------------------------------

difficulty_map = parse_difficulty_index()

print(f"Found difficulty labels for {len(difficulty_map)} puzzles.")

missing = []

for num in range(1, N_INSTANCES + 1):

    difficulty = import_puzzle(num, difficulty_map)

    if difficulty == -1:
        missing.append(num)

    print(f"Imported {num:04} difficulty={difficulty}")

    # Be polite to the website
    time.sleep(0.05)


with open("instances/janko/janko_difficulties.csv", "w", encoding="utf-8") as file:
    file.write("instance,difficulty\n")

    for num in range(1, N_INSTANCES + 1):
        diff = difficulty_map.get(num, -1)
        file.write(f"janko{num:04},{diff}\n")


print()
print("Done.")

if missing:
    print("Missing difficulty labels:")
    print(missing)