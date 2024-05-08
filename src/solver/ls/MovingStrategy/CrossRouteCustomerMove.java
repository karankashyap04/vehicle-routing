package solver.ls.MovingStrategy;

import solver.ls.Solution;
import solver.ls.VRPLocalSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Randomly picks a customer. Moves them to every other possible route (in current
// position) --> this is the entire neighborhood
public class CrossRouteCustomerMove implements MovingStrategy {
    private final Random random = new Random(350);

    private int pickRandomVehicle(Solution currentSolution) {
        // need to ensure that the vehicle picked is serving at least 1 customer
        int vehicleIdx;
        do {
            vehicleIdx = random.nextInt(currentSolution.routes.size());
        } while (currentSolution.routes.get(vehicleIdx).size() <= 2);
        return vehicleIdx;
    }

    private int pickRandomCustomerFromVehicleRoute(Solution currentSolution, int vehicleIdx) {
        // add 1 to result to prevent picking customer at index 0 (avoid depot)
        // subtract 2 at end -- subtract 1 to avoid picking last index (depot), and
        //   subtract another 1 to account for adding 1 in the beginning
        return 1 + random.nextInt(currentSolution.routes.get(vehicleIdx).size() - 2);
    }

    public List<Solution> getNeighborhood(Solution currentSolution, VRPLocalSearch instance) {
        // TODO: this needs to be fixed (wasn't fixed while removing multithreading)
//        // pick a random customer to move
//        int sourceRouteIdx = pickRandomVehicle(currentSolution);
//        if (currentSolution.routes.get(sourceRouteIdx).size() <= 2)
//            return new ArrayList<>();
//        int customerSourceIdx = pickRandomCustomerFromVehicleRoute(currentSolution, sourceRouteIdx);
//        int customer = currentSolution.routes.get(sourceRouteIdx).get(customerSourceIdx);
//
//        List<Solution> neighborhood = new ArrayList<>();
//
//        // get neighbors by moving customer to other routes (in same position in route, if possible)
//        for (int newRouteIdx = 0; newRouteIdx < currentSolution.routes.size(); newRouteIdx++) {
//            if (newRouteIdx == sourceRouteIdx)
//                continue;
//
//            List<Integer> destinationRoute = currentSolution.routes.get(newRouteIdx);
//            int customerDestinationIdx = 1 + random.nextInt(destinationRoute.size() - 1);
//
//            Solution newSolution = currentSolution.copy();
//            newSolution.routes.get(newRouteIdx).add(customerDestinationIdx, customer);
//            newSolution.routes.get(sourceRouteIdx).remove(customerSourceIdx);
//            neighborhood.add(newSolution);
//        }
//
//        return neighborhood;
        return new ArrayList<>();
    }

    /**
     * Picks a random customer, and moves them to a random position in a random (different) route
     *
     * @param currentSolution: the solution from which we are moving
     * @return the (candidate) solution obtained after moving a single customer
     */
    public Solution getSingleNeighbor(Solution currentSolution, VRPLocalSearch instance) {
        Solution newSolution = currentSolution.copy();

        // pick a random customer to move
        int sourceRouteIdx = pickRandomVehicle(currentSolution);
        if (currentSolution.routes.get(sourceRouteIdx).size() <= 2)
            return newSolution;
        int customerSourceIdx = pickRandomCustomerFromVehicleRoute(currentSolution, sourceRouteIdx);

        // pick a new route to move them to
        int destinationRouteIdx = random.nextInt(currentSolution.routes.size());
        if (destinationRouteIdx == sourceRouteIdx)
            return newSolution;
        // pick a random position in the route to move them to
        int customerDestinationIdx = 1 + random.nextInt(currentSolution.routes.get(destinationRouteIdx).size() - 1);

        // update the new solution
        int customer = currentSolution.routes.get(sourceRouteIdx).get(customerSourceIdx);
        newSolution.routes.get(destinationRouteIdx).add(customerDestinationIdx, customer);
        newSolution.routes.get(sourceRouteIdx).remove(customerSourceIdx);

        // run verification -- we only need to check that the two routes that were changed are still valid
        List<Integer> newRoute1 = newSolution.routes.get(destinationRouteIdx);
        List<Integer> newRoute2 = newSolution.routes.get(sourceRouteIdx);
        newSolution.isFeasible = this.isRouteFeasible(newRoute1, instance) && this.isRouteFeasible(newRoute2, instance);
        if (!newSolution.isFeasible) {
            // no need to update total distance here -- we only consider feasible solutions so this will be discarded
            return newSolution;
        }

        // compute new total distance -- we can compute this by seeing the change in distance for the two
        // modified routes
        List<Integer> oldRoute1 = currentSolution.routes.get(destinationRouteIdx);
        List<Integer> oldRoute2 = currentSolution.routes.get(sourceRouteIdx);
        newSolution.totalDistance += this.routeDistanceChange(oldRoute1, newRoute1, instance) + this.routeDistanceChange(oldRoute2, newRoute2, instance);

        return newSolution;
    }
}
