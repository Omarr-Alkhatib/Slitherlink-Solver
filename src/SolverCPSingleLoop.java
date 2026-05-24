import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.util.ArrayList;
import java.util.List;

public class SolverCPSingleLoop {

    final Slitherlink puzzle;
    final CpModel model;
    IntVar[][] horiz;
    IntVar[][] vert;



    public SolverCPSingleLoop(Slitherlink puzzle) {
        Loader.loadNativeLibraries();
        this.puzzle = puzzle;
        this.model = new CpModel();
    }

    public void buildModel(){
        int h = puzzle.h;
        int w = puzzle.w;

        horiz = new IntVar[h+1][w];
        vert = new IntVar[h][w+1];

        IntVar[][] abs_horiz = new IntVar[h+1][w];
        IntVar[][] abs_vert = new IntVar[h][w+1];

        BoolVar[][] horiz_pos = new BoolVar[h+1][w];
        BoolVar[][] vert_pos = new BoolVar[h][w+1];
        BoolVar[][] horiz_neg = new BoolVar[h+1][w];
        BoolVar[][] vert_neg = new BoolVar[h][w+1];

        // horizontal edges
        for (int r = 0; r < h + 1; r++) {
            for (int c = 0; c < w; c++) {
                horiz[r][c] = model.newIntVar(-1, 1, "H_" + r + "_" + c);
                abs_horiz[r][c] = model.newIntVar(0, 1, "absH_" + r + "_" + c);
                model.addAbsEquality(abs_horiz[r][c], horiz[r][c]);
                horiz_pos[r][c] = model.newBoolVar("H_pos_" + r + "_" + c);
                model.addEquality(horiz[r][c], 1).onlyEnforceIf(horiz_pos[r][c]);
                model.addDifferent(horiz[r][c], 1).onlyEnforceIf(horiz_pos[r][c].not());
                horiz_neg[r][c] = model.newBoolVar("H_neg_" + r + "_" + c);
                model.addEquality(horiz[r][c], -1).onlyEnforceIf(horiz_neg[r][c]);
                model.addDifferent(horiz[r][c], -1).onlyEnforceIf(horiz_neg[r][c].not());
            }
        }

        // vertical edges
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w + 1; c++) {
                vert[r][c] = model.newIntVar(-1, 1, "V_" + r + "_" + c);
                abs_vert[r][c] = model.newIntVar(0, 1, "absV_" + r + "_" + c);
                model.addAbsEquality(abs_vert[r][c], vert[r][c]);
                vert_pos[r][c] = model.newBoolVar("V_pos_" + r + "_" + c);
                model.addEquality(vert[r][c], 1).onlyEnforceIf(vert_pos[r][c]);
                model.addDifferent(vert[r][c], 1).onlyEnforceIf(vert_pos[r][c].not());
                vert_neg[r][c] = model.newBoolVar("V_neg_" + r + "_" + c);
                model.addEquality(vert[r][c], -1).onlyEnforceIf(vert_neg[r][c]);
                model.addDifferent(vert[r][c], -1).onlyEnforceIf(vert_neg[r][c].not());
            }
        }

        // constraint: sums = adjacent edges
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                if (puzzle.clue[r][c] == -1) continue;

                model.addEquality(LinearExpr.sum(new IntVar[] {abs_horiz[r][c], abs_vert[r][c], abs_horiz[r+1][c], abs_vert[r][c+1]}), puzzle.clue[r][c]);

            }
        }



        IntVar[][] deg = new IntVar[h+1][w+1];
        IntVar[][] x = new IntVar[h+1][w+1];
        BoolVar[][] deg_pos = new BoolVar[h+1][w+1];
        BoolVar[][] x_one = new BoolVar[h+1][w+1];

        List<BoolVar> sum_x_one = new ArrayList<>();


        for (int r = 0; r <= h; r++) {
            for (int c = 0; c <= w; c++) {

                // deg <= 2 to avoid branching and intersecting
                deg[r][c] = model.newIntVar(0, 2, "deg_" + r + "_" + c);

                // deg can only be 0 or 2 for a solved state
                model.addDifferent(deg[r][c], 1);

                List<IntVar> edges = new ArrayList<>(4);
                if (r > 0) edges.add(abs_vert[r - 1][c]);          // up
                if (c < w) edges.add(abs_horiz[r][c]);             // right
                if (r < h) edges.add(abs_vert[r][c]);              // down
                if (c > 0) edges.add(abs_horiz[r][c - 1]);         // left


                // deg should be equal to edges of the vertex
                model.addEquality(deg[r][c], LinearExpr.sum(edges.toArray(new IntVar[0])));

                // variable for positivity of deg
                deg_pos[r][c] = model.newBoolVar("deg_pos_" + r + "_" + c);
                model.addEquality(deg[r][c], 2).onlyEnforceIf(deg_pos[r][c]);
                model.addEquality(deg[r][c],0).onlyEnforceIf(deg_pos[r][c].not());

                // constraint for incoming and outgoing edges
                LinearExprBuilder builder = LinearExpr.newBuilder();

                if (r > 0) builder.addTerm(vert[r-1][c], 1);
                if (c > 0) builder.addTerm(horiz[r][c-1], 1);
                if (r < h) builder.addTerm(vert[r][c], -1);
                if (c < w) builder.addTerm(horiz[r][c], -1);

                model.addEquality(builder.build(), 0).onlyEnforceIf(deg_pos[r][c]);


                // variable for numbering the vertices
                x[r][c] = model.newIntVar(0, (long) (h+1)*(w+1), "x_" + r + "_" + c);

                // x is positive iff deg is positive
                model.addGreaterOrEqual(x[r][c], 1).onlyEnforceIf(deg_pos[r][c]);
                model.addEquality(x[r][c], 0).onlyEnforceIf(deg_pos[r][c].not());

                // variable for checking if x is 1, i.e. the vertex is the start of the loop
                x_one[r][c] = model.newBoolVar("x_one_" + r + "_" + c);
                model.addEquality(x[r][c], 1).onlyEnforceIf(x_one[r][c]);
                model.addDifferent(x[r][c], 1).onlyEnforceIf(x_one[r][c].not());


                sum_x_one.add(x_one[r][c]);
            }
        }


        // sum of all x_one is 1, i.e. there is one starting node, or one loop
        model.addEquality(LinearExpr.sum(sum_x_one.toArray(new BoolVar[0])), 1);



        // comparison of each vertex with its neighbors using x
        for (int r = 0; r <= h; r++){
            for (int c = 0; c <= w; c++){
                if (r > 0) {
                    BoolVar up = model.newBoolVar("Up_" + r + "_" + c);
                    model.addGreaterThan(x[r-1][c], x[r][c]).onlyEnforceIf(up);
                    model.addBoolOr(new Literal[]{up, x_one[r-1][c]}).onlyEnforceIf(vert_neg[r-1][c]);
                }
                if (r < h) {
                    BoolVar down = model.newBoolVar("Down_" + r + "_" + c);
                    model.addGreaterThan(x[r+1][c], x[r][c]).onlyEnforceIf(down);
                    model.addBoolOr(new Literal[]{down, x_one[r+1][c]}).onlyEnforceIf(vert_pos[r][c]);
                }
                if (c > 0) {
                    BoolVar left = model.newBoolVar("Left_" + r + "_" + c);
                    model.addGreaterThan(x[r][c-1], x[r][c]).onlyEnforceIf(left);
                    model.addBoolOr(new Literal[]{left, x_one[r][c-1]}).onlyEnforceIf(horiz_neg[r][c-1]);
                }
                if (c < w) {
                    BoolVar right = model.newBoolVar("Right_" + r + "_" + c);
                    model.addGreaterThan(x[r][c+1], x[r][c]).onlyEnforceIf(right);
                    model.addBoolOr(new Literal[]{right, x_one[r][c+1]}).onlyEnforceIf(horiz_pos[r][c]);
                }
            }
        }


    }

    public void solve(){
        buildModel();

        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);

        if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE){
            System.out.println("No Solution");
            return;
        }

        for (int r = 0; r <= puzzle.h; r++) {
            for (int c = 0; c < puzzle.w; c++) {
                long value = solver.value(horiz[r][c]);
                if (value != 0) puzzle.setHoriz(r, c, Slitherlink.Edge.ON);
            }
        }

        for (int r = 0; r < puzzle.h; r++) {
            for (int c = 0; c <= puzzle.w; c++) {
                long value = solver.value(vert[r][c]);
                if (value != 0) puzzle.setVert(r, c, Slitherlink.Edge.ON);
            }
        }

        puzzle.print();

        System.out.println("Wall Time : " + solver.wallTime() + " s");

    }
}

