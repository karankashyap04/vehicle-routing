package solver.ls;

import solver.ls.MovingStrategy.Move;

import java.util.concurrent.Callable;

public class MoveTask implements Callable<Solution> {

    private Solution solution;
    private Move move;
    private final VRPLocalSearch vrpLocalSearch;

    public MoveTask(Solution prevSolution, Move move, VRPLocalSearch vrpLocalSearch) {
        this.solution = prevSolution;
        this.move = move;
        this.vrpLocalSearch = vrpLocalSearch;
    }

    @Override
    public Solution call() {
        // make a deep copy of the solution -- prevent issues when mutating (as we perform the move)
        solution = solution.copy();

        // remove the customer from the old position
        int customer = solution.routes.get(move.prevVehicle()).remove(move.prevCustomerRouteIdx());
        // add the customer to the new position
        solution.routes.get(move.nextVehicle()).add(move.nextCustomerRouteIdx(), customer);

        // compute feasibility and total distance
        solution.isFeasible = vrpLocalSearch.isSolutionFeasible(solution);
        vrpLocalSearch.solutionTotalDistance(solution);

        return solution;
    }
}
