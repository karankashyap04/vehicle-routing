package solver.ls.MovingStrategy;

import solver.ls.Solution;

import java.util.List;

public interface MovingStrategy {
    List<Solution> getNeighborhood(Solution currentSolution);

    Solution getSingleNeighbor(Solution currentSolution);
}
