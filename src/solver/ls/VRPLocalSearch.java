package solver.ls;

import ilog.concert.IloException;
import ilog.cp.*;
import ilog.concert.*;
import solver.ls.MovingStrategy.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class VRPLocalSearch extends VRPInstance {

    IloCP cp;
    IloIntVar[][] customersServed; // (numVehicles, numCustomers - 1) --> (i, j): if vehicle i serves customer j

    Solution incumbentSolution;
    private double lastIncumbentUpdateTime = 0.0;
    private double INCUMBENT_UPDATE_TIMEOUT = 10.0; // 10 seconds

    MovingStrategy movingStrategy;
    final double TIMEOUT = 295.0; // stop running search after 295 seconds

    // reference: https://stackoverflow.com/questions/3269445/executorservice-how-to-wait-for-all-tasks-to-finish
    /*
     * if this flag is true, we get lists of moves from moving strategies.
     * if it is false, we get single moves from the moving strategies in singleMovingStrategies
     */
    private boolean MULTIPLE_MOVES_NEIGHBORHOOD = false;

    private List<MovingStrategy> singleMovingStrategies;

    public VRPLocalSearch(String filename, MovingStrategy movingStrategy, Timer watch) {
        super(filename, watch);
        this.movingStrategy = movingStrategy;
//        if (!MULTIPLE_MOVES_NEIGHBORHOOD) {
        this.singleMovingStrategies = new ArrayList<>(List.of(
                new TwoOpt(),
                new CrossRouteCustomerMove(),
                new RandomCustomerMovement(),
                new CrossRouteCustomerExchange()
        ));
//        }
    }

    private Solution constructSolutionFromCPVars() {
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
        return new Solution(routes);
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
                incumbentSolution = constructSolutionFromCPVars();
                lastIncumbentUpdateTime = watch.getTime();
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
     * @return a Solution: the most optimal feasible solution found via local search
     */
    public Solution localSearch() {
        // construct initial solution
        Solution currentSolution = constructInitialSolution();
        if (currentSolution == null) {
            System.out.println("Error: problem is infeasible!");
            return null;
        }

        solutionTotalDistance(currentSolution); // compute solution total distance (stored in totalDistance field)
        System.out.println("initial solution: " + currentSolution.totalDistance);

//        Random random = new Random(100000000);
        double tolerance = Math.pow(10, Math.min(3, Double.toString(incumbentSolution.totalDistance).length() - 1));
//        int restartsSinceNewIncumbent = 0;

        // start moving around
        while (watch.getTime() < TIMEOUT) {
            if (tolerance < 10)
                MULTIPLE_MOVES_NEIGHBORHOOD = true;
            if (watch.getTime() - lastIncumbentUpdateTime >= INCUMBENT_UPDATE_TIMEOUT) {
                System.out.println("RESTART!!!!!!!!!!!!!!!!!!!!!!!!!");
                currentSolution = incumbentSolution;
                lastIncumbentUpdateTime = watch.getTime();
                tolerance = Math.max(tolerance / 2, 0.5);
                System.out.println("tolerance after restart: " + tolerance);
                this.singleMovingStrategies = new ArrayList<>(List.of(
                        new TwoOpt(),
                        new CrossRouteCustomerMove(),
                        new RandomCustomerMovement(),
                        new CrossRouteCustomerExchange()
                ));
//                restartsSinceNewIncumbent++;
            }

            Solution newSolution = move(currentSolution);
            if (newSolution.isFeasible && newSolution.totalDistance < currentSolution.totalDistance + tolerance) {
                if (newSolution.totalDistance < incumbentSolution.totalDistance) {
                    incumbentSolution = newSolution;
                    System.out.println("new incumbent (2): " + incumbentSolution.totalDistance);
                    lastIncumbentUpdateTime = watch.getTime();
//                    restartsSinceNewIncumbent = 0;
                }
                currentSolution = newSolution;
            }
        }

        return incumbentSolution;
    }

    /**
     * Moves within the solution space
     * @return new solution reached (after move)
     */
    private Solution move(Solution currentSolution) {
        List<Solution> neighborhood = new ArrayList<>();

        if (MULTIPLE_MOVES_NEIGHBORHOOD) {
            // based on moving strategy, get neighborhood
//            neighborhoodMoves = this.movingStrategy.getNeighborhoodMoves(currentSolution);
            for (MovingStrategy strategy : this.singleMovingStrategies) {
//                neighborhood.addAll(strategy.getNeighborhood(currentSolution));
                for (int i = 0; i < 10; i++)
                    neighborhood.add(strategy.getSingleNeighbor(currentSolution, this));
            }
        } else {
//            neighborhoodMoves = new ArrayList<>();
            for (MovingStrategy strategy : this.singleMovingStrategies) {
                neighborhood.add(strategy.getSingleNeighbor(currentSolution, this));
            }
        }

        Solution bestNeighbor = null;
        for (Solution neighbor : neighborhood) {
            if (!neighbor.isFeasible)
                continue;
            if (bestNeighbor == null || neighbor.totalDistance < bestNeighbor.totalDistance)
                bestNeighbor = neighbor;
        }

        if (bestNeighbor == null) {
            System.out.println("Couldn't move: no feasible neighbors!");
            return incumbentSolution;
        }
        return bestNeighbor;
    }
}