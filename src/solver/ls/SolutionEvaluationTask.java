package solver.ls;

import java.util.concurrent.Callable;

public class SolutionEvaluationTask implements Callable<Solution> {

    private Solution solution;
    private final VRPLocalSearch vrpLocalSearch;

    public SolutionEvaluationTask(Solution prevSolution, VRPLocalSearch vrpLocalSearch) {
        this.solution = prevSolution;
        this.vrpLocalSearch = vrpLocalSearch;
    }

    @Override
    public Solution call() {
        // compute feasibility and total distance
        solution.isFeasible = vrpLocalSearch.isSolutionFeasible(solution);
        vrpLocalSearch.solutionTotalDistance(solution);
        return solution;
    }
}
