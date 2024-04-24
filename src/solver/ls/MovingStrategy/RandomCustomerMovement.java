package solver.ls.MovingStrategy;

import solver.ls.Solution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This approach picks a random customer to move. It then defines the neighborhood as
 * all possible places they could be moved to within their own route (change the order
 * in which they are visited within their route), and also moving them to other routes
 */
public class RandomCustomerMovement implements MovingStrategy {

    private final Random random = new Random(400000000);

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

    public List<Solution> getNeighborhood(Solution currentSolution) {
        // pick a random customer to move
        int sourceRouteIdx = pickRandomVehicle(currentSolution);
        if (currentSolution.routes.get(sourceRouteIdx).size() <= 2)
            return new ArrayList<>();
        int customerSourceIdx = pickRandomCustomerFromVehicleRoute(currentSolution, sourceRouteIdx);
        int customer = currentSolution.routes.get(sourceRouteIdx).get(customerSourceIdx);

        List<Solution> neighborhood = new ArrayList<>();

        // get neighbors by moving customer to other routes
        for (int newRouteIdx = 0; newRouteIdx < currentSolution.routes.size(); newRouteIdx++) {
            if (newRouteIdx == sourceRouteIdx)
                continue;

            List<Integer> destinationRoute = currentSolution.routes.get(newRouteIdx);
            int customerDestinationIdx = 1 + random.nextInt(destinationRoute.size() - 1);

            Solution newSolution = currentSolution.copy();
            newSolution.routes.get(newRouteIdx).add(customerDestinationIdx, customer);
            newSolution.routes.get(sourceRouteIdx).remove(customerSourceIdx);
            neighborhood.add(newSolution);
        }

        // get neighbors by moving customer to other positions within current route
        List<Integer> customerRoute = currentSolution.routes.get(sourceRouteIdx);
        for (int otherCustomerIdx = 1; otherCustomerIdx < customerRoute.size() - 1; otherCustomerIdx++) {
            if (otherCustomerIdx == customerSourceIdx) // don't want to swap to current location (does nothing)
                continue;

            Solution newSolution = currentSolution.copy();
            int otherCustomer = customerRoute.get(otherCustomerIdx);

            newSolution.routes.get(sourceRouteIdx).set(customerSourceIdx, otherCustomer);
            newSolution.routes.get(sourceRouteIdx).set(otherCustomerIdx, customer);

            neighborhood.add(newSolution);
        }

        return neighborhood;
    }

    /**
     * Picks a random customer, and moves them to a random position in some route
     * (could be the same route in which they already were)
     *
     * @param currentSolution: the solution from which we are moving
     * @return the move that is made to get to the next solution
     */
    public Solution getSingleNeighbor(Solution currentSolution) {
        Solution newSolution = currentSolution.copy();

        // pick a random customer to move
        int sourceRouteIdx = pickRandomVehicle(currentSolution);
        if (currentSolution.routes.get(sourceRouteIdx).size() <= 2)
            return newSolution;
        int customerSourceIdx = pickRandomCustomerFromVehicleRoute(currentSolution, sourceRouteIdx);

        // pick a new route to move them to
        int destinationRouteIdx = random.nextInt(currentSolution.routes.size());
        // pick a random position in the destination route to move them to
        int customerDestinationIdx = 1 + random.nextInt(currentSolution.routes.get(destinationRouteIdx).size() - 1);

        // update the new solution
        int customer = currentSolution.routes.get(sourceRouteIdx).get(customerSourceIdx);
        newSolution.routes.get(destinationRouteIdx).add(customerDestinationIdx, customer);
        newSolution.routes.get(sourceRouteIdx).remove(customerSourceIdx);

        return newSolution;
    }
}
