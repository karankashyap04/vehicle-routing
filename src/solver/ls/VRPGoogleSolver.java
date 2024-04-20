package solver.ls;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.FirstSolutionStrategy;
import com.google.ortools.constraintsolver.LocalSearchMetaheuristic;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.RoutingSearchParameters;
import com.google.ortools.constraintsolver.main;
//import com.google.protobuf.Duration;
import java.util.logging.Logger;

/**
 * This class is not a part of our actual solution. Since our solution uses a
 * local search technique, it is an incomplete method, and does not provide
 * any guarantees of optimality. In order to get a sense of how well our
 * local search approach performed, we used the VRP solver provided by
 * Google's OR-Tools suite, in order to get the optimal result, to
 * enable a comparison with our results.
 * <p>
 * Reference: https://developers.google.com/optimization/routing/vrp
 */
public class VRPGoogleSolver extends VRPInstance {

    private static final Logger logger = Logger.getLogger(VRPGoogleSolver.class.getName());
    long[] vehicleCapacities;

    public VRPGoogleSolver(String filename, Timer watch) {
        super(filename, watch);

        this.vehicleCapacities = new long[this.numVehicles];
        for (int i = 0; i < this.numVehicles; i++) {
            this.vehicleCapacities[i] = this.vehicleCapacity;
        }
    }

    /// @brief Print the solution.
    private void printSolution(RoutingModel routing, RoutingIndexManager manager, Assignment solution) {
        // Solution cost.
        logger.info("Objective : " + solution.objectiveValue());
        // Inspect solution.
        long maxRouteDistance = 0;
        for (int i = 0; i < this.numVehicles; ++i) {
            long index = routing.start(i);
            logger.info("Route for Vehicle " + i + ":");
            long routeDistance = 0;
            String route = "";
            while (!routing.isEnd(index)) {
                route += manager.indexToNode(index) + " -> ";
                long previousIndex = index;
                index = solution.value(routing.nextVar(index));
                routeDistance += routing.getArcCostForVehicle(previousIndex, index, i);
            }
            logger.info(route + manager.indexToNode(index));
            logger.info("Distance of the route: " + routeDistance + "m");
            maxRouteDistance = Math.max(routeDistance, maxRouteDistance);
        }
        logger.info("Maximum of the route distances: " + maxRouteDistance + "m");
    }


    public void solve() {
        Loader.loadNativeLibraries();

        RoutingIndexManager manager =
                new RoutingIndexManager(this.distance.length, this.numVehicles, 0);

        // Create Routing Model.
        RoutingModel routing = new RoutingModel(manager);


        // Create and register a transit callback.
        final int transitCallbackIndex =
                routing.registerTransitCallback((long fromIndex, long toIndex) -> {
                    // Convert from routing variable Index to user NodeIndex.
                    int fromNode = manager.indexToNode(fromIndex);
                    int toNode = manager.indexToNode(toIndex);
                    return (long)this.distance[fromNode][toNode];
                });

        // Define cost of each arc.
        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);

        // Add Capacity constraint.
        final int demandCallbackIndex = routing.registerUnaryTransitCallback((long fromIndex) -> {
            // Convert from routing variable Index to user NodeIndex.
            int fromNode = manager.indexToNode(fromIndex);
            return this.demandOfCustomer[fromNode];
        });

        routing.addDimensionWithVehicleCapacity(demandCallbackIndex, 0, // null capacity slack
                this.vehicleCapacities, // vehicle maximum capacities
                true, // start cumul to zero
                "Capacity");

        // Setting first solution heuristic.
        RoutingSearchParameters searchParameters =
                main.defaultRoutingSearchParameters()
                        .toBuilder()
                        .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                        .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
//                        .setTimeLimit(Duration.newBuilder().setSeconds(1).build()) // TODO: uncomment to set timeout
                        .build();

        // Solve the problem.
        Assignment solution = routing.solveWithParameters(searchParameters);

        // Print solution on console.
        printSolution(routing, manager, solution);
    }
}
