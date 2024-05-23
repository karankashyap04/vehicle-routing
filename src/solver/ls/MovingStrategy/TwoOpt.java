package solver.ls.MovingStrategy;

import solver.ls.Solution;
import solver.ls.VRPLocalSearch;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.max;
import static java.lang.Math.min;

// NOTE: reference: https://www.ncbi.nlm.nih.gov/pmc/articles/PMC8482434/
public class TwoOpt implements MovingStrategy {
    // example: 0 -> 5 -> 1 -> 2 -> 3 -> 4 -> 6 -> 0
    // might become: 0 -> 5 -> 1 -> 3 -> 2 -> 4 -> 6 -> 0 (if we remove the cross along the arc 2 -> 3)
    // (2 and 3 here are randomly picked)

    private final Random random = new Random(550);

    private int pickCustomerFromRoute(List<Integer> route) {
        // add 1 to result to prevent picking customer at index 0 (avoid depot)
        // subtract 2 at end -- subtract 1 to avoid picking last index (depot), and
        //   subtract another 1 to account for adding 1 in the beginning
        return 1 + random.nextInt(route.size() - 2);
    }

    private Solution performTwoOpt(Solution currentSolution, int routeIdx) {
        Solution newSolution = currentSolution.copy();
        List<Integer> route = currentSolution.routes.get(routeIdx);

        if (route.size() < 4)
            return newSolution;

        // pick 2 different customers
        int custIdx1 = 0;
        int custIdx2 = 0;
        while (custIdx1 == custIdx2) {
            custIdx1 = pickCustomerFromRoute(route);
            custIdx2 = pickCustomerFromRoute(route);
        }

        // swap so customer1 idx < customer2 idx
        int startCustomerIdx = min(custIdx1, custIdx2);
        int endCustomerIdx = max(custIdx1, custIdx2);

        for (int i = startCustomerIdx; i <= endCustomerIdx; i++) {
            int toSwapCustomer = route.get(endCustomerIdx - (i - startCustomerIdx));
            newSolution.routes.get(routeIdx).set(i, toSwapCustomer);
        }

        return newSolution;
    }

    /**
     * Picks a random route, and performs a random two-opt swap within it
     *
     * @param currentSolution: the solution from which we are making the move
     * @return the move that is made to get to the next solution
     */
    public Solution getSingleNeighbor(Solution currentSolution, VRPLocalSearch instance) {
        int routeIdx = random.nextInt(currentSolution.routes.size());
        Solution newSolution = performTwoOpt(currentSolution, routeIdx);

        // run verification -- we only need to check that the two routes that were changed are still valid
        List<Integer> newRoute = newSolution.routes.get(routeIdx);
        newSolution.isFeasible = this.isRouteFeasible(newRoute, instance);
        if (!newSolution.isFeasible) {
            // no need to update total distance here -- we only consider feasible solutions so this will be discarded
            return newSolution;
        }

        // compute new total distance -- we can compute this by seeing the change in distance for the two
        // modified routes
        List<Integer> oldRoute = currentSolution.routes.get(routeIdx);
        newSolution.totalDistance += this.routeDistanceChange(oldRoute, newRoute, instance);

        return newSolution;
    }
}
