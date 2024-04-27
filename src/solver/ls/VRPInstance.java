package solver.ls;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

public class VRPInstance {
    // VRP Input Parameters
    int numCustomers;                // the number of customers
    int numVehicles;            // the number of vehicles
    int vehicleCapacity;            // the capacity of the vehicles
    public int[] demandOfCustomer;        // the demand of each customer
    double[] xCoordOfCustomer;    // the x coordinate of each customer
    double[] yCoordOfCustomer;    // the y coordinate of each customer
    public double[][] distance;        // distances between all customers (including warehouse)

    Timer watch;


    public VRPInstance(String fileName, Timer watch) {
        this.watch = watch;

        Scanner read = null;
        try {
            read = new Scanner(new File(fileName));
        } catch (FileNotFoundException e) {
            System.out.println("Error: in VRPInstance() " + fileName + "\n" + e.getMessage());
            System.exit(-1);
        }

        numCustomers = read.nextInt();
        numVehicles = read.nextInt();
        vehicleCapacity = read.nextInt();

        System.out.println("Number of customers: " + numCustomers);
        System.out.println("Number of vehicles: " + numVehicles);
        System.out.println("Vehicle capacity: " + vehicleCapacity);

        demandOfCustomer = new int[numCustomers];
        xCoordOfCustomer = new double[numCustomers];
        yCoordOfCustomer = new double[numCustomers];

        for (int i = 0; i < numCustomers; i++) {
            demandOfCustomer[i] = read.nextInt();
            xCoordOfCustomer[i] = read.nextDouble();
            yCoordOfCustomer[i] = read.nextDouble();
        }

        for (int i = 0; i < numCustomers; i++)
            System.out.println(demandOfCustomer[i] + " " + xCoordOfCustomer[i] + " " + yCoordOfCustomer[i]);

        this.generateDistanceMatrix();

        System.out.println("\n Matrix:\n");
        for (int i = 0; i < numCustomers; i++) {
            for (int j = 0; j < numCustomers; j++) {
                System.out.print(this.distance[i][j] + " ");
            }
            System.out.println();
        }
    }

    public int getNumCustomers() {
        return numCustomers;
    }

    public int getVehicleCapacity() {
        return vehicleCapacity;
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private void generateDistanceMatrix() {
        this.distance = new double[numCustomers][numCustomers];
        for (int i = 0; i < numCustomers; i++) {
            for (int j = 0; j < numCustomers; j++) {
                if (i == j) {
                    distance[i][j] = 0;
                    continue;
                }
                double x1 = xCoordOfCustomer[i];
                double y1 = yCoordOfCustomer[i];
                double x2 = xCoordOfCustomer[j];
                double y2 = yCoordOfCustomer[j];
                double dist = this.distance(x1, y1, x2, y2);
                this.distance[i][j] = dist;
            }
        }
    }

    public boolean isSolutionFeasible(Solution solution) {
        // there should be some route for each vehicle
        if (solution.routes.size() != numVehicles)
            return false;

        // each route should begin and end at the depot (and therefore, each route should have at least 2 locations)
        for (List<Integer> route : solution.routes) {
            if (route.size() < 2)
                return false;
            if (route.get(0) != 0 || route.get(route.size() - 1) != 0)
                return false;
        }

        // every customer is visited exactly once
        int[] visitedCustomers = new int[numCustomers]; // use array for membership tracking and checking -- faster than hashing with a set
        for (List<Integer> route : solution.routes) {
            for (int customer : route) {
                if (customer != 0 && visitedCustomers[customer] == 1) // customer was visited more than once
                    return false;
                visitedCustomers[customer] = 1;
            }
        }
        for (int customer = 1; customer < numCustomers; customer++) {
            if (visitedCustomers[customer] == 0) // customer was not visited by any vehicle
                return false;
        }

        // no vehicle should exceed its capacity
        for (List<Integer> route : solution.routes) {
            int capacityUsed = 0;
            for (int customer : route) {
                capacityUsed += demandOfCustomer[customer];
                if (capacityUsed > vehicleCapacity)
                    return false;
            }
        }

        return true;
    }

    public double solutionTotalDistance(Solution solution) {
        double totalDistance = 0.0;
        for (List<Integer> route : solution.routes) {
            for (int customerIdx = 1; customerIdx < route.size(); customerIdx++) {
                int prevCustomer = route.get(customerIdx - 1);
                int thisCustomer = route.get(customerIdx);
                totalDistance += distance[prevCustomer][thisCustomer];
            }
        }
        solution.totalDistance = totalDistance;
        return totalDistance;
    }
}
