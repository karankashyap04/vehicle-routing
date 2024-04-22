# vehicle-routing

## Some Initial Thoughts

- **Multithreading**: we can use multithreading (with a thread pool) for calculating scores for different neighbors in a neighborhood to speed things up (while identifying where to move next in the solution space)
- **Constructing initial solution**: maybe we could use IBM's CP solver (or maybe CPLEX) to construct the initial solution -- I think we could formulate the constraints like those for a binpacking problem (vehicles correspond to bins). CP might also have an inbuilt binpacking constraint, which could be helpful.


## Thoughts About IP Model

**NOTE** We eventually discarded our attempt to make an IP model; we realized this wasn't helpful/straightforward -- using Google's
OR tools VRP solver to get rough approximations of optimal results was a better option.

Attempt 1:
- Vehicles numbered 1 to numVehicles (0 in matrix represents no vehicle assigned)
- `numCustomers x numCustomers` matrix of decision vars -- to represent the edges in the graph. The value stored represents which vehicle traversed the edge
- `numTrucks` array of decision vars -- represents trucks used in solution
- Constraints
  - Paths start at the warehouse -- constrain using `vehicleUsed` decision variable
  - Paths end at the warehouse -- might not need to include that it actually returns in constraints; just add dist to warehouse in optimization function
  - Every node should have at least one incoming edge (sum of column is greater than or equal to 1)
    - We expect in minimization cplex will not assign multiple vehicles to visit the same customer

Attempt 2:
- Decision vars
  - `numCustomers x (numCustomers - 1)` matrix of values. Each value represents an edge in the graph. The value stored represents the number of times the edge is traversed
    - `matrix[i][j]` is whether a vehicle went from customer `i` to customer `j`
    - Each value is 0 or 1 (except for vars on the diagonal -- these values _must_ be 0)
    - #columns is `numCustomers - 1`: this is because we will not make paths go back to the warehouse in the model. We will just add back that cost 
- Constraints
  - All customers must have one incoming and one outgoing edge -- entries not in the first row or column must have sum of column and sum of row equal to 1
  - The sum of the 0th row must be the same as the sum of the 0th column (each vehicle must begin and end at the warehouse)


## Next Steps 

Local Search Attempt 1:
- Define CP model to get initial candidate solution
- Define moving mechanism to construct neighborhood
  - Move one customer to same index in all the paths
  - Move one customer to all indices of a single path
- Filter our infeasible neighbors
- Take best improvement from feasible neighbors / random choice with some probability 

TODO:
- Set up CP solver
- Solution class (to store solution)
- Solution verification class (feasibility checker)


## Local Search Exploration Ideas
- Move random customer to all other positions in this route, and same position in other routes
- 2-opt + random customer move: once we have a random customer move that succeeds in moving across routes, run 2-opt a bunch of times before allowing another cross-route movement