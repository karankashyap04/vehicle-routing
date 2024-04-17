package solver.ls;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.Scanner;

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
}
