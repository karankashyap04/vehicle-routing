package solver.ls;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SolutionFileGenerator {
    public static void generate(Solution solution, double computedDistance, int flag, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        String finalString = Double.toString(computedDistance) + " " + Integer.toString(flag) + "\n";

        for (List<Integer> route : solution.routes) {
            List<String> routeStringList = new ArrayList<>();
            for (int customer : route) {
                routeStringList.add(Integer.toString(customer));
            }
            finalString += String.join(" ", routeStringList) + "\n";
        }

        finalString = finalString.substring(0, finalString.length() - 1); // remove extra \n at the end

        writer.write(finalString);

        writer.close();
    }
}
