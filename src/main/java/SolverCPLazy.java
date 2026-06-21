import com.google.ortools.Loader;
import com.google.ortools.sat.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SolverCPLazy {

    final Slitherlink puzzle;
    final CpModel model;
    BoolVar[] edges;
    IntVar[] deg;
    double wallTime;
    public int generatedSolutions = 0;

    public SolverCPLazy(Slitherlink puzzle) {
        Loader.loadNativeLibraries();
        this.puzzle = puzzle;
        this.model = new CpModel();
    }


    // helper function to count loops
    public boolean loopCount(){
        boolean[] visited = new boolean[puzzle.degree.length];

        for (int start = 0; start < visited.length; start++){
            if (puzzle.degree[start] != 2 || visited[start]) continue;

            List<Integer> loopEdges = new ArrayList<>();

            int current = start;

            outer:
            while (true){
                visited[current] = true;

                for (int e : puzzle.vertexEdges[current]){
                    if (e == -1) continue;

                    if (puzzle.edges[e] == Slitherlink.Edge.ON){
                        int neighbor = (puzzle.edgeVertices[e][0] == current) ? puzzle.edgeVertices[e][1] : puzzle.edgeVertices[e][0];

                        // found the start point
                        if (neighbor == start) {
                            loopEdges.add(e);
                            break outer;
                        }

                        if (!visited[neighbor]) {
                            loopEdges.add(e);
                            current = neighbor;
                            break;
                        }

                    }
                }
            }

            // test loop edges in a copy board
            Slitherlink test = new Slitherlink(puzzle.h, puzzle.w, puzzle.clue);
            Arrays.fill(test.edges, Slitherlink.Edge.OFF);

            for (int e : loopEdges) {
                test.setEdge(e, Slitherlink.Edge.ON);
            }

            if (test.checkAllSums()){
                puzzle.copyState(test);
                return true;
            }

            BoolVar[] edgeVars = new BoolVar[loopEdges.size()];

            for (int i = 0; i < loopEdges.size(); i++) {
                edgeVars[i] = edges[loopEdges.get(i)];
            }

            model.addLessThan(LinearExpr.sum(edgeVars), loopEdges.size());
        }
        return false;
    }

    public void buildModel(){

        // create BoolVars for edges
        edges = new BoolVar[puzzle.edges.length];

        for (int e = 0; e < edges.length; e++) {
            edges[e] = model.newBoolVar("Edge_" + e);
        }

        // 1 edge is required to form a loop and avoid empty solutions
        model.addGreaterOrEqual(LinearExpr.sum(edges), 1);


        // constraint: sums = adjacent edges
        for (int i = 0; i < puzzle.clue.length; i++) {
            if (puzzle.clue[i] == -1) continue;

            List<BoolVar> adj = new ArrayList<>(4);

            for (int e : puzzle.clueEdges[i]) adj.add(edges[e]);

            model.addEquality(LinearExpr.sum(adj.toArray(new BoolVar[0])), puzzle.clue[i]);
        }

        // create IntVars for degrees of vertices as helper variables
        deg = new IntVar[puzzle.degree.length];

        for (int i = 0; i < deg.length; i++) {

            // deg <= 2 to avoid branching and intersecting
            deg[i] = model.newIntVar(0, 2, "deg_" + i);

            List<BoolVar> adj = new ArrayList<>(4);

            for (int e : puzzle.vertexEdges[i]) {
                if (e == -1) continue;

                adj.add(edges[e]);
            }


            // deg should be equal to edges of the vertex
            model.addEquality(deg[i], LinearExpr.sum(adj.toArray(new BoolVar[0])));

            // deg can only be 0 or 2 for a solved state
            model.addDifferent(deg[i], 1);
        }

    }

    public void solve(){
        buildModel();

        while (true){
            CpSolver solver = new CpSolver();

            CpSolverStatus status = solver.solve(model);

            generatedSolutions++;

            if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE){
                System.out.println("No Solution");
                return;
            }

            // Copy solver values into puzzle
            for (int e = 0; e < edges.length; e++) {
                boolean value = solver.booleanValue(edges[e]);
                if (value) puzzle.setEdge(e, Slitherlink.Edge.ON);
                else puzzle.setEdge(e, Slitherlink.Edge.OFF);
            }



            wallTime+= solver.wallTime();

            if (loopCount()) {

                System.out.println("Wall time : " + wallTime + " s");
                return;
            }
        }
    }
}
