# vehicle-routing

## Some Initial Thoughts

- **Multithreading**: we can use multithreading (with a thread pool) for calculating scores for different neighbors in a neighborhood to speed things up (while identifying where to move next in the solution space)
- **Constructing initial solution**: maybe we could use IBM's CP solver (or maybe CPLEX) to construct the initial solution -- I think we could formulate the constraints like those for a binpacking problem (vehicles correspond to bins). CP might also have an inbuilt binpacking constraint, which could be helpful.
