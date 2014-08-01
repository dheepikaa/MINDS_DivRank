/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.StringTokenizer;

/**
 *
 * @author minhhx
 */
public class PermutationTester {

    public static ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
    public static int noRuns = 0;

    public static void main(String[] args) throws FileNotFoundException, IOException {
        //String folder = args[0];
        for (int i = 0; i < 100; i++) {
            result.add(new ArrayList<Double>());
        }

        // this is the potential produced by the algorithm
        // balanced
//        double[] thresholds = {0.5,0.6721652950548891,0.7543103448275866,0.8338697869489422,0.8847957679530705,0.9196910213889053,0.9451379304449381,0.9636982707318811,0.9761771894685582,0.9841235442082558,0.9887028587062753,0.9913890622562154,0.9928219563687537,0.9937304075235108,1.0};

        // unbalanced?
        //double[] thresholds = {0,0.7345913657344556, 0.8054471609460849, 0.8539712531209848, 0.8900214798051506, 0.9198406384407138, 0.9435367519897923, 0.9630786816536921, 0.9762581890203962, 0.9841933758085791, 0.9901327340403103, 0.9939655172413795, 0.993730407523511, 0.9913793103448276, 1.0};

        // rerun bigger balanced
        //double[] thresholds = {0.7650273224043715,0.7814207650273224,0.7868852459016393,0.8142076502732241,0.825136612021858,0.8469945355191257,0.8524590163934426,0.8633879781420765,0.8743169398907104,0.9143897996357012,0.9672131147540981,0.9849132810643859,0.9907719918568527,0.994968444547061,0.9971786186267746,0.9981259467200196,0.998737949399039,0.9991615045004943,0.9992749828815405,0.9990833774017274,0.9985016745989777,0.998360655737705,0.9990064580228514,1.0,1.0};
        
        // for Nazli
        double[] thresholds = {0.5,0.5,0.5,0.5,0.7037037037037037,0.7400793650793649,0.793803418803418,0.8817242189335237,0.9307692307692405,0.9536169190098504,0.9664791373652148,0.973790322580649,0.9801018783826829,0.98661800486618,0.9902777777777778,1.0};
        // end for Nazli

//        // read from null hypothesis file for global state permute
//        String fileName = "";
//        for (int i = 1; i <= 9; i++) {
//            for (int j = 1; j <= 10; j++) {
//                fileName = folder + "/err" + Integer.toString(j) + ".txt";
//                readFile(fileName);
//            }
//        }
/*
        // read from null hypothesis file of rexp level permute
        String fileName = "";
        for (int j = 1; j <= 1000; j++) {
            try {
                fileName = folder + "/err" + Integer.toString(j) + ".txt";
                readFile(fileName);
            } catch (Exception e) {
//                System.out.println(e);
            }
        }
*/
        // for Nazli
        readFile("/home/minhhx/Downloads/random_node.txt");
        // end for Nazli
//        // sort results and print
//        for (int i = 0; i < result.size(); i++){
//            ArrayList<Double> resultSize = result.get(i);
//            Collections.sort(resultSize);
//            System.out.println("Size " + i + ":");
//            for (int j = 0; j < resultSize.size(); j++){
//                System.out.print("," + resultSize.get(j));
//            }
//            System.out.println();
//        }

        // we compare the above potential with the null hypothesis (sampling with random labels)
        System.out.println("\tnoRuns " + noRuns);
        int count = 0;
        double avg = 0.0;
        for (int size = 1; size <= 14; size++) {
            avg = 0.0;
            count = 0;
            ArrayList<Double> resultSize = result.get(size);
            for (int j = 0; j < resultSize.size(); j++) {
                if (resultSize.get(j) > thresholds[size]) {
                    count++;
                }
                avg+=resultSize.get(j);
            }
            System.out.println("Size " + (size) + "\tcount " + count + "\tavg " + avg/resultSize.size() +" thres " + thresholds[size] + "\tp-value " + (double) count / noRuns);
        }

//        int[] stat = new int[101];
//
//        for (int i = 0; i < result.size(); i++){
//            ArrayList<Double> resultSize = result.get(i);
//            System.out.println("Size " + i + ":");
//            Arrays.fill(stat, 0);
//            for (int j = 0; j < resultSize.size(); j++){
////                System.out.print("," + resultSize.get(j));
//                int potBucket = (int)(resultSize.get(j)*100);
//                stat[potBucket]++;
//            }
//            String strPot = "";
//            String strNo = "";
//            for (int j = 0; j < stat.length;j ++){
//                if (stat[j]!=0){
//                strPot += ","+Double.toString(j/100.0);
//                strNo += ","+Double.toString((double)stat[j]/resultSize.size());
//                }
//
//            }
//            System.out.println("Potential:\n" + strPot + "\nCount:\n" + strNo + "\n");
//        }
    }

    public static void readFile(String fileName) throws FileNotFoundException, IOException {
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String line;

        String lineSize = "";
        String linePotential = "";

        while (true) {
            line = in.readLine();

            if (line == null) {
                break;
            }            

            if (line.contains("potentialPerSize")) {
                // read visited subgraph size and avg potential per size
                for (int i = 0; i < 4; i++) {
                    line = in.readLine();
                }                
                lineSize = line.trim();
                in.readLine();
                line = in.readLine();
                linePotential = line.trim();

                // read data into results
                StringTokenizer tokenSize = new StringTokenizer(lineSize, ",");
                StringTokenizer tokenPotential = new StringTokenizer(linePotential, ",");
                while (tokenSize.hasMoreTokens()) {
                    int size = Integer.parseInt(tokenSize.nextToken());
                    double potential = Double.parseDouble(tokenPotential.nextToken());
                    result.get(size).add(potential);
                }
                noRuns++;
            }
        }
        in.close();
    }
}
