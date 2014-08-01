/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.answer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author minhhx
 */
public class CrossValidation_Unused {

    HashMap<String, Set<String>> featureListMap; // map gene name to featureList
    HashMap<String, Integer> classMap; // map gene name to class label

    public CrossValidation_Unused(OptimalForest forest, int noFold, String fileProExp, 
            String fileSampleLabel, String outputPrefix) throws FileNotFoundException{
        loadSamples(fileProExp, fileSampleLabel);
//        test(forest, noFold, outputPrefix);
    }

//    private void test(OptimalForest forest, int noFold, String outputPrefix) throws FileNotFoundException {
//        Set<Set<String>> setFold = createFold(noFold);
//        int count = 1;
//        for (Set<String> aFold : setFold) {
//            String outputFile = outputPrefix + Integer.toString(count++);
//            PrintStream out = new PrintStream(new FileOutputStream(outputFile));
//
//            for (String sample : aFold){
//                Set<String> features = featureListMap.get(sample);
//                Integer classLabel = classMap.get(sample);
//                if (features == null || classLabel == null)
//                    continue;
//                double prediction = forest.classify(features);
//                out.println(classLabel + " " + prediction);
//            }
//            out.close();
//        }
//    }

    // divide test data into k fold
    private Set<Set<String>> createFold(int noFold) {
        int noSamplerPerFold = featureListMap.size() / noFold;
        int count = 0;
        Set<Set<String>> setFold = new HashSet<Set<String>>();
        Set<String> aFold = new HashSet<String>();
        for (String s : featureListMap.keySet()) {
            if (count == noSamplerPerFold) {
                setFold.add(aFold);
                aFold = new HashSet<String>();
                count = 0;
            }
            aFold.add(s);
            count++;
        }
        return setFold;
    }

    
    // load data from files
    private void loadSamples(String fileProExp, String fileSampleLabel) {
        featureListMap = new HashMap<String, Set<String>>();
        classMap = new HashMap<String, Integer>();

        // load feature list
        try {
            BufferedReader br1 = new BufferedReader(new FileReader(fileProExp));
            while (br1.ready()) {
                String[] X = br1.readLine().split("\t");
                Set<String> featureList = new HashSet<String>();

                for (int i = 1; i < X.length; i++) {
                    featureList.add(X[i].trim());
                }
                featureListMap.put(X[0], featureList);
            }
        } catch (Exception e) {
            System.out.println("Exeption: " + e);
        }

        // load sample class
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileSampleLabel));
            while (br.ready()) {
                String X = br.readLine();                
                String[] Y = X.split("\t");
                int classLabel = Integer.parseInt(Y[1]);
                classMap.put(Y[0].trim(), classLabel);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Finish Loading test data");
    }
}
