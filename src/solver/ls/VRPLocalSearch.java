package solver.ls;

import ilog.concert.IloException;
import ilog.cp.*;
import ilog.concert.*;
import solver.ls.MovingStrategy.MovingStrategy;

import java.util.ArrayList;
import java.util.List;

public class VRPLocalSearch extends VRPInstance {

    IloCP cp;
    IloIntVar[][] customersServed; // (numVehicles, numCustomers - 1) --> (i, j): if vehicle i serves customer j

    Solution incumbentSolution;

    public VRPLocalSearch(String filename) {
        super(filename);
    }

    public Solution constructInitialSolution() {
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

    // TODO: define search function (for local search)

    /**
     * Moves within the solution space
     * @return new solution reached (after move)
     */
    private List<List<Integer>> move(MovingStrategy movingStrategy, Solution currentSolution) {
        // based on moving strategy, get neighborhood

        // evaluate solutions in neighborhood
        // - maybe only consider feasible solutions
        // - maybe consider feasible and infeasible with some penalty
        // - Serdar mentioned reading about local search "fitness"

        // return "best" one
        // - don't always have to return the best; maybe random walk with some probability
    }
}
