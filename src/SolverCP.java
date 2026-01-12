import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.CpSolverSolutionCallback;

import java.util.ArrayList;
import java.util.List;

public class SolverCP {

    public class SolutionPrinter extends CpSolverSolutionCallback {
        private final BoolVar[][] h;
        private final BoolVar[][] v;

        private int solutionCount = 0;

        public SolutionPrinter(BoolVar[][] h,  BoolVar[][] v) {
            this.h = h;
            this.v = v;

        }

        @Override
        public void onSolutionCallback() {
            solutionCount++;

            visited = new boolean[puzzle.h + 1][puzzle.w + 1];

            // Copy solver values into puzzle
            for (int r = 0; r <= puzzle.h; r++) {
                for (int c = 0; c < puzzle.w; c++) {
                    puzzle.setHoriz(r, c, value(h[r][c]) == 1);
                }
            }

            for (int r = 0; r < puzzle.h; r++) {
                for (int c = 0; c <= puzzle.w; c++) {
                    puzzle.setVert(r, c, value(v[r][c]) == 1);
                }
            }

            int loops = loopCount();
            if (loops == 1){
                puzzle.print();
                stopSearch();
            }

        }
    }

    final Slitherlink puzzle;
    final CpModel model;
    BoolVar[][] horiz;
    BoolVar[][] vert;
    IntVar[][] deg;
    boolean[][] visited;

    public SolverCP(Slitherlink puzzle) {
        Loader.loadNativeLibraries();
        this.puzzle = puzzle;
        this.model = new CpModel();

    }

    // helper function for traversing loops and marking vertices as visited
    public void traverse(int r, int c){
        int nr = r;
        int nc = c;
        int prevR = r;
        int prevC = c;
        if (puzzle.horiz[r][c]) {nc = c + 1;} else {nr = r + 1;}

        while (nr != r || nc != c){
            visited[nr][nc] = true;
            if (nc < puzzle.w)
                if (puzzle.horiz[nr][nc])
                    if (nc + 1 != prevC) {
                        prevR = nr;
                        prevC = nc;
                        nc = nc + 1;
                        continue;
                    }
            if (nr < puzzle.h)
                if (puzzle.vert[nr][nc])
                    if (nr + 1 != prevR) {
                        prevR = nr;
                        prevC = nc;
                        nr = nr + 1;
                        continue;
                    }
            if (nc > 0)
                if (puzzle.horiz[nr][nc - 1])
                    if(nc - 1 != prevC) {
                        prevR = nr;
                        prevC = nc;
                        nc = nc - 1;
                        continue;
                    }
            if (nr > 0)
                if (puzzle.vert[nr - 1][nc])
                    if (nr - 1 != prevR) {
                        prevR = nr;
                        prevC = nc;
                        nr = nr - 1;
                    }

        }
    }

    // helper function to count loops
    public int loopCount(){
        int loopCount = 0;
        for (int r = 0; r <= puzzle.h; r++){
            for (int c = 0; c <= puzzle.w; c++){
                if (puzzle.degree[r][c] == 2 && !visited[r][c]){
                    visited[r][c] = true;
                    loopCount++;
                    traverse(r, c);
                }
            }
        }
        return loopCount;
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

    }

    public void solve(){
        buildModel();

        CpSolver solver = new CpSolver();
        solver.getParameters().setEnumerateAllSolutions(true);

        SolutionPrinter cb = new SolutionPrinter(horiz, vert);

        CpSolverStatus status = solver.solve(model, cb);

        System.out.println("  solutions : " + cb.solutionCount);
        System.out.println("  wall time : " + solver.wallTime() + " s");

        /*if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE){

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
            System.out.println("Wall Time : " + solver.wallTime()*1000 + " ms");
        } else {
            puzzle.solved = false;
        }*/
    }
}
