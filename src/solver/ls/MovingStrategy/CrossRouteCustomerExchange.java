package solver.ls.MovingStrategy;

import solver.ls.Solution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Randomly picks two customers in different routes and then exchanges their position.
// For the whole neighborhood, this repeats the process above numVehicles/2 times
public class CrossRouteCustomerExchange implements MovingStrategy {

    private final Random random = new Random(200000000);

    public List<Solution> getNeighborhood(Solution currentSolution) {
        final int NEIGHBORHOOD_SIZE = currentSolution.routes.size() / 2;

        List<Solution> neighborhood = new ArrayList<>();
        for (int i = 0; i < NEIGHBORHOOD_SIZE; i++)
            neighborhood.add(getSingleNeighbor(currentSolution));
        return neighborhood;
    }

    public Solution getSingleNeighbor(Solution currentSolution) {
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

        return newSolution;
    }
}
