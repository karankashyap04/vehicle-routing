package solver.ls;

import solver.ls.MovingStrategy.RandomCustomerMovement;
import solver.ls.MovingStrategy.TwoOptWithCrossRouteCustomerMove;

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

        VRPLocalSearch solver = new VRPLocalSearch(input, watch);
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
                ", \"Result\": " + String.format("%.2f", totalDistance) +
                ", \"Solution\": \"" + solution.getSolutionString() + "\"}");
    }
}
