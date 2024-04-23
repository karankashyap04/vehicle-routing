package solver.ls.MovingStrategy;

import solver.ls.Solution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Randomly picks two customers in different routes and then exchanges their position.
// For the whole neighborhood, this repeats the process above numVehicles/2 times
public class CrossRouteCustomerExchange implements MovingStrategy {

    private final Random random = new Random(200000000);

    public List<Move> getNeighborhoodMoves(Solution currentSolution) {
        final int NEIGHBORHOOD_SIZE = currentSolution.routes.size() / 2;

        List<Move> neighborhoodMoves = new ArrayList<>();
        for (int i = 0; i < NEIGHBORHOOD_SIZE; i++)
            neighborhoodMoves.add(getSingleNeighbor(currentSolution));
        return neighborhoodMoves;
    }

    public Move getSingleNeighbor(Solution currentSolution) {
        int numVehicles = currentSolution.routes.size();
        final int NUM_TRIES = 5;
        List<Integer> emptyList = new ArrayList<>();

        // pick the route for customer 1 (route must have length > 2 -- at least 1 customer on the route)
        int customer1Route = random.nextInt(numVehicles);
        int i = 0;
        while (i < NUM_TRIES && currentSolution.routes.get(customer1Route).size() <= 2) {
            customer1Route = random.nextInt(numVehicles);
            i++;
        }
        if (currentSolution.routes.get(customer1Route).size() <= 2)
            return new Move(emptyList, emptyList, emptyList, emptyList);

        // pick the route for customer 2
        int customer2Route = random.nextInt(numVehicles);
        i = 0;
        while (i < NUM_TRIES && (customer1Route == customer2Route || currentSolution.routes.get(customer2Route).size() <= 2)) {
            customer2Route = random.nextInt(numVehicles);
            i++;
        }
        if (currentSolution.routes.get(customer2Route).size() <= 2 || customer1Route == customer2Route)
            return new Move(emptyList, emptyList, emptyList, emptyList);

        // pick the customer from route 1
        int route1Customer = 1 + random.nextInt(currentSolution.routes.get(customer1Route).size() - 2);
        // pick the customer from route 2
        int route2Customer = 1 + random.nextInt(currentSolution.routes.get(customer2Route).size() - 2);

        return new Move(
                new ArrayList<>(List.of(customer1Route, customer2Route)),
                new ArrayList<>(List.of(route1Customer, route2Customer)),
                new ArrayList<>(List.of(customer2Route, customer1Route)),
                new ArrayList<>(List.of(route2Customer, route1Customer))
        );
    }
}
