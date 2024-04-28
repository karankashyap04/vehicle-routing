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

        Random random = new Random(100000000);
        double tolerance = Math.pow(10, Math.min(3, Double.toString(incumbentSolution.totalDistance).length() - 1));
        int restartsSinceMinTolerance = 0;
        boolean bruteForcedBefore = false;
        double lastToleranceUpdateTime = watch.getTime();

        // start moving around
        while (watch.getTime() < TIMEOUT) {
            if ((!bruteForcedBefore && restartsSinceMinTolerance >= 4) || (bruteForcedBefore && restartsSinceMinTolerance >= 2)) {
                // brute force some routes
                incumbentSolution = this.bruteForceRoutes(incumbentSolution);
                currentSolution = incumbentSolution;
                lastIncumbentUpdateTime = watch.getTime();

                restartsSinceMinTolerance = 0;
                bruteForcedBefore = true;
            }
            if (tolerance < 10)
                MULTIPLE_MOVES_NEIGHBORHOOD = true;
            if (watch.getTime() - lastIncumbentUpdateTime >= INCUMBENT_UPDATE_TIMEOUT || watch.getTime() - lastToleranceUpdateTime >= 30) {
                System.out.println("RESTART!!!!!!!!!!!!!!!!!!!!!!!!!");
                currentSolution = incumbentSolution;
                lastIncumbentUpdateTime = watch.getTime();
                tolerance = Math.max(tolerance / 2, 0.5);
                System.out.println("tolerance after restart: " + tolerance);
                lastToleranceUpdateTime = watch.getTime();
                if (tolerance == 0.5)
                    restartsSinceMinTolerance++;
            }

            Solution newSolution = move(currentSolution);
            if (newSolution.isFeasible && newSolution.totalDistance < currentSolution.totalDistance + tolerance) {
                if (newSolution.totalDistance < incumbentSolution.totalDistance) {
                    incumbentSolution = newSolution;
                    System.out.println("new incumbent (2): " + incumbentSolution.totalDistance);
                    lastIncumbentUpdateTime = watch.getTime();
//                    restartsSinceNewIncumbent = 0;
                    restartsSinceMinTolerance = 0;
                }
                currentSolution = newSolution;
            }
        }

        return incumbentSolution;
    }

    private void swap(int[] elements, int index1, int index2) {
        int temp = elements[index1];
        elements[index1] = elements[index2];
        elements[index2] = temp;
    }

    private double checkDistance(int[] elements) {
        double routeDistance = 0.0;
        for (int i = 0; i < elements.length; i++) {
            if (i == 0) {
                int customer = elements[i];
                routeDistance += this.distance[0][customer];
            } else {
                int thisCustomer = elements[i];
                int prevCustomer = elements[i-1];
                routeDistance += this.distance[prevCustomer][thisCustomer];
            }
        }
        routeDistance += this.distance[elements[elements.length - 1]][0];
        return routeDistance;
    }

    // brute forces short routes -- essentially brute forcing TSP
    private Solution bruteForceRoutes(Solution currentSolution) {
        System.out.println("brute force short routes");
        for (int routeIdx = 0; routeIdx < currentSolution.routes.size(); routeIdx++) {
            List<Integer> oldRoute = currentSolution.routes.get(routeIdx);

            // if size <= 4 -> no room for improvement
            // if size > 10 -> brute force will take too long
            if (oldRoute.size() <= 4 || oldRoute.size() > 10)
                continue;

            // compute distance of old route
            double oldRouteDistance = 0;
            for (int i = 1; i < oldRoute.size(); i++) {
                int thisCustomer = oldRoute.get(i);
                int prevCustomer = oldRoute.get(i-1);
                oldRouteDistance += this.distance[prevCustomer][thisCustomer];
            }

            // printing
            System.out.println("old route:");
            for (int customer : oldRoute) {
                System.out.print(customer + " -> ");
            }

            // generate all permutations of customers in route (reference: https://www.baeldung.com/java-array-permutations)
            int[] elements = new int[oldRoute.size() - 2]; // don't include 0s here
            for (int i = 1; i < oldRoute.size() - 1; i++) {
                elements[i-1] = oldRoute.get(i);
            }

            int[] indexes = new int[elements.length];

            int i = 0;
            while (i < indexes.length) {
                if (indexes[i] < i) {
                    swap(elements, i % 2 == 0 ?  0: indexes[i], i);
                    double permutationDistance = checkDistance(elements);
                    if (permutationDistance < oldRouteDistance) {
                        System.out.println("better solution found");
                        Solution newSolution = currentSolution.copy();
                        List<Integer> route = newSolution.routes.get(routeIdx);
                        for (int customerIdx = 1; customerIdx < route.size() - 1; customerIdx++) {
                            route.set(customerIdx, elements[customerIdx-1]);
                        }
                        newSolution.totalDistance += (permutationDistance - oldRouteDistance);
                        System.out.println("new incumbent: " + newSolution.totalDistance);

                        // printing
                        System.out.println("new route:");
                        for (int customer : route) {
                            System.out.print(customer + " -> ");
                        }

                        return newSolution;
                    }
                    indexes[i]++;
                    i = 0;
                }
                else {
                    indexes[i] = 0;
                    i++;
                }
            }
        }

        return currentSolution;
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
