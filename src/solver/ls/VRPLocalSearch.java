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
    final int NUM_THREADS = 10;
    ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);

    /*
     * if this flag is true, we get lists of moves from moving strategies.
     * if it is false, we get single moves from the moving strategies in singleMovingStrategies
     */
    private final boolean MULTIPLE_MOVES_NEIGHBORHOOD = false;

    private List<MovingStrategy> singleMovingStrategies;

    public VRPLocalSearch(String filename, MovingStrategy movingStrategy, Timer watch) {
        super(filename, watch);
        this.movingStrategy = movingStrategy;
//        if (!MULTIPLE_MOVES_NEIGHBORHOOD) {
        this.singleMovingStrategies = new ArrayList<>(List.of(
                new TwoOpt(), new CrossRouteCustomerMove(), new RandomCustomerMovement(), new CrossRouteCustomerExchange()
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
        // start moving around
        while (watch.getTime() < TIMEOUT) {
            if (watch.getTime() - lastIncumbentUpdateTime >= INCUMBENT_UPDATE_TIMEOUT) {
                System.out.println("RESTART!!!!!!!!!!!!!!!!!!!!!!!!!");
                boolean foundSolution = false;
                try {
                    cp.setParameter(IloCP.IntParam.RandomSeed, random.nextInt(1000000));
                    foundSolution = cp.solve();
//                    cp.startNewSearch();
//                    foundSolution = cp.next();

                } catch(IloException e) {
                    System.out.println("unexpected failure from cp.next");
                }
                if (foundSolution) {
                    currentSolution = constructSolutionFromCPVars();
                    System.out.println(currentSolution.getSolutionString());
                    solutionTotalDistance(currentSolution);
                    if (currentSolution.totalDistance <= incumbentSolution.totalDistance) {
                        incumbentSolution = currentSolution;
                        System.out.println("new incumbent (1): " + incumbentSolution.totalDistance);
                    }
                    lastIncumbentUpdateTime = watch.getTime();
                    continue;
                }
            }

            Solution newSolution = move(currentSolution);
            if (newSolution.isFeasible && newSolution.totalDistance < currentSolution.totalDistance + 0.5) {
                if (newSolution.totalDistance < incumbentSolution.totalDistance) {
                    incumbentSolution = newSolution;
                    System.out.println("new incumbent (2): " + incumbentSolution.totalDistance);
                    lastIncumbentUpdateTime = watch.getTime();
                }
                currentSolution = newSolution;
            }

//            Solution newSolution = move(currentSolution);
//            if (newSolution.isFeasible && newSolution.totalDistance < incumbentSolution.totalDistance) {
//                incumbentSolution = newSolution;
//                System.out.println("new incumbent (2): " + incumbentSolution.totalDistance);
//                lastIncumbentUpdateTime = watch.getTime();
//            }
//
//            currentSolution = newSolution;
        }

        threadPool.shutdown();
        return incumbentSolution;
    }

    /**
     * Moves within the solution space
     * @return new solution reached (after move)
     */
    private Solution move(Solution currentSolution) {
        List<Move> neighborhoodMoves = new ArrayList<>();

        if (MULTIPLE_MOVES_NEIGHBORHOOD) {
            // based on moving strategy, get neighborhood
//            neighborhoodMoves = this.movingStrategy.getNeighborhoodMoves(currentSolution);
            for (MovingStrategy strategy : this.singleMovingStrategies) {
                neighborhoodMoves.addAll(strategy.getNeighborhoodMoves(currentSolution));
            }
        } else {
//            neighborhoodMoves = new ArrayList<>();
            for (MovingStrategy strategy : this.singleMovingStrategies) {
                neighborhoodMoves.add(strategy.getSingleNeighbor(currentSolution));
            }
        }

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
