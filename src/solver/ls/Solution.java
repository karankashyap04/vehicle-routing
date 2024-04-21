package solver.ls;

import java.util.ArrayList;
import java.util.List;

public class Solution {

    public List<List<Integer>> routes;
    public double totalDistance;
    public boolean isFeasible;

    public Solution(List<List<Integer>> routes) {
        this.routes = routes;
    }

    public Solution(List<List<Integer>> routes, double totalDistance) {
        this.routes = routes;
        this.totalDistance = totalDistance;
    }

    public Solution(List<List<Integer>> routes, double totalDistance, boolean isFeasible) {
        this.routes = routes;
        this.totalDistance = totalDistance;
        this.isFeasible = isFeasible;
    }

    public Solution copy() {
        List<List<Integer>> copyRoutes = new ArrayList<>();
        for (List<Integer> route : this.routes) {
            copyRoutes.add(new ArrayList<>(route));
        }
        return new Solution(copyRoutes, this.totalDistance, this.isFeasible);
    }

    public String getSolutionString() {
        StringBuilder result = new StringBuilder("0 "); // begin with 0 (since local search doesn't provide optimality guarantees)

        for (List<Integer> route : this.routes) {
            for (int location : route) {
                result.append(location).append(" ");
            }
        }

        return result.toString().trim(); // remove whitespace from the end
    }
}
