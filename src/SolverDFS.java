public class SolverDFS {

    public final Slitherlink puzzle;

    public SolverDFS(Slitherlink puzzle) {
        this.puzzle = puzzle;
    }

    void dfs(int r, int c){
        if (puzzle.solved) return;

        if (puzzle.checkAllSums() && puzzle.singleLoop()){
            puzzle.solved = true;
            return;
        }

        int nr, nc;

        // UP
        nr = r - 1;
        nc = c;
        if (puzzle.inside(nr, nc))
            if (puzzle.vert[nr][nc] != Slitherlink.Edge.ON)
                if (puzzle.degree[nr][nc] < 2){
                    puzzle.setVert(nr, nc, Slitherlink.Edge.ON);
                    dfs(nr, nc);
                    if (puzzle.solved) return;
                    puzzle.setVert(nr, nc, Slitherlink.Edge.UNKNOWN);
                }



        // RIGHT
        nr = r;
        nc = c + 1;
        if (puzzle.inside(nr, nc))
            if (puzzle.horiz[r][c] != Slitherlink.Edge.ON)
                if (puzzle.degree[nr][nc] < 2){
                    puzzle.setHoriz(r, c, Slitherlink.Edge.ON);
                    dfs(nr, nc);
                    if (puzzle.solved) return;
                    puzzle.setHoriz(r, c, Slitherlink.Edge.UNKNOWN);
                }



        // DOWN
        nr = r + 1;
        nc = c;
        if (puzzle.inside(nr, nc))
            if (puzzle.vert[r][c] != Slitherlink.Edge.ON)
                if (puzzle.degree[nr][nc] < 2){
                    puzzle.setVert(r, c, Slitherlink.Edge.ON);
                    dfs(nr, nc);
                    if (puzzle.solved) return;
                    puzzle.setVert(r, c, Slitherlink.Edge.UNKNOWN);
                }



        // LEFT
        nr = r;
        nc = c - 1;
        if (puzzle.inside(nr, nc))
            if (puzzle.horiz[nr][nc] != Slitherlink.Edge.ON)
                if (puzzle.degree[nr][nc] < 2){
                    puzzle.setHoriz(nr, nc, Slitherlink.Edge.ON);
                    dfs(nr, nc);
                    if (puzzle.solved) return;
                    puzzle.setHoriz(nr, nc, Slitherlink.Edge.UNKNOWN);
                }
    }

    void solve(){
        for (int r = 0; r <= puzzle.h; r++){
            for (int c = 0; c <= puzzle.w; c++){
                dfs(r, c);
                if (puzzle.solved) return;
            }
        }
    }
}
