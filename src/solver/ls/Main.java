package solver.ls;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java Main <file>");
            return;
        }

        String input = args[0];
        Path path = Paths.get(input);
        String filename = path.getFileName().toString();
        System.out.println("Instance: " + input);

        Timer watch = new Timer();
        watch.start();
//        VRPInstance instance = new VRPInstance(input);
//        VRPGoogleSolver solver = new VRPGoogleSolver(input);
        VRPLocalSearch solver = new VRPLocalSearch(input);
        Solution solution = solver.constructInitialSolution();
        watch.stop();

        for (int i = 0; i < solver.numVehicles; i++) {
            for (int custNum : solution.routes.get(i)) {
                System.out.print(custNum + " ");
            }
            System.out.println();
        }
        System.out.println("total distance: " + solver.solutionTotalDistance(solution));

        System.out.println("{\"Instance\": \"" + filename +
                "\", \"Time\": " + String.format("%.2f", watch.getTime()) +
                ", \"Result\": \"--\"" +
                ", \"Solution\": \"--\"}");
    }
}