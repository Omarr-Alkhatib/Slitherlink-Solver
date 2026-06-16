public class SolverDS {

    public static class Metrics {
        int S;
        int V;
        int A;

        public void add(Metrics other){
            S += other.S;
            V += other.V;
            A += other.A;
        }
    }

    public final Slitherlink puzzle;
    public int LoE = 0;
    public int[] S = new int[3];
    public int[] V = new int[3];
    public int[] A = new int[3];
    public double difficulty;

    public SolverDS(Slitherlink puzzle) {
        this.puzzle = puzzle;
    }

    public int preprocessing(Slitherlink puzzle) {
        return puzzle.ruleNeighborsOfThree() + puzzle.ruleTwoAtCorners();
    }

    public int simplify(Slitherlink puzzle) {
        int total = 0;

        while (true){
            int changes = 0;

            changes += puzzle.ruleVertex();
            changes += puzzle.ruleClue();
            changes += puzzle.ruleCornersOfThree();
            changes += puzzle.ruleCornersOfTwo();
            changes += puzzle.ruleCornersOfOne();
            changes += puzzle.rule1x1();

            total += changes;

            if (changes == 0) break;
        }

        return total;
    }

    public boolean deduce(Slitherlink board, int depth, Metrics metrics){
        board.changed = false;

        for (int e = 0; e < board.edges.length; e++){
            if (board.edges[e] != Slitherlink.Edge.UNKNOWN) continue;

            // Shaving
            // Copies for the shaving process
            Slitherlink copy1 = new Slitherlink(board);
            Slitherlink copy2 = new Slitherlink(board);

            // Metrics for difficulty estimation
            Metrics m1 = new Metrics();
            Metrics m2 = new Metrics();

            // Edge ON
            copy1.setEdge(e, Slitherlink.Edge.ON);

            m1.S += simplify(copy1);

            //copy1.print();
            //System.out.println("Testing edge " + e + " ON at depth " + depth + " in LoE " + LoE + " and simplifying \n");

            // if 2-LoE, recursively call deduce again
            boolean contra1_LoE2 = false;
            if (depth > 1){
                //System.out.println("Deduce at shallower depth 1");
                contra1_LoE2 = deduce(copy1, depth - 1, m1);
                //System.out.println("Back to depth 2");
            }

            // checking for contradiction
            boolean contra1 = copy1.contradiction() || contra1_LoE2;
            if (contra1){

                // set the opposite value on board
                board.setEdge(e, Slitherlink.Edge.OFF);

                metrics.V++;

                metrics.S += simplify(board);

                //board.print();
                //System.out.println("Set the opposite value in the original and simplify \n");

                if (board.isSolved()) {
                    metrics.S += board.finalizeSolution();

                    //System.out.println("Puzzle solved after setting the opposite value \n");

                    return false;
                }

            } else {

                // if the test edge solves the puzzle
                if (copy1.isSolved()) {

                    // copy the result state to the original puzzle and return
                    board.copyState(copy1);

                    metrics.add(m1);
                    metrics.V++;
                    metrics.S += board.finalizeSolution();

                    //board.print();
                    //System.out.println("Puzzle solved after testing the edge \n");

                    return false;
                }

            }

            // Edge OFF
            copy2.setEdge(e, Slitherlink.Edge.OFF);

            int simplifyEdges2 = simplify(copy2);

            //copy2.print();
            //System.out.println("Testing edge " + e + " OFF at depth " + depth + " in LoE " + LoE + " and simplifying \n");

            // if 2-LoE, recursive call deduce again
            boolean contra2_LoE2 = false;
            if (depth > 1){
                //System.out.println("Deduce at shallower depth 1");
                contra2_LoE2 = deduce(copy2, depth - 1, m2);
                //System.out.println("Back to depth 2");
            }

            // checking for contradiction
            boolean contra2 = copy2.contradiction() || contra2_LoE2;
            if (contra2){

                // set the opposite value on board
                board.setEdge(e, Slitherlink.Edge.ON);

                metrics.V++;

                metrics.S += simplify(board);



                //board.print();
                //System.out.println("Set the opposite value in the original and simplify \n");

                if (board.isSolved()) {
                    metrics.S += board.finalizeSolution();

                    //System.out.println("Puzzle solved after setting the opposite value \n");

                    return false;
                }

            } else {

                // if the test edge solves the puzzle
                if (copy2.isSolved()) {

                    // copy the result state to the original puzzle and return
                    board.copyState(copy2);

                    if (!contra1) {
                        metrics.V++;
                        metrics.S += simplifyEdges2;
                    }
                    metrics.add(m2);

                    metrics.S += board.finalizeSolution();


                    //board.print();
                    //System.out.println("Puzzle solved after testing the edge \n");

                    return false;
                }

            }

            // If both tests were consistent, apply Agreement
            if (!contra1 && !contra2) agreement(board, copy1, copy2, metrics);

            // If both values cause a contradiction, branch is no longer valid
            if (contra1 && contra2) return true;

            // Early exit for 2-LoE
            if (depth > 1 && board.changed) return false;
        }

        return false;
    }

    public void agreement(Slitherlink board, Slitherlink copy1, Slitherlink copy2, Metrics metrics){
        int changes = 0;

        for (int e = 0; e < board.edges.length; e++){
            if (copy1.edges[e] == copy2.edges[e]) changes += board.setEdge(e, copy1.edges[e]) ? 1 : 0;
        }

        metrics.A += changes;

        metrics.S += simplify(board);

        if (changes > 0) {
            //board.print();
            //System.out.println("Both tests were consistent. Applying agreement on board \n");
        }
    }

    public void solve() {

        // 0-LoE
        S[0] += preprocessing(puzzle);
        S[0] += simplify(puzzle);

        //puzzle.print();
        //System.out.println("After the first simplification at 0-LoE \n");

        if (puzzle.contradiction()){
            System.out.println("Puzzle is contradictory and cannot be solved");
            return;
        }

        if (puzzle.isSolved()){
            S[0] += puzzle.finalizeSolution();
            difficulty = (double) S[0] / (double) (puzzle.edges.length * 2);

            //System.out.println("S0 = " + S[0]);
            System.out.println("Puzzle is already solved after the first simplification");
            return;
        }

        puzzle.changed = true;

        while (!puzzle.solved && puzzle.changed){

            // 1-LoE
            while (!puzzle.solved && puzzle.changed){
                LoE = 1;

                Metrics m1 = new Metrics();

                //System.out.println("Entering 1-LoE");

                boolean unsolvable = deduce(puzzle, 1, m1);
                if (unsolvable){
                    System.out.println("Puzzle is contradictory and cannot be solved");
                    return;
                }

                S[1] += m1.S;
                V[1] += m1.V;
                A[1] += m1.A;
            }

            // 2-LoE
            if (!puzzle.solved){
                LoE = 2;

                Metrics m2 = new Metrics();

                //System.out.println("Puzzle is not solvable in 1-LoE. Entering 2-LoE:");

                boolean unsolvable = deduce(puzzle, 2, m2);
                if (unsolvable){
                    System.out.println("Puzzle is contradictory and cannot be solved");
                    return;
                }

                S[2] += m2.S;
                V[2] += m2.V;
                A[2] += m2.A;
            }
        }

        difficulty = (double) (S[0] + 4 * (S[1] + V[1] + A[1]) + 9 * (S[2] + V[2] + A[2]))
                          / (double) (puzzle.edges.length * 2);

        if (puzzle.isSolved()){
            //puzzle.print();
            System.out.println("Puzzle solved \n");

            System.out.println("S0 = " + S[0]);
            System.out.println("S1 = " + S[1]);
            System.out.println("V1 = " + V[1]);
            System.out.println("A1 = " + A[1]);
            System.out.println("S2 = " + S[2]);
            System.out.println("V2 = " + V[2]);
            System.out.println("A2 = " + A[2]);
            System.out.println("Variables: " + (S[0] + S[1] + S[2] + V[0] + V[1] + V[2] + A[0] + A[1] + A[2]));
            System.out.println("Difficulty: " + difficulty);

        } else {
            //puzzle.print();
            System.out.println("Puzzle is NON-DEDUCIBLE at 2-LoE");
        }

    }
}
