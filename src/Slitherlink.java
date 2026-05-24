import java.util.Arrays;

public class Slitherlink {

    public enum Edge {
        UNKNOWN,
        ON,
        OFF
    }

    final int h, w;
    final int[][] clue;
    final Edge[][] horiz;
    final Edge[][] vert;
    final int[][] degree;
    int on_edges;

    boolean solved = false;
    boolean contradictionVertex = false;
    boolean contradictionClue = false;

    // Loop states
    static final int NO_LOOP = 0;
    static final int SINGLE_LOOP = 1;
    static final int PREMATURE_LOOP = 2;

    public Slitherlink(int h, int w, int[][] clues) {
        this.h = h;
        this.w = w;
        this.on_edges = 0;

        this.clue = new int[h][w];
        for (int r = 0; r < h; r++){
            for (int c = 0; c < w; c++){
                clue[r][c] = clues[r][c];
            }
        }

        this.horiz = new Edge[h+1][w];
        for (int r = 0; r <= h; r++) {
            for (int c = 0; c < w; c++) {
                horiz[r][c] = Edge.UNKNOWN;
            }
        }

        this.vert = new Edge[h][w+1];
        for (int r = 0; r < h; r++) {
            for (int c = 0; c <= w; c++) {
                vert[r][c] = Edge.UNKNOWN;
            }
        }

        this.degree = new int[h+1][w+1];
    }

    // copy contructor
    public Slitherlink(Slitherlink that) {
        this.h = that.h;
        this.w = that.w;
        this.on_edges = that.on_edges;

        this.clue = that.clue;

        this.horiz = new Edge[h+1][w];
        for (int r = 0; r <= h; r++) {
            for (int c = 0; c < w; c++) {
                horiz[r][c] = that.horiz[r][c];
            }
        }

        this.vert = new Edge[h][w+1];
        for (int r = 0; r < h; r++) {
            for (int c = 0; c <= w; c++) {
                vert[r][c] = that.vert[r][c];
            }
        }

        this.degree = new int[h+1][w+1];
        for (int r = 0; r <= h; r++) {
            for (int c = 0; c <= w; c++) {
                degree[r][c] = that.degree[r][c];
            }
        }

        this.solved = that.solved;
        this.contradictionClue = that.contradictionClue;
        this.contradictionVertex = that.contradictionVertex;
    }


    public boolean setHoriz(int r, int c, Edge val) {
        if (horiz[r][c] == val) return false;

        Edge oldVal = horiz[r][c];
        horiz[r][c] = val;

        if (val == Edge.ON){
            degree[r][c]++;
            degree[r][c+1]++;
            on_edges++;
        }
        else if (oldVal == Edge.ON) {
            degree[r][c]--;
            degree[r][c+1]--;
            on_edges--;
        }

        return true;
    }

    public boolean setVert(int r, int c, Edge val) {
        if (vert[r][c] == val) return false;

        Edge oldVal = vert[r][c];
        vert[r][c] = val;

        if (val == Edge.ON){
            degree[r][c]++;
            degree[r+1][c]++;
            on_edges++;
        }
        else if (oldVal == Edge.ON) {
            degree[r][c]--;
            degree[r+1][c]--;
            on_edges--;
        }

        return true;
    }

    int adjacentEdgesSum(int r, int c){
        int sum = 0;
        if (horiz[r][c] == Edge.ON) sum++;
        if (vert[r][c] == Edge.ON) sum++;
        if (horiz[r+1][c] == Edge.ON) sum++;
        if (vert[r][c+1] == Edge.ON) sum++;
        return sum;
    }

    boolean checkAllSums(){
        for (int r = 0; r < h; r++){
            for (int c = 0; c < w; c++){
                if (clue[r][c] != -1){
                    if (clue[r][c] != adjacentEdgesSum(r,c)){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean isSolved() {
        solved = loopCheck() == SINGLE_LOOP && checkAllSums();
        return solved;
    }

    // generated with Chatgpt
    public void print() {
        int R = 2 * h + 1;
        int C = 2 * w + 1;

        char[][] buf = new char[R][C];
        for (char[] row : buf) Arrays.fill(row, ' ');

        // 1) Draw vertices
        for (int r = 0; r <= h; r++) {
            for (int c = 0; c <= w; c++) {
                buf[2 * r][2 * c] = '+';
            }
        }

        // 2) Draw horizontal edges
        for (int r = 0; r <= h; r++) {
            for (int c = 0; c < w; c++) {
                if (horiz[r][c] == Edge.ON) {
                    buf[2 * r][2 * c + 1] = '-';
                }
                else if (horiz[r][c] == Edge.OFF) {
                    buf[2 * r][2 * c + 1] = 'x';
                }
            }
        }

        // 3) Draw vertical edges
        for (int r = 0; r < h; r++) {
            for (int c = 0; c <= w; c++) {
                if (vert[r][c] == Edge.ON) {
                    buf[2 * r + 1][2 * c] = '|';
                }
                else if (vert[r][c] == Edge.OFF) {
                    buf[2 * r + 1][2 * c] = 'x';
                }
            }
        }

        // 4) Draw clues
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                if (clue[r][c] >= 0) {
                    buf[2 * r + 1][2 * c + 1] =
                            (char) ('0' + clue[r][c]);
                }
            }
        }

        // 5) Print
        for (char[] row : buf) {
            System.out.println(new String(row));
        }
    }


    // RULES FOR SIMPLIFY

    public boolean ruleVertex(){
        boolean changed = false;
        for (int r = 0; r <= h; r++){
            for (int c = 0; c <= w; c++){
                int off = 0;
                int on = 0;

                Edge[] edges = new Edge[4];

                if (r > 0) edges[0] = vert[r-1][c];
                if (c > 0) edges[1] = horiz[r][c-1];
                if (r < h) edges[2] = vert[r][c];
                if (c < w) edges[3] = horiz[r][c];

                for (Edge e : edges){
                    if (e == null) off++;
                    else if (e == Edge.ON) on++;
                    else if (e == Edge.OFF) off++;
                }

                if (on > 2) contradictionVertex = true;
                if (on == 1 && off == 3) contradictionVertex = true;

                if (off == 3){
                    if (edges[0] == Edge.UNKNOWN) changed |= setVert(r-1, c, Edge.OFF);
                    if (edges[1] == Edge.UNKNOWN) changed |= setHoriz(r, c-1, Edge.OFF);
                    if (edges[2] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.OFF);
                    if (edges[3] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.OFF);
                }
                else if (on == 2){
                    if (edges[0] == Edge.UNKNOWN) changed |= setVert(r-1, c, Edge.OFF);
                    if (edges[1] == Edge.UNKNOWN) changed |= setHoriz(r, c-1, Edge.OFF);
                    if (edges[2] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.OFF);
                    if (edges[3] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.OFF);
                }
                else if (off == 2 && on == 1){
                    if (edges[0] == Edge.UNKNOWN) changed |= setVert(r-1, c, Edge.ON);
                    if (edges[1] == Edge.UNKNOWN) changed |= setHoriz(r, c-1, Edge.ON);
                    if (edges[2] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.ON);
                    if (edges[3] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.ON);
                }
            }
        }

        return changed;
    }

    public boolean ruleCornersOfThree() {
        boolean changed = false;
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                if (clue[r][c] != 3) continue;

                Edge[] cornerEdges = new Edge[8];

                if (c > 0){
                    cornerEdges[7] = horiz[r+1][c-1];
                    cornerEdges[0] = horiz[r][c-1];
                }
                if (r > 0){
                    cornerEdges[1] = vert[r-1][c];
                    cornerEdges[2] = vert[r-1][c+1];
                }
                if (c < w - 1){
                    cornerEdges[3] = horiz[r][c+1];
                    cornerEdges[4] = horiz[r+1][c+1];
                }
                if (r < h - 1){
                    cornerEdges[5] = vert[r+1][c+1];
                    cornerEdges[6] = vert[r+1][c];
                }



                if ((cornerEdges[0] == null || cornerEdges[0] == Edge.OFF) && (cornerEdges[1] == null || cornerEdges[1] == Edge.OFF)){
                    if (horiz[r][c] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.ON);
                    if (vert[r][c] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.ON);
                }
                if ((cornerEdges[2] == null || cornerEdges[2] == Edge.OFF) && (cornerEdges[3] == null || cornerEdges[3] == Edge.OFF)){
                    if (horiz[r][c] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.ON);
                    if (vert[r][c+1] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.ON);
                }
                if ((cornerEdges[4] == null || cornerEdges[4] == Edge.OFF) && (cornerEdges[5] == null || cornerEdges[5] == Edge.OFF)){
                    if (horiz[r+1][c] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.ON);
                    if (vert[r][c+1] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.ON);
                }
                if ((cornerEdges[6] == null || cornerEdges[6] == Edge.OFF) && (cornerEdges[7] == null || cornerEdges[7] == Edge.OFF)){
                    if (horiz[r+1][c] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.ON);
                    if (vert[r][c] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.ON);
                }



                if (cornerEdges[0] == Edge.ON){
                    if (horiz[r+1][c] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.ON);
                    if (vert[r][c+1] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.ON);
                    if (cornerEdges[1] == Edge.UNKNOWN) changed |= setVert(r-1, c, Edge.OFF);
                }
                if (cornerEdges[1] == Edge.ON){
                    if (horiz[r+1][c] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.ON);
                    if (vert[r][c+1] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.ON);
                    if (cornerEdges[0] == Edge.UNKNOWN) changed |= setHoriz(r, c-1, Edge.OFF);
                }
                if (cornerEdges[2] == Edge.ON){
                    if (horiz[r+1][c] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.ON);
                    if (vert[r][c] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.ON);
                    if (cornerEdges[3] == Edge.UNKNOWN) changed |= setHoriz(r, c+1, Edge.OFF);
                }
                if (cornerEdges[3] == Edge.ON){
                    if (horiz[r+1][c] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.ON);
                    if (vert[r][c] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.ON);
                    if (cornerEdges[2] == Edge.UNKNOWN) changed |= setVert(r-1, c+1, Edge.OFF);
                }
                if (cornerEdges[4] == Edge.ON){
                    if (horiz[r][c] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.ON);
                    if (vert[r][c] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.ON);
                    if (cornerEdges[5] == Edge.UNKNOWN) changed |= setVert(r+1, c+1, Edge.OFF);
                }
                if (cornerEdges[5] == Edge.ON){
                    if (horiz[r][c] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.ON);
                    if (vert[r][c] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.ON);
                    if (cornerEdges[4] == Edge.UNKNOWN) changed |= setHoriz(r+1, c+1, Edge.OFF);
                }
                if (cornerEdges[6] == Edge.ON){
                    if (horiz[r][c] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.ON);
                    if (vert[r][c+1] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.ON);
                    if (cornerEdges[7] == Edge.UNKNOWN) changed |= setHoriz(r+1, c-1, Edge.OFF);
                }
                if (cornerEdges[7] == Edge.ON){
                    if (horiz[r][c] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.ON);
                    if (vert[r][c+1] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.ON);
                    if (cornerEdges[6] == Edge.UNKNOWN) changed |= setVert(r+1, c, Edge.OFF);
                }
            }
        }

        return changed;
    }

    public boolean ruleCornersOfOne() {
        boolean changed = false;
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                if (clue[r][c] != 1) continue;

                Edge[] cornerEdges = new Edge[8];

                if (c > 0){
                    cornerEdges[7] = horiz[r+1][c-1];
                    cornerEdges[0] = horiz[r][c-1];
                }
                if (r > 0){
                    cornerEdges[1] = vert[r-1][c];
                    cornerEdges[2] = vert[r-1][c+1];
                }
                if (c < w - 1){
                    cornerEdges[3] = horiz[r][c+1];
                    cornerEdges[4] = horiz[r+1][c+1];
                }
                if (r < h - 1){
                    cornerEdges[5] = vert[r+1][c+1];
                    cornerEdges[6] = vert[r+1][c];
                }

                if ((cornerEdges[0] == null || cornerEdges[0] == Edge.OFF) && (cornerEdges[1] == null || cornerEdges[1] == Edge.OFF)){
                    if (horiz[r][c] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.OFF);
                    if (vert[r][c] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.OFF);
                }
                if ((cornerEdges[2] == null || cornerEdges[2] == Edge.OFF) && (cornerEdges[3] == null || cornerEdges[3] == Edge.OFF)){
                    if (horiz[r][c] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.OFF);
                    if (vert[r][c+1] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.OFF);
                }
                if ((cornerEdges[4] == null || cornerEdges[4] == Edge.OFF) && (cornerEdges[5] == null || cornerEdges[5] == Edge.OFF)){
                    if (horiz[r+1][c] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.OFF);
                    if (vert[r][c+1] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.OFF);
                }
                if ((cornerEdges[6] == null || cornerEdges[6] == Edge.OFF) && (cornerEdges[7] == null || cornerEdges[7] == Edge.OFF)){
                    if (horiz[r+1][c] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.OFF);
                    if (vert[r][c] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.OFF);
                }


                if ((cornerEdges[0] == Edge.ON && cornerEdges[1] == Edge.OFF) || (cornerEdges[1] == Edge.ON && cornerEdges[0] == Edge.OFF)){
                    if (horiz[r+1][c] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.OFF);
                    if (vert[r][c+1] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.OFF);
                }
                if ((cornerEdges[2] == Edge.ON && cornerEdges[3] == Edge.OFF) || (cornerEdges[3] == Edge.ON && cornerEdges[2] == Edge.OFF)){
                    if (horiz[r+1][c] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.OFF);
                    if (vert[r][c] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.OFF);
                }
                if ((cornerEdges[4] == Edge.ON && cornerEdges[5] == Edge.OFF) || (cornerEdges[5] == Edge.ON && cornerEdges[4] == Edge.OFF)){
                    if (horiz[r][c] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.OFF);
                    if (vert[r][c] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.OFF);
                }
                if ((cornerEdges[6] == Edge.ON && cornerEdges[7] == Edge.OFF) || (cornerEdges[7] == Edge.ON && cornerEdges[6] == Edge.OFF)){
                    if (horiz[r][c] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.OFF);
                    if (vert[r][c+1] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.OFF);
                }
            }
        }

        return changed;
    }

    public boolean ruleCornersTwo() {
        boolean changed = false;
        if (clue[0][0] == 2){
            changed |= setHoriz(0, 1, Edge.ON);
            changed |= setVert(1, 0, Edge.ON);
        }
        if (clue[0][w-1] == 2){
            changed |= setHoriz(0, w-2, Edge.ON);
            changed |= setVert(1, w, Edge.ON);
        }
        if (clue[h-1][0] == 2){
            changed |= setHoriz(h, 1, Edge.ON);
            changed |= setVert(h-2, 0, Edge.ON);
        }
        if (clue[h-1][w-1] == 2){
            changed |= setHoriz(h, w-2, Edge.ON);
            changed |= setVert(h-2, w, Edge.ON);
        }

        return changed;
    }

    public boolean ruleNeighborsOfThree() {
        boolean changed = false;
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                if (clue[r][c] != 3) continue;

                // right
                if (c < w - 1 && clue[r][c+1] == 3){
                    changed |= setVert(r, c, Edge.ON);
                    changed |= setVert(r, c+1, Edge.ON);
                    changed |= setVert(r, c+2, Edge.ON);
                    if (r + 1 < h) changed |= setVert(r+1, c+1, Edge.OFF);
                    if (r - 1 >= 0) changed |= setVert(r-1, c+1, Edge.OFF);

                }

                // down-right
                if (r < h - 1 && c < w - 1 && clue[r+1][c+1] == 3){
                    changed |= setVert(r, c, Edge.ON);
                    changed |= setHoriz(r, c, Edge.ON);
                    changed |= setVert(r+1, c+2, Edge.ON);
                    changed |= setHoriz(r+2, c+1, Edge.ON);
                }

                // down
                if (r < h - 1 && clue[r+1][c] == 3){
                    changed |= setHoriz(r, c, Edge.ON);
                    changed |= setHoriz(r+1, c, Edge.ON);
                    changed |= setHoriz(r+2, c, Edge.ON);
                    if (c + 1 < w) changed |= setHoriz(r+1, c+1, Edge.OFF);
                    if (c - 1 >= 0) changed |= setHoriz(r+1, c-1, Edge.OFF);
                }

                // down-left
                if (r < h - 1 && c > 0 && clue[r+1][c-1] == 3){
                    changed |= setVert(r, c+1, Edge.ON);
                    changed |= setHoriz(r, c, Edge.ON);
                    changed |= setVert(r+1, c-1, Edge.ON);
                    changed |= setHoriz(r+2, c-1, Edge.ON);
                }
            }
        }

        return changed;
    }

    public boolean ruleClue() {
        boolean changed = false;
        for (int r = 0; r < h; r++){
            for (int c = 0; c < w; c++){
                if (clue[r][c] < 0) continue;

                int on = 0;
                int off = 0;

                Edge[] edges = {
                        horiz[r][c],
                        vert[r][c],
                        horiz[r+1][c],
                        vert[r][c+1]
                };

                for (Edge e : edges){
                    if (e == Edge.ON) on++;
                    if (e == Edge.OFF) off++;
                }

                if (on > clue[r][c] || off > 4 - clue[r][c]) contradictionClue = true;

                if (on == clue[r][c]){
                    if (edges[0] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.OFF);
                    if (edges[1] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.OFF);
                    if (edges[2] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.OFF);
                    if (edges[3] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.OFF);
                }

                else if (off == 4 - clue[r][c]){
                    if (edges[0] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.ON);
                    if (edges[1] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.ON);
                    if (edges[2] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.ON);
                    if (edges[3] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.ON);
                }
            }
        }

        return changed;
    }

    public boolean rule1x1() {
        boolean changed = false;
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                int on = 0;

                Edge[] edges = {
                        horiz[r][c],
                        vert[r][c],
                        horiz[r+1][c],
                        vert[r][c+1]
                };

                for (Edge e : edges){
                    if (e == Edge.ON) on++;
                }

                if (on == 3){
                    if (edges[0] == Edge.UNKNOWN) changed |= setHoriz(r, c, Edge.OFF);
                    if (edges[1] == Edge.UNKNOWN) changed |= setVert(r, c, Edge.OFF);
                    if (edges[2] == Edge.UNKNOWN) changed |= setHoriz(r+1, c, Edge.OFF);
                    if (edges[3] == Edge.UNKNOWN) changed |= setVert(r, c+1, Edge.OFF);
                }
            }
        }

        return changed;
    }

    public int loopCheck(){
        boolean[][] visited = new boolean[h+1][w+1];
        for (int r = 0; r <= h; r++){
            for (int c = 0; c <= w; c++){
                if (degree[r][c] == 2 && !visited[r][c]){
                    int loopEdges = 0;
                    int nr = r;
                    int nc = c;

                    while (true){
                        visited[nr][nc] = true;

                        // right
                        if (nc < w && horiz[nr][nc] == Edge.ON) {

                            if (nr == r && nc + 1 == c  && loopEdges > 1) {
                                loopEdges++;
                                if (loopEdges < on_edges) return PREMATURE_LOOP;
                                else return SINGLE_LOOP;
                            }

                            if (!visited[nr][nc + 1]) {
                                loopEdges++;
                                nc = nc + 1;
                                continue;
                            }
                        }

                        // down
                        if (nr < h && vert[nr][nc] == Edge.ON) {

                            if (nr + 1 == r && nc == c  && loopEdges > 1) {
                                loopEdges++;
                                if (loopEdges < on_edges) return PREMATURE_LOOP;
                                else return SINGLE_LOOP;
                            }

                            if (!visited[nr + 1][nc]) {
                                loopEdges++;
                                nr = nr + 1;
                                continue;
                            }
                        }

                        // left
                        if (nc > 0 && horiz[nr][nc - 1] == Edge.ON) {

                            if (nr == r && nc - 1 == c  && loopEdges > 1) {
                                loopEdges++;
                                if (loopEdges < on_edges) return PREMATURE_LOOP;
                                else return SINGLE_LOOP;
                            }

                            if (!visited[nr][nc - 1]) {
                                loopEdges++;
                                nc = nc - 1;
                                continue;
                            }
                        }

                        // up
                        if (nr > 0 && vert[nr - 1][nc] == Edge.ON) {

                            if (nr - 1 == r && nc == c && loopEdges > 1) {
                                loopEdges++;
                                if (loopEdges < on_edges) return PREMATURE_LOOP;
                                else return SINGLE_LOOP;
                            }

                            if (!visited[nr - 1][nc]) {
                                loopEdges++;
                                nr = nr - 1;
                                continue;
                            }
                        }

                        break;
                    }
                }
            }
        }
        return NO_LOOP;
    }

    public boolean contradiction() {
        if (contradictionClue) System.out.println("Contradicion in Clues");
        if (contradictionVertex) System.out.println("Contradicion in Vertices");
        int loopCheck = loopCheck();
        if (loopCheck == PREMATURE_LOOP) System.out.println("Contradicion in Loop");
        return contradictionClue || contradictionVertex || loopCheck == PREMATURE_LOOP;
    }
}
