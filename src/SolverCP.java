import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;

import java.util.ArrayList;
import java.util.List;

public class SolverCP {

    final Slitherlink puzzle;
    final CpModel model;
    BoolVar[][] horiz;
    BoolVar[][] vert;
    IntVar[][] deg;

    public SolverCP(Slitherlink puzzle, CpModel model) {
        Loader.loadNativeLibraries();
        this.puzzle = puzzle;
        this.model = model;
    }

    public void buildModel(){
        int h = puzzle.h;
        int w = puzzle.w;

        // create BoolVars
        horiz = new BoolVar[h+1][w];
        vert = new BoolVar[h][w+1];

        // horizontal edges
        for (int r = 0; r < h + 1; r++) {
            for (int c = 0; c < w; c++) {
                horiz[r][c] = model.newBoolVar("H_" + r + "_" + c);
            }
        }

        // vertical edges
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w + 1; c++) {
                vert[r][c] = model.newBoolVar("V_" + r + "_" + c);
            }
        }

        // constraint: sums = adjacent edges
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                if (puzzle.clue[r][c] == -1) continue;

                List<BoolVar> adj = new ArrayList<>(4);
                adj.add(horiz[r][c]);
                adj.add(vert[r][c]);
                adj.add(horiz[r+1][c]);
                adj.add(vert[r][c+1]);

                model.addEquality(LinearExpr.sum(adj.toArray(new BoolVar[0])), puzzle.clue[r][c]);

            }
        }

        // create IntVars for degrees of vertices as helper variables
        deg = new IntVar[h+1][w+1];
        for (int r = 0; r <= h; r++) {
            for (int c = 0; c <= w; c++) {

                // deg <= 2 to avoid branching and intersecting
                deg[r][c] = model.newIntVar(0, 2, "deg_" + r + "_" + c);

                List<BoolVar> edges = new ArrayList<>(4);
                if (r > 0) edges.add(vert[r - 1][c]);          // up
                if (c < w) edges.add(horiz[r][c]);          // right
                if (r < h) edges.add(vert[r][c]);          // down
                if (c > 0) edges.add(horiz[r][c - 1]);          // left


                // deg should be equal to edges of the vertex
                model.addEquality(deg[r][c], LinearExpr.sum(edges.toArray(new BoolVar[0])));

                // deg can only be 0 or 2 for a solved state
                model.addDifferent(deg[r][c], 1);
            }
        }

        // missing single loop constraint
    }

    public void solve(){
        buildModel();

        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE){

            for (int r = 0; r <= puzzle.h; r++) {
                for (int c = 0; c < puzzle.w; c++) {
                    boolean value = solver.booleanValue(horiz[r][c]);
                    puzzle.setHoriz(r, c, value);
                }
            }

            for (int r = 0; r < puzzle.h; r++) {
                for (int c = 0; c <= puzzle.w; c++) {
                    boolean value = solver.booleanValue(vert[r][c]);
                    puzzle.setVert(r, c, value);
                }
            }

            puzzle.solved = true;
            return;
        } else {
            puzzle.solved = false;

            return;
        }
    }
}
