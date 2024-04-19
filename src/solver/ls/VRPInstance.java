package solver.ls;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class VRPInstance {
    // VRP Input Parameters
    int numCustomers;                // the number of customers
    int numVehicles;            // the number of vehicles
    int vehicleCapacity;            // the capacity of the vehicles
    int[] demandOfCustomer;        // the demand of each customer
    double[] xCoordOfCustomer;    // the x coordinate of each customer
    double[] yCoordOfCustomer;    // the y coordinate of each customer
    double[][] distance;        // distances between all customers (including warehouse)


    public VRPInstance(String fileName) {
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

    public boolean isSolutionFeasible(int[][] routes) {
        // there should be some route for each vehicle
        if (routes.length != numVehicles)
            return false;

        // each route should begin and end at the depot (and therefore, each route should have at least 2 locations)
        for (int[] route : routes) {
            if (route.length < 2)
                return false;
            if (route[0] != 0 || route[route.length - 1] != 0)
                return false;
        }

        // every customer is visited exactly once
        int[] visitedCustomers = new int[numCustomers]; // use array for membership tracking and checking -- faster than hashing with a set
        for (int[] route : routes) {
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
        for (int[] route : routes) {
            int capacityUsed = 0;
            for (int customer : route) {
                capacityUsed += demandOfCustomer[customer];
                if (capacityUsed > vehicleCapacity)
                    return false;
            }
        }

        return true;
    }

    public double solutionTotalDistance(int[][] routes) {
        double totalDistance = 0.0;
        for (int[] route : routes) {
            for (int customerIdx = 1; customerIdx < numCustomers; customerIdx++) {
                int prevCustomer = route[customerIdx - 1];
                int thisCustomer = route[customerIdx];
                totalDistance += distance[prevCustomer][thisCustomer];
            }
        }
        return totalDistance;
    }
}
