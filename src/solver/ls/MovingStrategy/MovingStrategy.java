package solver.ls.MovingStrategy;

import solver.ls.Solution;

import java.util.List;

public interface MovingStrategy {
    List<Move> getNeighborhoodMoves(Solution currentSolution);
}
