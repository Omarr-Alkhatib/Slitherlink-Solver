public class SolverDS {

    public final Slitherlink puzzle;

    public SolverDS(Slitherlink puzzle) {
        this.puzzle = puzzle;
    }

    public void simplify(Slitherlink puzzle) {
        boolean changed = true;
        while (changed){
            changed = false;

            changed |= puzzle.ruleVertex();
            changed |= puzzle.ruleClue();
            changed |= puzzle.ruleNeighborsOfThree();
            changed |= puzzle.ruleCornersTwo();
            changed |= puzzle.ruleCornersOfThree();
            changed |= puzzle.ruleCornersOfOne();
            changed |= puzzle.rule1x1();
        }
    }

    public boolean deduce(Slitherlink board, int depth){
        boolean changed = false;

        for (int r = 0; r <= board.h; r++){
            for (int c = 0; c <= board.w; c++){
                for (boolean horizontal : new boolean[]{true, false}) {
                    if (horizontal) {
                        if (c == board.w) continue;
                        if (board.horiz[r][c] != Slitherlink.Edge.UNKNOWN) continue;
                    }

                    if (!horizontal){
                        if (r == board.h) continue;
                        if (board.vert[r][c] != Slitherlink.Edge.UNKNOWN) continue;
                    }

                    // Shaving
                    // Copies for the shaving process
                    Slitherlink copy1 = new Slitherlink(board);
                    Slitherlink copy2 = new Slitherlink(board);

                    // Edge ON
                    if (horizontal) copy1.setHoriz(r, c, Slitherlink.Edge.ON);
                    else copy1.setVert(r, c, Slitherlink.Edge.ON);

                    simplify(copy1);

                    copy1.print();
                    if (horizontal) System.out.println("Testing horiz[" + r + "][" + c + "] ON at depth " + depth + " and simplifying \n");
                    else System.out.println("Testing vert[" + r + "][" + c + "] ON at depth " + depth + " and simplifying \n");

                    boolean contra1 = copy1.contradiction();
                    if (contra1){
                        if (horizontal) board.setHoriz(r, c, Slitherlink.Edge.OFF);
                        else board.setVert(r, c, Slitherlink.Edge.OFF);

                        simplify(board);

                        board.print();
                        System.out.println("Set the opposite value in the original and simplify \n");

                        changed = true;
                        if (board.isSolved()) {
                            System.out.println("Puzzle solved after setting the opposite value \n");
                            return true;
                        }
                    } else {
                        if (copy1.isSolved()) {
                            if (horizontal) board.setHoriz(r, c, Slitherlink.Edge.ON);
                            else board.setVert(r, c, Slitherlink.Edge.ON);

                            simplify(board);
                            board.solved = true;

                            board.print();
                            System.out.println("Puzzle solved after testing the edge \n");
                            return true;
                        }
                        if (depth > 1){
                            System.out.println("Deduce at shallower depth 1");
                            deduce(copy1, depth - 1);
                            System.out.println("Back to depth 2");
                        }
                    }

                    // Edge OFF
                    if (horizontal) copy2.setHoriz(r, c, Slitherlink.Edge.OFF);
                    else copy2.setVert(r, c, Slitherlink.Edge.OFF);

                    simplify(copy2);
                    copy2.print();
                    if (horizontal) System.out.println("Testing horiz[" + r + "][" + c + "] OFF at depth " + depth + " and simplifying \n");
                    else System.out.println("Testing vert[" + r + "][" + c + "] OFF at depth " + depth + " and simplifying \n");

                    boolean contra2 = copy2.contradiction();
                    if (contra2){
                        if (horizontal) board.setHoriz(r, c, Slitherlink.Edge.ON);
                        else board.setVert(r, c, Slitherlink.Edge.ON);
                        simplify(board);
                        board.print();
                        System.out.println("Set the opposite value in the original and simplify \n");

                        changed = true;
                        if (board.isSolved()) {
                            System.out.println("Puzzle solved after setting the opposite value \n");
                            return true;
                        }

                    } else {
                        if (copy2.isSolved()) {
                            if (horizontal) board.setHoriz(r, c, Slitherlink.Edge.OFF);
                            else board.setVert(r, c, Slitherlink.Edge.OFF);

                            simplify(board);
                            board.solved = true;

                            board.print();
                            System.out.println("Puzzle solved after testing the edge \n");
                            return true;
                        }
                        if (depth > 1){
                            System.out.println("Deduce at shallower depth 1");
                            deduce(copy2, depth - 1);
                            System.out.println("Back to depth 2");
                        }
                    }

                    // If both tests were consistent, apply Agreement
                    boolean changed_agree = false;

                    if (!contra1 && !contra2) changed_agree = agreement(board, copy1, copy2);

                    changed |= changed_agree;

                    if (changed_agree) {
                        board.print();
                        System.out.println("Both tests were consistent. Applying agreement on board \n");
                    }

                    // Early exit for 2-LoE
                    if (depth > 1 && changed) return true;
                }
            }
        }

        return changed;
    }

    public boolean agreement(Slitherlink original, Slitherlink copy1, Slitherlink copy2){
        boolean changed = false;

        // Horizontal Edges
        for (int r = 0; r <= original.h; r++) {
            for (int c = 0; c < original.w; c++) {
                if (copy1.horiz[r][c] == copy2.horiz[r][c]) changed |= original.setHoriz(r, c, copy1.horiz[r][c]);
            }
        }

        // Vertical Edges
        for (int r = 0; r < original.h; r++) {
            for (int c = 0; c <= original.w; c++) {
                if (copy1.vert[r][c] == copy2.vert[r][c]) changed |= original.setVert(r, c, copy1.vert[r][c]);
            }
        }

        simplify(original);

        return changed;
    }

    public void solve() {

        // 0-LoE
        simplify(puzzle);
        puzzle.print();
        System.out.println("After the first simplification at 0-LoE \n");

        if (puzzle.isSolved()){
            System.out.println("Puzzle is already solved after the first simplification");
            return;
        }

        while (!puzzle.solved){
            boolean changed = false;

            // 1-LoE
            while (!puzzle.solved){
                System.out.println("Entering 1-LoE");
                changed = deduce(puzzle, 1);
                if (!changed) break;
            }

            // 2-LoE
            if (!puzzle.solved){
                System.out.println("Puzzle is not solvable in 1-LoE. Entering 2-LoE:");
                changed = deduce(puzzle, 2);
            }

            if (!changed) break;
        }

        if (puzzle.isSolved()){
            puzzle.print();
            System.out.println("Puzzle solved");
        } else {
            System.out.println("Puzzle is NON-DEDUCIBLE at 2-LoE");
        }

    }
}
