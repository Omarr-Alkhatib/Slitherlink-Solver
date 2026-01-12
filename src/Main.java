import java.io.*;

public class Main {
    public static void main(String[] args) throws Exception{
        BufferedReader br = new BufferedReader(new FileReader("instances/janko/Janko316.txt"));
        String[] s = br.readLine().trim().split("\\s+");
        int h = Integer.parseInt(s[0]);
        int w = Integer.parseInt(s[1]);

        int[][] clue = new int[h][w];
        for(int i=0;i<h;i++){
            String[] tok = br.readLine().trim().split("\\s+");
            for(int j=0;j<w;j++){
                clue[i][j] = switch(tok[j]){
                    case "3" -> 3;
                    case "2" -> 2;
                    case "1" -> 1;
                    case "0" -> 0;
                    default  -> -1;
                };
            }
        }

        Slitherlink slither = new Slitherlink(h, w, clue);
        SolverCP solver = new SolverCP(slither);
        long start = System.currentTimeMillis();
        solver.solve();
        long end = System.currentTimeMillis();

        /*if (slither.solved) {
            System.out.println("Solved in " + (end - start) + " ms");
            slither.print();
        } else {
            System.out.println("No solution.");
        }*/
    }
}