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

public class SolverCPVerifier {

    public class SolutionPrinter extends CpSolverSolutionCallback {
        private final BoolVar[] edges;

        private int solutionCount = 0;

        public SolutionPrinter(BoolVar[] edges) {
            this.edges = edges;

        }

        @Override
        public void onSolutionCallback() {
            solutionCount++;

            // Copy solver values into puzzle
            for (int e = 0; e < edges.length; e++) {
                if (value(edges[e]) == 1) puzzle.setEdge(e, Slitherlink.Edge.ON);
                else puzzle.setEdge(e, Slitherlink.Edge.OFF);
            }

            //puzzle.print();

            if (!loopMultiplicity()) {

                stopSearch();
            }

        }
    }

    final Slitherlink puzzle;
    final CpModel model;
    public double wallTime = 0;
    public boolean timedOut = false;
    BoolVar[] edges;
    IntVar[] deg;
    public int generatedSolutions = 0;

    public SolverCPVerifier(Slitherlink puzzle) {
        Loader.loadNativeLibraries();
        this.puzzle = puzzle;
        this.model = new CpModel();

    }


    // helper function to count loops
    public boolean loopMultiplicity(){
        boolean[] visited = new boolean[puzzle.degree.length];
        boolean moreThanOneLoop = false;
        for (int start = 0; start < visited.length; start++){
            if (puzzle.degree[start] != 2 || visited[start]) continue;

            if (moreThanOneLoop) return true;
            moreThanOneLoop = true;

            int current = start;

            outer:
            while (true){
                visited[current] = true;

                for (int e : puzzle.vertexEdges[current]){
                    if (e == -1 || puzzle.edges[e] != Slitherlink.Edge.ON) continue;

                    int neighbor = (puzzle.edgeVertices[e][0] == current) ? puzzle.edgeVertices[e][1] : puzzle.edgeVertices[e][0];

                    // found the start point
                    if (neighbor == start) break outer;

                    if (!visited[neighbor]) {
                        current = neighbor;
                        break;
                    }
                }
            }

        }
        return false;
    }

    public void buildModel(){

        // create BoolVars for edges
        edges = new BoolVar[puzzle.edges.length];

        for (int e = 0; e < edges.length; e++) {
            edges[e] = model.newBoolVar("Edge_" + e);
        }

        // 1 or more edges are required to form a loop and avoid empty solutions
        model.addGreaterOrEqual(LinearExpr.sum(edges), 1);


        // constraint: sums = adjacent edges
        for (int i = 0; i < puzzle.clue.length; i++) {
            if (puzzle.clue[i] == -1) continue;

            List<BoolVar> adj = new ArrayList<>(1);

            for (int e : puzzle.clueEdges[i]) adj.add(edges[e]);

            model.addEquality(LinearExpr.sum(adj.toArray(new BoolVar[0])), puzzle.clue[i]);
        }

        // create IntVars for degrees of vertices as helper variables
        deg = new IntVar[puzzle.degree.length];

        for (int i = 0; i < deg.length; i++) {

            // deg <= 2 to avoid branching and intersecting
            deg[i] = model.newIntVar(0, 2, "deg_" + i);

            List<BoolVar> inc = new ArrayList<>(4);

            for (int e : puzzle.vertexEdges[i]) {
                if (e == -1) continue;

                inc.add(edges[e]);
            }


            // deg should be equal to edges of the vertex
            model.addEquality(deg[i], LinearExpr.sum(inc.toArray(new BoolVar[0])));

            // deg can only be 0 or 2 for a solved state
            model.addDifferent(deg[i], 1);
        }

    }

    public void solve(){
        buildModel();

        CpSolver solver = new CpSolver();
        solver.getParameters().setEnumerateAllSolutions(true);
        solver.getParameters().setMaxTimeInSeconds(60.0);

        SolutionPrinter cb = new SolutionPrinter(edges);

        CpSolverStatus status = solver.solve(model, cb);

        generatedSolutions = cb.solutionCount;

        wallTime = solver.wallTime();

        timedOut = wallTime >= 60.0 - 0.1;

        System.out.println("  Timeout    : " + timedOut);
        System.out.println("  Solutions : " + cb.solutionCount);
        System.out.println("  Wall time : " + solver.wallTime() + " s");


    }
}