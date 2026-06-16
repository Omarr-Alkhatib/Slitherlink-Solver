import java.util.Arrays;

public class Slitherlink {

    public enum Edge {
        UNKNOWN,
        ON,
        OFF
    }

    final int h, w;
    final int[] clue;
    final Edge[] edges;
    final int[][] clueEdges;
    final int[][] vertexEdges;
    final int[][] edgeVertices;
    final int[] degree;
    int on_edges;

    boolean solved = false;
    boolean changed = false;
    boolean contradictionVertex = false;
    boolean contradictionClue = false;

    double difficulty;

    // Loop states
    static final int NO_LOOP = 0;
    static final int SINGLE_LOOP = 1;
    static final int PREMATURE_LOOP = 2;

    public Slitherlink(int h, int w, int[] clues) {
        this.h = h;
        this.w = w;

        this.on_edges = 0;
        this.edges = new Edge[(h+1)*w + (w+1)*h];
        Arrays.fill(edges, Edge.UNKNOWN);

        this.edgeVertices = new int[edges.length][2];
        // Vertical
        for (int i = 0; i < (w + 1) * h; i++) {
            edgeVertices[i][0] = i;
            edgeVertices[i][1] = i + (w + 1);
        }
        // Horizontal
        for (int i = (w + 1) * h; i < edges.length; i++) {

            int j = i - (w + 1) * h;

            int row = j / w;
            int col = j % w;

            edgeVertices[i][0] = row * (w + 1) + col;
            edgeVertices[i][1] = row * (w + 1) + col + 1;
        }

        this.clue = new int[h*w];
        this.clueEdges = new int[clue.length][4];
        for (int i = 0; i < clue.length; i++) {

            clue[i] = clues[i];

            int vEdges = (w + 1) * h;
            int row = i / w;
            clueEdges[i] = new int[]{
                    i + row + 1,       // RIGHT
                    i + vEdges + w,    // DOWN
                    i + row,           // LEFT
                    i + vEdges,        // UP
            };
        }


        this.degree = new int[(h+1)*(w+1)];
        this.vertexEdges = new int[degree.length][4];
        for (int i = 0; i < degree.length; i++){

            int vEdges = (w + 1) * h;
            int row = i / (w + 1);
            int col = i % (w + 1);

            vertexEdges[i][0] = (col < w) ? vEdges + row * w + col : -1;                       // RIGHT
            vertexEdges[i][2] = (col > 0) ? vEdges + row * w + col - 1 : -1;                   // LEFT

            vertexEdges[i][1] = (i < (w+1)*h) ? i : -1;                                        // DOWN
            vertexEdges[i][3] = (i >= (w+1)) ? i - (w+1) : -1;                                 // UP
        }
    }

    // copy constructor
    public Slitherlink(Slitherlink that) {
        this.h = that.h;
        this.w = that.w;

        this.on_edges = that.on_edges;
        this.edges = new Edge[(h+1)*w + (w+1)*h];
        System.arraycopy(that.edges, 0, edges, 0, edges.length);

        this.clue = that.clue;
        this.clueEdges = that.clueEdges;
        this.vertexEdges = that.vertexEdges;
        this.edgeVertices = that.edgeVertices;

        this.degree = new int[(h+1)*(w+1)];
        System.arraycopy(that.degree, 0, degree, 0, degree.length);

        this.solved = that.solved;
        this.contradictionClue = that.contradictionClue;
        this.contradictionVertex = that.contradictionVertex;
    }

    public void copyState(Slitherlink other) {

        System.arraycopy(other.edges, 0, this.edges, 0, edges.length);

        System.arraycopy(other.degree, 0, this.degree, 0, degree.length);

        this.on_edges = other.on_edges;
        this.solved = other.solved;
    }

    public boolean setEdge(int i, Edge val) {
        if (edges[i] == val) return false;

        Edge oldVal =  edges[i];
        edges[i] = val;
        changed = true;

        int v1 = edgeVertices[i][0];
        int v2 = edgeVertices[i][1];

        if (val == Edge.ON){
            degree[v1]++;
            degree[v2]++;
            on_edges++;
        }
        else if (oldVal == Edge.ON) {
            degree[v1]--;
            degree[v2]--;
            on_edges--;
        }

        return true;
    }



    int adjacentEdgesSum(int clue){
        int sum = 0;
        for (int i = 0; i < 4; i++){
            Edge e = edges[clueEdges[clue][i]];
            if (e == Edge.ON) sum++;
        }
        return sum;
    }

    boolean checkAllSums(){
        for (int i = 0; i < clue.length; i++){
                if (clue[i] != -1 && clue[i] != adjacentEdgesSum(i)) return false;
            }

        return true;
    }

    public boolean isSolved() {
        solved = (loopCheck() == SINGLE_LOOP) && checkAllSums();
        return solved;
    }

    public int finalizeSolution() {
        int changes = 0;

        for (int e = 0; e < edges.length; e++)
            if (edges[e] == Edge.UNKNOWN) changes += setEdge(e, Edge.OFF) ? 1 : 0;


        return changes;
    }

    // generated with Chatgpt
    public void print() {
        int R = 2 * h + 1;
        int C = 2 * w + 1;

        char[][] buf = new char[R][C];

        for (char[] row : buf) Arrays.fill(row, ' ');

        // 1) Draw vertices
        for (int v = 0; v < degree.length; v++) {

            int row = v / (w + 1);
            int col = v % (w + 1);

            buf[2 * row][2 * col] = '+';
        }

        int vEdges = (w + 1) * h;

        // vertical edges
        for (int i = 0; i < vEdges; i++) {

            if (edges[i] == Edge.UNKNOWN) continue;

            int e = i;

            int col = e % (w + 1);
            int row = e / (w + 1);

            // vertical edge sits between (row,col) and (row+1,col)
            int r = 2 * row + 1;
            int c = 2 * col;

            char ch = (edges[e] == Edge.ON) ? '|' : 'x';

            buf[r][c] = ch;

        }

        // horizontal edges
        for (int i = vEdges; i < edges.length; i++) {

            if (edges[i] == Edge.UNKNOWN) continue;

            int e = i - vEdges;

            int row = e / w;
            int col = e % w;

            // horizontal edge sits between (row,col) and (row,col+1)
            int r = 2 * row;
            int c = 2 * col + 1;

            char ch = (edges[i] == Edge.ON) ? '-' : 'x';

            buf[r][c] = ch;

        }

        // 3) Draw clues
        for (int i = 0; i < clue.length; i++) {

            if (clue[i] < 0) continue;

            int row = i / w;
            int col = i % w;

            buf[2 * row + 1][2 * col + 1]
                    = (char) ('0' + clue[i]);
        }

        // 4) Print
        for (char[] row : buf) {
            System.out.println(new String(row));
        }
    }


    // RULES FOR SIMPLIFY

    public int ruleVertex(){
        int changes = 0;

        for (int i = 0; i < degree.length; i++){

            int off = 0;
            int on = 0;

            for (int e : vertexEdges[i]) {
                if (e == -1) off++;
                else if (edges[e] == Edge.ON) on++;
                else if (edges[e] == Edge.OFF) off++;
            }

            if (on > 2) contradictionVertex = true;
            if (on == 1 && off == 3) contradictionVertex = true;

            if (off == 3 || on == 2){
                for (int e : vertexEdges[i]) {
                    if (e == -1) continue;
                    if (edges[e] == Edge.UNKNOWN) changes += setEdge(e, Edge.OFF) ? 1 : 0;
                }
            }

            else if (off == 2 && on == 1){
                for (int e : vertexEdges[i]) {
                    if (e == -1) continue;
                    if (edges[e] == Edge.UNKNOWN) changes += setEdge(e, Edge.ON) ? 1 : 0;
                }
            }
        }

        return changes;
    }

    public int ruleClue() {
        int changes = 0;

        for (int i = 0; i < clue.length; i++){
            if (clue[i] < 0) continue;

            int on = 0;
            int off = 0;

            for (int e : clueEdges[i]) {
                if (edges[e] == Edge.ON) on++;
                else if (edges[e] == Edge.OFF) off++;
            }

            if (on > clue[i] || off > 4 - clue[i]) contradictionClue = true;

            if (on == clue[i]){
                for (int e : clueEdges[i]) {
                    if (edges[e] == Edge.UNKNOWN) changes += setEdge(e, Edge.OFF) ? 1 : 0;
                }
            }

            else if (off == 4 - clue[i]){
                for (int e : clueEdges[i]) {
                    if (edges[e] == Edge.UNKNOWN) changes += setEdge(e, Edge.ON) ? 1 : 0;
                }
            }

        }

        return changes;
    }

    public int rule1x1() {
        int changes = 0;

        for (int i = 0; i < clue.length; i++) {

            int on = 0;

            for (int e : clueEdges[i]) {
                if (edges[e] == Edge.ON) on++;
            }

            if (on == 3){
                for (int e : clueEdges[i]) {
                    if (edges[e] == Edge.UNKNOWN) changes += setEdge(e, Edge.OFF) ? 1 : 0;
                }
            }
        }

        return changes;
    }

    // Helper function to get the corner outer edges of a clue
    public int[] getCornerEdges(int clue) {

        int[] cornerEdges = new int[8];
        Arrays.fill(cornerEdges, -1);

        int row = clue / w;
        int col = clue % w;

        if (col > 0) {
            cornerEdges[0] = clueEdges[clue][3] - 1;
            cornerEdges[7] = clueEdges[clue][1] - 1;
        }

        if (row > 0) {
            cornerEdges[1] = clueEdges[clue][2] - (w + 1);
            cornerEdges[2] = clueEdges[clue][0] - (w + 1);
        }

        if (col < w - 1){
            cornerEdges[3] = clueEdges[clue][3] + 1;
            cornerEdges[4] = clueEdges[clue][1] + 1;
        }

        if (row < h - 1) {
            cornerEdges[5] = clueEdges[clue][0] + (w + 1);
            cornerEdges[6] = clueEdges[clue][2] + (w + 1);
        }

        return cornerEdges;
    }

    public int ruleCornersOfThree() {
        int changes = 0;

        for (int i = 0; i < clue.length; i++) {
            if (clue[i] != 3) continue;

            int[] cornerEdgesIdx = getCornerEdges(i);
            Edge[] cornerEdges = new Edge[8];

            for (int e = 0; e < 8; e++){
                int idx = cornerEdgesIdx[e];
                if (idx == -1) continue;
                cornerEdges[e] = edges[idx];
            }

            int right = clueEdges[i][0];
            int down  = clueEdges[i][1];
            int left  = clueEdges[i][2];
            int up    = clueEdges[i][3];


            // Top-left corner blocked
            if ((cornerEdges[0] == null || cornerEdges[0] == Edge.OFF) && (cornerEdges[1] == null || cornerEdges[1] == Edge.OFF)){
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.ON) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.ON) ? 1 : 0;
            }
            // Top-right corner blocked
            if ((cornerEdges[2] == null || cornerEdges[2] == Edge.OFF) && (cornerEdges[3] == null || cornerEdges[3] == Edge.OFF)){
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.ON) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.ON) ? 1 : 0;
            }
            // Down-right corner blocked
            if ((cornerEdges[4] == null || cornerEdges[4] == Edge.OFF) && (cornerEdges[5] == null || cornerEdges[5] == Edge.OFF)){
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.ON) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.ON) ? 1 : 0;
            }
            // Down-left corner blocked
            if ((cornerEdges[6] == null || cornerEdges[6] == Edge.OFF) && (cornerEdges[7] == null || cornerEdges[7] == Edge.OFF)){
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.ON) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.ON) ? 1 : 0;
            }


            // Line coming from top-left corner
            if (cornerEdges[0] == Edge.ON){
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.ON) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.ON) ? 1 : 0;
                if (cornerEdges[1] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[1], Edge.OFF) ? 1 : 0;
            }
            if (cornerEdges[1] == Edge.ON){
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.ON) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.ON) ? 1 : 0;
                if (cornerEdges[0] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[0], Edge.OFF) ? 1 : 0;
            }

            // Line coming from top-right corner
            if (cornerEdges[2] == Edge.ON){
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.ON) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.ON) ? 1 : 0;
                if (cornerEdges[3] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[3], Edge.OFF) ? 1 : 0;
            }
            if (cornerEdges[3] == Edge.ON){
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.ON) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.ON) ? 1 : 0;
                if (cornerEdges[2] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[2], Edge.OFF) ? 1 : 0;
            }

            // Line coming from down-right corner
            if (cornerEdges[4] == Edge.ON){
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.ON) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.ON) ? 1 : 0;
                if (cornerEdges[5] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[5], Edge.OFF) ? 1 : 0;
            }
            if (cornerEdges[5] == Edge.ON){
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.ON) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.ON) ? 1 : 0;
                if (cornerEdges[4] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[4], Edge.OFF) ? 1 : 0;
            }

            // Line coming from down-left corner
            if (cornerEdges[6] == Edge.ON){
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.ON) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.ON) ? 1 : 0;
                if (cornerEdges[7] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[7], Edge.OFF) ? 1 : 0;
            }
            if (cornerEdges[7] == Edge.ON){
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.ON) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.ON) ? 1 : 0;
                if (cornerEdges[6] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[6], Edge.OFF) ? 1 : 0;
            }
        }

        return changes;
    }

    public int ruleCornersOfTwo() {
        int changes = 0;

        for (int i = 0; i < clue.length; i++) {
            if (clue[i] != 2) continue;

            int[] cornerEdgesIdx = getCornerEdges(i);
            Edge[] cornerEdges = new Edge[8];

            for (int e = 0; e < 8; e++){
                int idx = cornerEdgesIdx[e];
                if (idx == -1) continue;
                cornerEdges[e] = edges[idx];
            }

            int right = clueEdges[i][0];
            int down  = clueEdges[i][1];
            int left  = clueEdges[i][2];
            int up    = clueEdges[i][3];


            // Line coming from top-left corner
            if ((cornerEdges[0] == Edge.ON) && (edges[right] == Edge.OFF || edges[down] == Edge.OFF)){
                if (cornerEdges[1] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[1], Edge.OFF) ? 1 : 0;
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.ON) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.ON) ? 1 : 0;
            }

            if ((cornerEdges[1] == Edge.ON) && (edges[right] == Edge.OFF || edges[down] == Edge.OFF)){
                if (cornerEdges[0] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[0], Edge.OFF) ? 1 : 0;
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.ON) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.ON) ? 1 : 0;
            }

            // Line coming from top-right corner
            if ((cornerEdges[2] == Edge.ON) && (edges[left] == Edge.OFF || edges[down] == Edge.OFF)){
                if (cornerEdges[3] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[3], Edge.OFF) ? 1 : 0;
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.ON) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.ON) ? 1 : 0;
            }

            if ((cornerEdges[3] == Edge.ON) && (edges[left] == Edge.OFF || edges[down] == Edge.OFF)){
                if (cornerEdges[2] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[2], Edge.OFF) ? 1 : 0;
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.ON) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.ON) ? 1 : 0;
            }

            // Line coming from down-right corner
            if ((cornerEdges[4] == Edge.ON) && (edges[left] == Edge.OFF || edges[up] == Edge.OFF)){
                if (cornerEdges[5] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[5], Edge.OFF) ? 1 : 0;
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.ON) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.ON) ? 1 : 0;
            }

            if ((cornerEdges[5] == Edge.ON) && (edges[left] == Edge.OFF || edges[up] == Edge.OFF)){
                if (cornerEdges[4] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[4], Edge.OFF) ? 1 : 0;
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.ON) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.ON) ? 1 : 0;
            }

            // Line coming from down-left corner
            if ((cornerEdges[6] == Edge.ON) && (edges[right] == Edge.OFF || edges[up] == Edge.OFF)){
                if (cornerEdges[7] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[7], Edge.OFF) ? 1 : 0;
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.ON) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.ON) ? 1 : 0;
            }

            if ((cornerEdges[7] == Edge.ON) && (edges[right] == Edge.OFF || edges[up] == Edge.OFF)){
                if (cornerEdges[6] == Edge.UNKNOWN) changes += setEdge(cornerEdgesIdx[6], Edge.OFF) ? 1 : 0;
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.ON) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.ON) ? 1 : 0;
            }
        }

        return changes;
    }

    public int ruleCornersOfOne() {
        int changes = 0;

        for (int i = 0; i < clue.length; i++) {
            if (clue[i] != 1) continue;

            int[] cornerEdgesIdx = getCornerEdges(i);
            Edge[] cornerEdges = new Edge[8];

            for (int e = 0; e < 8; e++){
                int idx = cornerEdgesIdx[e];
                if (idx == -1) continue;
                cornerEdges[e] = edges[idx];
            }

            int right = clueEdges[i][0];
            int down  = clueEdges[i][1];
            int left  = clueEdges[i][2];
            int up    = clueEdges[i][3];


            // Top-left corner blocked
            if ((cornerEdges[0] == null || cornerEdges[0] == Edge.OFF) && (cornerEdges[1] == null || cornerEdges[1] == Edge.OFF)){
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.OFF) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.OFF) ? 1 : 0;
            }
            // Top-right corner blocked
            if ((cornerEdges[2] == null || cornerEdges[2] == Edge.OFF) && (cornerEdges[3] == null || cornerEdges[3] == Edge.OFF)){
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.OFF) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.OFF) ? 1 : 0;
            }
            // Down-right corner blocked
            if ((cornerEdges[4] == null || cornerEdges[4] == Edge.OFF) && (cornerEdges[5] == null || cornerEdges[5] == Edge.OFF)){
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.OFF) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.OFF) ? 1 : 0;
            }
            // Down-left corner blocked
            if ((cornerEdges[6] == null || cornerEdges[6] == Edge.OFF) && (cornerEdges[7] == null || cornerEdges[7] == Edge.OFF)){
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.OFF) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.OFF) ? 1 : 0;
            }


            // Line coming from top-left corner
            if ((cornerEdges[0] == Edge.ON && cornerEdges[1] == Edge.OFF) || (cornerEdges[1] == Edge.ON && cornerEdges[0] == Edge.OFF)){
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.OFF) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.OFF) ? 1 : 0;
            }

            // Line coming from top-right corner
            if ((cornerEdges[2] == Edge.ON && cornerEdges[3] == Edge.OFF) || (cornerEdges[3] == Edge.ON && cornerEdges[2] == Edge.OFF)){
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.OFF) ? 1 : 0;
                if (edges[down] == Edge.UNKNOWN) changes += setEdge(down, Edge.OFF) ? 1 : 0;
            }

            // Line coming from down-right corner
            if ((cornerEdges[4] == Edge.ON && cornerEdges[5] == Edge.OFF) || (cornerEdges[5] == Edge.ON && cornerEdges[4] == Edge.OFF)){
                if (edges[left] == Edge.UNKNOWN) changes += setEdge(left, Edge.OFF) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.OFF) ? 1 : 0;
            }

            // Line coming from down-left corner
            if ((cornerEdges[6] == Edge.ON && cornerEdges[7] == Edge.OFF) || (cornerEdges[7] == Edge.ON && cornerEdges[6] == Edge.OFF)){
                if (edges[right] == Edge.UNKNOWN) changes += setEdge(right, Edge.OFF) ? 1 : 0;
                if (edges[up] == Edge.UNKNOWN) changes += setEdge(up, Edge.OFF) ? 1 : 0;
            }
        }

        return changes;
    }

    public int ruleTwoAtCorners() {
        int changes = 0;

        // top-left
        if (clue[0] == 2){
            changes += setEdge(clueEdges[0][2] + (w + 1), Edge.ON) ? 1 : 0;
            changes += setEdge(clueEdges[0][3] + 1, Edge.ON) ? 1 : 0;
        }
        // top-right
        if (clue[w-1] == 2){
            changes += setEdge(clueEdges[w-1][3] - 1, Edge.ON) ? 1 : 0;
            changes += setEdge(clueEdges[w-1][0] + (w + 1), Edge.ON) ? 1 : 0;
        }
        // bottom-left
        if (clue[w * (h - 1)] == 2){
            changes += setEdge(clueEdges[w * (h - 1)][2] - (w + 1), Edge.ON) ? 1 : 0;
            changes += setEdge(clueEdges[w * (h - 1)][1] + 1, Edge.ON) ? 1 : 0;
        }
        // bottom-right
        if (clue[clue.length - 1] == 2){
            changes += setEdge(clueEdges[clue.length - 1][0] - (w + 1), Edge.ON) ? 1 : 0;
            changes += setEdge(clueEdges[clue.length - 1][1] - 1, Edge.ON) ? 1 : 0;
        }

        return changes;
    }

    public int ruleNeighborsOfThree() {
        int changes = 0;

        boolean ruleValid = false;

        for (int c : clue)
            if (c == 1 || c == 2) {
                ruleValid = true;
                break;
            }

        if (!ruleValid) return 0;

        for (int i = 0; i < clue.length; i++) {
            if (clue[i] != 3) continue;

            // right
            if (i % w < w - 1 && clue[i+1] == 3){
                changes += setEdge(clueEdges[i][2], Edge.ON) ? 1 : 0;
                changes += setEdge(clueEdges[i][0], Edge.ON) ? 1 : 0;
                changes += setEdge(clueEdges[i+1][0], Edge.ON) ? 1 : 0;
                if (i / w < h - 1) changes += setEdge(clueEdges[i][0] + (w+1), Edge.OFF) ? 1 : 0;
                if (i / w >= 1) changes += setEdge(clueEdges[i][0] - (w+1), Edge.OFF) ? 1 : 0;
            }

            // down-right
            if (i / w < h - 1 && i % w < w - 1 && clue[i+w+1] == 3){
                changes += setEdge(clueEdges[i][2], Edge.ON) ? 1 : 0;
                changes += setEdge(clueEdges[i][3], Edge.ON) ? 1 : 0;
                changes += setEdge(clueEdges[i+w+1][0], Edge.ON) ? 1 : 0;
                changes += setEdge(clueEdges[i+w+1][1], Edge.ON) ? 1 : 0;
            }

            // down
            if (i / w < h - 1 && clue[i+w] == 3){
                changes += setEdge(clueEdges[i][3], Edge.ON) ? 1 : 0;
                changes += setEdge(clueEdges[i][1], Edge.ON) ? 1 : 0;
                changes += setEdge(clueEdges[i+w][1], Edge.ON) ? 1 : 0;
                if (i % w < w - 1) changes += setEdge(clueEdges[i][1] + 1, Edge.OFF) ? 1 : 0;
                if (i % w >= 1) changes += setEdge(clueEdges[i][1] - 1, Edge.OFF) ? 1 : 0;
            }

            // down-left
            if (i / w < h - 1 && i % w > 0 && clue[i+w-1] == 3){
                changes += setEdge(clueEdges[i][3], Edge.ON) ? 1 : 0;
                changes += setEdge(clueEdges[i][0], Edge.ON) ? 1 : 0;
                changes += setEdge(clueEdges[i+w-1][1], Edge.ON) ? 1 : 0;
                changes += setEdge(clueEdges[i+w-1][2], Edge.ON) ? 1 : 0;
            }
        }

        return changes;
    }



    public int loopCheck(){
        boolean[] visited = new boolean[degree.length];
        for (int start = 0; start < degree.length; start++){
            if (degree[start] != 2 || visited[start]) continue;

            int loopEdges = 0;
            int current = start;

            while (true){
                visited[current] = true;
                boolean move = false;

                for (int e : vertexEdges[current]){
                    if (e == -1 || edges[e] != Edge.ON) continue;

                    int neighbor = (edgeVertices[e][0] == current) ? edgeVertices[e][1] : edgeVertices[e][0];

                    // found the start point
                    if (neighbor == start && loopEdges > 1){
                        loopEdges++;
                        if (loopEdges < on_edges) return PREMATURE_LOOP;
                        else return SINGLE_LOOP;
                    }

                    if (!visited[neighbor]){
                        loopEdges++;
                        move = true;
                        current = neighbor;
                        break;
                    }
                }

                if (!move) break;
            }
        }

        return NO_LOOP;
    }

    public boolean contradiction() {
        // if (contradictionClue) System.out.println("Contradiction in Clues");
        // if (contradictionVertex) System.out.println("Contradiction in Vertices");
        int loopCheck = loopCheck();
        // if (loopCheck == PREMATURE_LOOP) System.out.println("Contradiction in Loop");
        return contradictionClue || contradictionVertex || loopCheck == PREMATURE_LOOP;
    }
}
