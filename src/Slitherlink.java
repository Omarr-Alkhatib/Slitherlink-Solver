import java.util.Arrays;

public class Slitherlink {

    final int h, w;
    final int[][] clue;
    final boolean[][] horiz;
    final boolean[][] vert;
    final int[][] degree;
    boolean solved = false;

    public Slitherlink(int h, int w, int[][] clues) {
        this.h = h;
        this.w = w;
        this.clue = new int[h][w];
        this.horiz = new boolean[h+1][w];
        this.vert = new boolean[h][w+1];
        this.degree = new int[h+1][w+1];
        for (int r = 0; r < h; r++){
            for (int c = 0; c < w; c++){
                clue[r][c] = clues[r][c];
            }
        }
    }

    boolean inside(int r, int c) {
        return 0 <= r && r <= h && 0 <= c && c <= w;
    }

    public void setHoriz(int r, int c, boolean val) {
        if (horiz[r][c] == val) return;

        horiz[r][c] = val;

        if (val){
            degree[r][c]++;
            degree[r][c+1]++;
        } else {
            degree[r][c]--;
            degree[r][c+1]--;
        }
    }

    public void setVert(int r, int c, boolean val) {
        if (vert[r][c] == val) return;

        vert[r][c] = val;

        if (val){
            degree[r][c]++;
            degree[r+1][c]++;
        } else {
            degree[r][c]--;
            degree[r+1][c]--;
        }
    }

    int adjacentEdgesSum(int r, int c){
        int sum = 0;
        if (horiz[r][c]) sum++;
        if (vert[r][c]) sum++;
        if (horiz[r+1][c]) sum++;
        if (vert[r][c+1]) sum++;
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

    boolean singleLoop(){
        boolean atLeastOneCell = false;
        for (int r = 0; r <= h; r++) {
            for (int c = 0; c <= w; c++) {
                if (degree[r][c] != 0 && degree[r][c] != 2){
                    return false;
                } else {
                    if (degree[r][c] == 2){
                        atLeastOneCell = true;
                    }
                }
            }
        }
        return atLeastOneCell;
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
                if (horiz[r][c]) {
                    buf[2 * r][2 * c + 1] = '-';
                }
            }
        }

        // 3) Draw vertical edges
        for (int r = 0; r < h; r++) {
            for (int c = 0; c <= w; c++) {
                if (vert[r][c]) {
                    buf[2 * r + 1][2 * c] = '|';
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
}
