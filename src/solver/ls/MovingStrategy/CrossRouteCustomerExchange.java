package solver.ls.MovingStrategy;

import solver.ls.Solution;
import solver.ls.VRPLocalSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Randomly picks two customers in different routes and then exchanges their position.
// For the whole neighborhood, this repeats the process above numVehicles/2 times
public class CrossRouteCustomerExchange implements MovingStrategy {

    private final Random random = new Random(250);

    public Solution getSingleNeighbor(Solution currentSolution, VRPLocalSearch instance) {
        int numVehicles = currentSolution.routes.size();
        final int NUM_TRIES = 5;
        List<Integer> emptyList = new ArrayList<>();

        Solution newSolution = currentSolution.copy();

        // pick the route for customer 1 (route must have length > 2 -- at least 1 customer on the route)
        int customer1Route = random.nextInt(numVehicles);
        int i = 0;
        while (i < NUM_TRIES && currentSolution.routes.get(customer1Route).size() <= 2) {
            customer1Route = random.nextInt(numVehicles);
            i++;
        }
        if (currentSolution.routes.get(customer1Route).size() <= 2)
            return newSolution;

        // pick the route for customer 2
        int customer2Route = random.nextInt(numVehicles);
        i = 0;
        while (i < NUM_TRIES && (customer1Route == customer2Route || currentSolution.routes.get(customer2Route).size() <= 2)) {
            customer2Route = random.nextInt(numVehicles);
            i++;
        }
        if (currentSolution.routes.get(customer2Route).size() <= 2 || customer1Route == customer2Route)
            return newSolution;

        // pick the customer from route 1
        int route1CustomerIdx = 1 + random.nextInt(currentSolution.routes.get(customer1Route).size() - 2);
        // pick the customer from route 2
        int route2CustomerIdx = 1 + random.nextInt(currentSolution.routes.get(customer2Route).size() - 2);

        int route1Customer = currentSolution.routes.get(customer1Route).get(route1CustomerIdx);
        int route2Customer = currentSolution.routes.get(customer2Route).get(route2CustomerIdx);

        // exchange the two customers
        newSolution.routes.get(customer1Route).set(route1CustomerIdx, route2Customer);
        newSolution.routes.get(customer2Route).set(route2CustomerIdx, route1Customer);

        // run verification -- we only need to check that the two routes that were changed are still valid
        List<Integer> newRoute1 = newSolution.routes.get(customer1Route);
        List<Integer> newRoute2 = newSolution.routes.get(customer2Route);
        newSolution.isFeasible = this.isRouteFeasible(newRoute1, instance) && this.isRouteFeasible(newRoute2, instance);
        if (!newSolution.isFeasible) {
            // no need to update total distance here -- we only consider feasible solutions so this will be discarded
            return newSolution;
        }

        // compute new total distance -- we can compute this by seeing the change in distance for the two
        // modified routes
        List<Integer> oldRoute1 = currentSolution.routes.get(customer1Route);
        List<Integer> oldRoute2 = currentSolution.routes.get(customer2Route);
        newSolution.totalDistance += this.routeDistanceChange(oldRoute1, newRoute1, instance) + this.routeDistanceChange(oldRoute2, newRoute2, instance);

        return newSolution;
    }
}
