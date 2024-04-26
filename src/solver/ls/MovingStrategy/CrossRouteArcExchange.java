package solver.ls.MovingStrategy;

import solver.ls.Solution;
import solver.ls.VRPLocalSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This is based on the "Cross" moving strategy defined here:
 * https://citeseerx.ist.psu.edu/document?repid=rep1&type=pdf&doi=033ebeec30386fcb4232593e05b4dead972d385a
 */
public class CrossRouteArcExchange implements MovingStrategy {

    private final Random random = new Random(600000000);
    private final static List<Integer> emptyList = new ArrayList<>();

    public List<Solution> getNeighborhood(Solution currentSolution, VRPLocalSearch instance) {
        final int NEIGHBORHOOD_SIZE = currentSolution.routes.size() / 2;

        List<Solution> neighborhoodMoves = new ArrayList<>();
        for (int i = 0; i < NEIGHBORHOOD_SIZE; i++)
            neighborhoodMoves.add(getSingleNeighbor(currentSolution, instance));
        return neighborhoodMoves;
    }

    public Solution getSingleNeighbor(Solution currentSolution, VRPLocalSearch instance) {
        Solution newSolution = currentSolution.copy();
        int numVehicles = currentSolution.routes.size();
        // pick the two routes
        int route1Idx = random.nextInt(numVehicles);
        int route2Idx = random.nextInt(numVehicles);

        List<Integer> route1 = currentSolution.routes.get(route1Idx);
        List<Integer> route2 = currentSolution.routes.get(route2Idx);

        if (route1Idx == route2Idx || route1.size() <= 2 || route2.size() <= 2)
            return newSolution;

        // pick the start position of the arcs from both routes
        int route1Start = 1 + random.nextInt(route1.size() - 2);
        int route2Start = 1 + random.nextInt(route2.size() - 2);

        // remove old parts of arcs from new solution
        List<Integer> newRoute1 = newSolution.routes.get(route1Idx);
        List<Integer> newRoute2 = newSolution.routes.get(route2Idx);
        for (int i = route1Start; i < newRoute1.size() - 1; i++)
            newRoute1.remove(i);
        for (int i = route2Start; i < newRoute2.size() - 1; i++)
            newRoute2.remove(i);

        // exchange arcs
        for (int i = route2Start; i < route2.size() - 1; i++)
            newRoute1.add(newRoute1.size() - 1, route2.get(i));
        for (int i = route1Start; i < route1.size() - 1; i++)
            newRoute2.add(newRoute2.size() - 1, route1.get(i));

        // run verification -- we only need to check that the two routes that were changed are still valid
        newSolution.isFeasible = this.isRouteFeasible(newRoute1, instance) && this.isRouteFeasible(newRoute2, instance);
        if (!newSolution.isFeasible) {
            // no need to update total distance here -- we only consider feasible solutions so this will be discarded
            return newSolution;
        }

        // compute new total distance -- we can compute this by seeing the change in distance for the two
        // modified routes
        newSolution.totalDistance += this.routeDistanceChange(route1, newRoute1, instance) + this.routeDistanceChange(route2, newRoute2, instance);

        return newSolution;
    }
}