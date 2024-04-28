package solver.ls;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class Checker {
    public static void check(VRPInstance instance, Solution solution, double computedDistance) {
        // feasibility check
        // 1. no capacity constraint is violated
        for (List<Integer> route : solution.routes) {
            int demandServed = 0;
            for (int customer : route) {
                demandServed += instance.demandOfCustomer[customer];
            }

            if (demandServed > instance.vehicleCapacity) {
                System.out.println("failed -- capacity constraint not met!");
                System.exit(-1);
            }
        }

        // 2. every customer is seen exactly once (ignore warehouse for this)
        Set<Integer> seenCustomers = new HashSet<>();
        for (List<Integer> route : solution.routes) {
            for (int customer : route) {
                if (customer == 0)
                    continue;

                if (seenCustomers.contains(customer)) {
                    System.out.println("failed -- customer" + customer + " visited by multiple vehicles");
                    System.exit(-1);
                }

                seenCustomers.add(customer);
            }
        }

        // 3. every route begins and ends in warehouse (and has at least 2 customers)
        for (List<Integer> route : solution.routes) {
            if (route.size() < 2) {
                System.out.println("failed -- route has less than 2 customers");
                System.exit(-1);
            }

            if (route.get(0) != 0 || route.get(route.size() - 1) != 0) {
                System.out.println("failed -- route doesn't begin and end at warehouse");
                System.exit(-1);
            }
        }


        // distance check
        // total distance of the solution string matches the given total distance in the log file
        double totalDistance = 0;
        for (List<Integer> route : solution.routes) {
            if (route.size() == 2)
                continue;

            for (int i = 1; i < route.size(); i++) {
                int previousCustomer = route.get(i - 1);
                int currCustomer = route.get(i);

                double prevX = instance.xCoordOfCustomer[previousCustomer];
                double prevY = instance.yCoordOfCustomer[previousCustomer];

                double currX = instance.xCoordOfCustomer[currCustomer];
                double currY = instance.yCoordOfCustomer[currCustomer];

                totalDistance += Math.sqrt(Math.pow(currX - prevX, 2) + Math.pow(currY - prevY, 2));
            }
        }


        if (computedDistance != Double.parseDouble(String.format("%.2f", totalDistance))) {
            System.out.println("failed -- computed distance not accurate!");
            System.out.println("computed total distance: " + computedDistance);
            System.out.println("actual distance: " + Double.parseDouble(String.format("%.2f", totalDistance)));
            System.exit(-1);
        }
    }

    public static void main(String[] args) throws Exception {
        // read results.log
        String resultsPath = "resultsNoMultithreading.log";
        BufferedReader reader = new BufferedReader(new FileReader(resultsPath));

        String line = reader.readLine();

        while (line != null) {

            String[] splitLine = line.split(", ");

            String filename = splitLine[0];
            filename = "input/" + filename.substring(filename.indexOf(":") + 3, filename.length() - 1);
            System.out.println("filename: " + filename);

            double computedDistance = Double.parseDouble(splitLine[2].substring(splitLine[2].indexOf(":") + 2));

            String solutionString = splitLine[3].substring(splitLine[3].indexOf(":") + 3, splitLine[3].length() - 2);
            String[] solutionArray = solutionString.split(" ");
            int flag = Integer.parseInt(solutionArray[0]);

            List<List<Integer>> routes = new ArrayList<>();
            boolean routeStarted = false;
            for (int i = 1; i < solutionArray.length; i++) {
                int customer = Integer.parseInt(solutionArray[i]);
                if (customer == 0 && !routeStarted) {
                    routeStarted = true;
                    routes.add(new ArrayList<>());
                    routes.get(routes.size() - 1).add(customer);
                } else if (customer == 0 && routeStarted) {
                    routeStarted = false;
                    routes.get(routes.size() - 1).add(customer);
                } else {
                    routes.get(routes.size() - 1).add(customer);
                }
            }

            Solution solution = new Solution (routes);

            String solFilename = "solutions/" + filename.substring(filename.indexOf("/") + 1) + ".sol";
            SolutionFileGenerator.generate(solution, computedDistance, flag, solFilename);

            Timer watch = new Timer();
            VRPInstance instance = new VRPInstance(filename, watch);

            check(instance, solution, computedDistance);

            line = reader.readLine();
        }

        reader.close();
    }
}
