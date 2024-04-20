package solver.ls;

import ilog.concert.IloException;
import ilog.cp.*;
import ilog.concert.*;
import solver.ls.MovingStrategy.Move;
import solver.ls.MovingStrategy.MovingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class VRPLocalSearch extends VRPInstance {

    IloCP cp;
    IloIntVar[][] customersServed; // (numVehicles, numCustomers - 1) --> (i, j): if vehicle i serves customer j

    Solution incumbentSolution;
    MovingStrategy movingStrategy;
    final double TIMEOUT = 297.0; // stop running search after 297 seconds

    // reference: https://stackoverflow.com/questions/3269445/executorservice-how-to-wait-for-all-tasks-to-finish
    final int NUM_THREADS = 10;
    ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);

    public VRPLocalSearch(String filename, MovingStrategy movingStrategy, Timer watch) {
        super(filename, watch);
        this.movingStrategy = movingStrategy;
    }

    private Solution constructInitialSolution() {
        try {
            cp = new IloCP();

            // routes array
            customersServed = new IloIntVar[numVehicles][numCustomers];
            for (int i = 0; i < numVehicles; i++) {
                customersServed[i] = cp.intVarArray(numCustomers, 0, 1);
            }

            // every column should sum to 1 -- each customer is visited exactly once
            for (int j = 1; j < numCustomers; j++) {
                IloNumExpr sum = cp.constant(0);
                for (int i = 0; i < numVehicles; i++) {
                    sum = cp.sum(sum, customersServed[i][j]);
                }

                cp.addEq(sum, 1);
            }

            // no vehicle exceeds its capacity
            for (int i = 0; i < numVehicles; i++) {
                cp.addLe(cp.scalProd(customersServed[i], demandOfCustomer), vehicleCapacity);
            }

            if (cp.solve()) {
                List<List<Integer>> routes = new ArrayList<>();
                for (int i = 0; i < numVehicles; i++) {
                    List<Integer> vehicleRoute = new ArrayList<>();
                    vehicleRoute.add(0);
                    for (int j = 1; j < numCustomers; j++) {
                        int isCustomerServed = (int) cp.getValue(customersServed[i][j]);
                        if (isCustomerServed == 1) {
                            vehicleRoute.add(j);
                        }
                    }
                    vehicleRoute.add(0);
                    routes.add(vehicleRoute);
                }

                incumbentSolution = new Solution(routes);
                return incumbentSolution;
            } else {
                System.out.println("Problem is infeasible!");
                return null;
            }

        } catch (IloException e) {
            System.out.println("Error: " + e);
            return null;
        }
    }

    /**
     * Performs local search to try and find an optimal solution
     * (NOTE: since local search is an incomplete method, there is no guarantee of optimality)
     *
     * @return Solution: the most optimal feasible solution found via local search
     */
    public Solution localSearch() {
        // construct initial solution
        Solution currentSolution = constructInitialSolution();
        if (currentSolution == null) {
            System.out.println("Error: problem is infeasible!");
            return null;
        }

        solutionTotalDistance(currentSolution); // compute solution total distance (stored in totalDistance field)

        // start moving around
        while (watch.getTime() < TIMEOUT) {
            Solution newSolution = move(currentSolution);
            if (newSolution.isFeasible && newSolution.totalDistance < incumbentSolution.totalDistance)
                incumbentSolution = newSolution;
            currentSolution = newSolution;
        }

        threadPool.shutdown();
        return incumbentSolution;
    }

    /**
     * Moves within the solution space
     * @return new solution reached (after move)
     */
    private Solution move(Solution currentSolution) {
        // based on moving strategy, get neighborhood
        List<Move> neighborhoodMoves = this.movingStrategy.getNeighborhoodMoves(currentSolution);

        // evaluate solutions in neighborhood
        List<Callable<Solution>> moveTasks = new ArrayList<>();
        for (Move neighborhoodMove : neighborhoodMoves) {
            moveTasks.add(new MoveTask(currentSolution, neighborhoodMove, this));
        }
        try {
            List<Future<Solution>> neighborhood = threadPool.invokeAll(moveTasks);

            // return "best" one
            // - don't always have to return the best; maybe random walk with some probability
            // - maybe only consider feasible solutions
            // - maybe consider feasible and infeasible with some penalty
            // - Serdar mentioned reading about local search "fitness"
            // TODO: for now, just picking best feasible neighbor; this will definitely need to change, since it could
            //       lead to a lot of issues (ex: what if there are no feasible moves from the current location, etc)
            Solution bestNeighbor = null;
            for (Future<Solution> futureNeighbor : neighborhood) {
                Solution neighbor = futureNeighbor.get();
                if (!neighbor.isFeasible)
                    continue;
                if (bestNeighbor == null || neighbor.totalDistance < bestNeighbor.totalDistance)
                    bestNeighbor = neighbor;
            }

            if (bestNeighbor == null) {
                // TODO: we should do something better in this situation; this current situation will lead to it
                //       repeatedly arriving at this same point (stuck in a local minima)
                System.out.println("Couldn't move: no feasible neighbors!");
                return incumbentSolution;
            }
            return bestNeighbor;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
