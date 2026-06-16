import java.io.*;

// This class is used for manually testing solvers

public class Main {
    public static void main(String[] args) throws Exception{
        BufferedReader br = new BufferedReader(new FileReader("instances/Tokoton/Tokoton-01-slitherlink-079-36x20.txt"));

        String line;
        int h = -1;
        int w = -1;
        int[] clues = null;
        double difficulty = -1;

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.equals("[size]")) {
                String[] size = br.readLine().trim().split("\\s+");

                h = Integer.parseInt(size[0]);
                w = Integer.parseInt(size[1]);

                clues = new int[h * w];
            }

            else if (line.equals("[cells]")) {
                for (int r = 0; r < h; r++) {
                    String[] row = br.readLine().trim().split("\\s+");
                    for (int c = 0; c < w; c++) {
                        int i = r * w + c;

                        clues[i] = switch (row[c]) {
                            case "0" -> 0;
                            case "1" -> 1;
                            case "2" -> 2;
                            case "3" -> 3;
                            default -> -1;
                        };
                    }
                }
            }

            else if (line.equals("[difficulty]")) {
                difficulty = Double.parseDouble(br.readLine().trim());
            }
        }

        br.close();

        Slitherlink puzzle = new Slitherlink(h, w, clues);
        puzzle.difficulty = difficulty;

        SolverDS solver = new SolverDS(puzzle);
        long start = System.currentTimeMillis();
        solver.solve();
        long end = System.currentTimeMillis();
        System.out.println("Solved in " + (end - start) + " ms");


    }
}