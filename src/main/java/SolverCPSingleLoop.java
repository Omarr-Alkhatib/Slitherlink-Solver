import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.util.ArrayList;
import java.util.List;

public class SolverCPSingleLoop {

    final Slitherlink puzzle;
    final CpModel model;
    IntVar[] edges;
    IntVar[] edges_abs;

    public SolverCPSingleLoop(Slitherlink puzzle) {
        Loader.loadNativeLibraries();
        this.puzzle = puzzle;
        this.model = new CpModel();
    }

    public void buildModel(){

        // create BoolVars for edges

        // directed edges
        edges = new IntVar[puzzle.edges.length];

        // absolute value of directed edges, i.e. undirected
        edges_abs = new IntVar[edges.length];

        BoolVar[] edges_pos = new BoolVar[edges.length];
        BoolVar[] edges_neg = new BoolVar[edges.length];


        for (int e = 0; e < edges.length; e++) {
            edges[e] = model.newIntVar(-1, 1, "Edge_" + e);
            edges_abs[e] = model.newIntVar(0, 1, "absEdge_" + e);

            model.addAbsEquality(edges_abs[e], edges[e]);

            // positivity of edges
            edges_pos[e] = model.newBoolVar("posEdge_" + e);
            model.addEquality(edges[e], 1).onlyEnforceIf(edges_pos[e]);
            model.addDifferent(edges[e], 1).onlyEnforceIf(edges_pos[e].not());

            // negativity of edges
            edges_neg[e] = model.newBoolVar("negEdge_" + e);
            model.addEquality(edges[e], -1).onlyEnforceIf(edges_neg[e]);
            model.addDifferent(edges[e], -1).onlyEnforceIf(edges_neg[e].not());
        }


        // constraint: sums = adjacent edges
        for (int i = 0; i < puzzle.clue.length; i++) {
            if (puzzle.clue[i] == -1) continue;

            List<IntVar> adj = new ArrayList<>(4);

            for (int e : puzzle.clueEdges[i]) adj.add(edges_abs[e]);

            model.addEquality(LinearExpr.sum(adj.toArray(new IntVar[0])), puzzle.clue[i]);
        }


        IntVar[] deg = new IntVar[puzzle.degree.length];
        IntVar[] x = new IntVar[deg.length];
        BoolVar[] deg_pos = new BoolVar[deg.length];
        BoolVar[] x_one = new BoolVar[deg.length];

        List<BoolVar> sum_x_one = new ArrayList<>();


        for (int i = 0; i < deg.length; i++) {

            // deg <= 2 to avoid branching and intersecting
            deg[i] = model.newIntVar(0, 2, "deg_" + i);

            // deg can only be 0 or 2 for a solved state
            model.addDifferent(deg[i], 1);

            List<IntVar> adj = new ArrayList<>(4);

            for (int e : puzzle.vertexEdges[i]) {
                if (e == -1) continue;

                adj.add(edges_abs[e]);
            }

            // deg should be equal to edges of the vertex
            model.addEquality(deg[i], LinearExpr.sum(adj.toArray(new IntVar[0])));

            // variable for positivity of deg
            deg_pos[i] = model.newBoolVar("deg_pos_" + i);
            model.addEquality(deg[i], 2).onlyEnforceIf(deg_pos[i]);
            model.addEquality(deg[i],0).onlyEnforceIf(deg_pos[i].not());

            // constraint : incoming edges = outgoing edges
            LinearExprBuilder builder = LinearExpr.newBuilder();

            for (int e : puzzle.vertexEdges[i]) {
                if (e == -1) continue;

                if (puzzle.edgeVertices[e][0] == i) builder.addTerm(edges[e], -1);
                else builder.addTerm(edges[e], 1);
            }

            model.addEquality(builder.build(), 0).onlyEnforceIf(deg_pos[i]);



            // variable for numbering the vertices
            x[i] = model.newIntVar(0, deg.length, "x_" + i);

            // x is positive iff deg is positive
            model.addGreaterOrEqual(x[i], 1).onlyEnforceIf(deg_pos[i]);
            model.addEquality(x[i], 0).onlyEnforceIf(deg_pos[i].not());

            // variable for checking if x is 1, i.e. the vertex is the start of the loop
            x_one[i] = model.newBoolVar("x_one_" + i);
            model.addEquality(x[i], 1).onlyEnforceIf(x_one[i]);
            model.addDifferent(x[i], 1).onlyEnforceIf(x_one[i].not());


            sum_x_one.add(x_one[i]);
        }


        // sum of all x_one is 1, i.e. there is one starting node, or one loop
        model.addEquality(LinearExpr.sum(sum_x_one.toArray(new BoolVar[0])), 1);

        // comparison of each vertex with its neighbors using x
        for (int e = 0; e < edges.length; e++) {

            int from = puzzle.edgeVertices[e][0];
            int to   = puzzle.edgeVertices[e][1];

            // positive edges: down or right
            BoolVar gt = model.newBoolVar("gt_" + e);

            model.addGreaterThan(x[to], x[from]).onlyEnforceIf(gt);

            model.addBoolOr(new Literal[]{gt, x_one[to]}).onlyEnforceIf(edges_pos[e]);

            // negative edges: up or left
            BoolVar lt = model.newBoolVar("lt_" + e);

            model.addGreaterThan(x[from], x[to]).onlyEnforceIf(lt);

            model.addBoolOr(new Literal[]{lt, x_one[from]}).onlyEnforceIf(edges_neg[e]);
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

        // Copy solver values into puzzle
        for (int e = 0; e < edges.length; e++) {
            long value = solver.value(edges[e]);
            if (value != 0) puzzle.setEdge(e, Slitherlink.Edge.ON);
            else puzzle.setEdge(e, Slitherlink.Edge.OFF);
        }


        System.out.println("Wall Time : " + solver.wallTime() + " s");

    }
}



