package solver.ls.MovingStrategy;

import solver.ls.Solution;
import solver.ls.VRPLocalSearch;

import java.util.List;

public class TwoOptWithCrossRouteCustomerMove implements MovingStrategy {

    private final CrossRouteCustomerMove crossRouteCustomerMove;
    private final TwoOpt twoOpt;
    private static int countSinceLastCrossMove = 0;
    private final int TWO_OPT_BETWEEN_CROSS_MOVE_COUNT = 5;

    public TwoOptWithCrossRouteCustomerMove() {
        this.crossRouteCustomerMove = new CrossRouteCustomerMove();
        this.twoOpt = new TwoOpt();
    }

    public Solution getSingleNeighbor(Solution currentSolution, VRPLocalSearch instance) {
        if (countSinceLastCrossMove < TWO_OPT_BETWEEN_CROSS_MOVE_COUNT) {
            // do a two-opt
            countSinceLastCrossMove++;
            return twoOpt.getSingleNeighbor(currentSolution, instance);
        }
        else {
            // do cross route move
            countSinceLastCrossMove = 0;
            return crossRouteCustomerMove.getSingleNeighbor(currentSolution, instance);
        }
    }
}
