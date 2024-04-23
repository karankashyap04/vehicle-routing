package solver.ls.MovingStrategy;

import solver.ls.Solution;

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

    private final Random random = new Random();

    private int pickCustomerFromRoute(List<Integer> route) {
        // add 1 to result to prevent picking customer at index 0 (avoid depot)
        // subtract 2 at end -- subtract 1 to avoid picking last index (depot), and
        //   subtract another 1 to account for adding 1 in the beginning
        return 1 + random.nextInt(route.size() - 2);
    }

    private Move getNeighborhoodMove(List<Integer> route, int routeIdx) {
        // pick two different customers
        int custIdx1 = 0;
        int custIdx2 = 0;
        while (custIdx1 == custIdx2) {
            custIdx1 = pickCustomerFromRoute(route);
            custIdx2 = pickCustomerFromRoute(route);
        }

        // swap so customer1 idx < customer2 idx
        int startCustomerIdx = min(custIdx1, custIdx2);
        int endCustomerIdx = max(custIdx1, custIdx2);

        // generate a Move to represent this:
        List<Integer> prevVehicle = new ArrayList<>();
        List<Integer> prevCustomerRouteIdx = new ArrayList<>();
        List<Integer> nextVehicle = new ArrayList<>();
        List<Integer> nextCustomerRouteIdx = new ArrayList<>();


        for (int i = startCustomerIdx; i <= endCustomerIdx; i++) {
            prevVehicle.add(routeIdx);
            nextVehicle.add(routeIdx);

            prevCustomerRouteIdx.add(i);
            nextCustomerRouteIdx.add(endCustomerIdx - (i - startCustomerIdx));
        }

        return new Move(prevVehicle, prevCustomerRouteIdx, nextVehicle, nextCustomerRouteIdx);
    }

    public List<Move> getNeighborhoodMoves(Solution currentSolution) {
        // run once on each route
        List<Move> neighborhoodMoves = new ArrayList<>();
        for (int i = 0; i < currentSolution.routes.size(); i++) {
            List<Integer> route = currentSolution.routes.get(i);
            if (route.size() < 4)
                continue;
            neighborhoodMoves.add(getNeighborhoodMove(route, i));
        }
        return neighborhoodMoves;
    }

    /**
     * Picks a random route, and performs a random two-opt swap within it
     *
     * @param currentSolution: the solution from which we are making the move
     * @return the move that is made to get to the next solution
     */
    public Move getSingleNeighbor(Solution currentSolution) {
        int routeIdx = random.nextInt(currentSolution.routes.size());
        if (currentSolution.routes.get(routeIdx).size() < 4) {
            List<Integer> emptyList = new ArrayList<>();
            return new Move(emptyList, emptyList, emptyList, emptyList);
        }
        return getNeighborhoodMove(currentSolution.routes.get(routeIdx), routeIdx);
    }
}
