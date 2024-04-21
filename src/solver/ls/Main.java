package solver.ls;

import solver.ls.MovingStrategy.MovingStrategy;
import solver.ls.MovingStrategy.RandomCustomerMovement;

import java.nio.file.Path;
import java.nio.file.Paths;

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

        MovingStrategy movingStrategy = new RandomCustomerMovement();
        VRPLocalSearch solver = new VRPLocalSearch(input, movingStrategy, watch);
        Solution solution = solver.localSearch();
        watch.stop();

        for (int i = 0; i < solver.numVehicles; i++) {
            for (int custNum : solution.routes.get(i)) {
                System.out.print(custNum + " ");
            }
            System.out.println();
        }
        double totalDistance = solver.solutionTotalDistance(solution);

        System.out.println("{\"Instance\": \"" + filename +
                "\", \"Time\": " + String.format("%.2f", watch.getTime()) +
                ", \"Result\": \"" + totalDistance + "\"" +
                ", \"Solution\": \"" + solution.getSolutionString() + "\"}");
    }
}