/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.answer;

import gclassifier.DecisionTreeController;
import gclassifier.Graph;
import gclassifier.Node;
import gclassifier.sampler.MH_Fast_DivRank;
import gclassifier.sampler.MetroSampler;
import gclassifier.sampler.WeightedSamplerMH;
import gclassifier.sampler.WeightedSamplerMHDA_Fast;
import gclassifier.sampler.WeightedSamplerMH_Fast;
import gclassifier.sampler.WeightedSamplerMH_SVM;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import libsvm.SVMHelper;
import libsvm.svm_node;
import libsvm.svm_problem;

/**
 *
 * @author minhhx
 */
public class CrossValidation {

    boolean[][] storeFile;
    Set<Integer> sampleIndex;
    public Graph ppiGraph;
    ArrayList<ArrayList<String>> sampleInfoStore;
    // fold for crossvalidation
    ArrayList<Set<Integer>> foldList;
    int noFold;

//    public CrossValidation(int _noFold, String fileProExp,
//            String fileSampleLabel, String filePPI,
//            String outputPrefix, int maxIter, int seedSize,
//            double threshold, int noTrees) throws FileNotFoundException{
    public CrossValidation(int _noFold, String fileProExp,
            String fileSampleLabel, String filePPI) throws FileNotFoundException {
        noFold = _noFold;
        loadSamples(fileProExp, fileSampleLabel, filePPI);
//        test(outputPrefix, maxIter, seedSize, threshold, noTrees);
    }

    public void clearHighPotentialNodes(Set<Integer> highNodes) {
        for (Integer nid : highNodes) {
            for (int i = 0; i < storeFile.length; i++) {
                storeFile[i][nid] = false;
            }
        }
    }

    public void test(String outputPrefix, int maxIter, int seedSize,
            double threshold, int noTrees) throws FileNotFoundException {
        if (noTrees == 0) {
            noTrees = Integer.MAX_VALUE;
        }
        for (int i = 0; i < noFold; i++) {
            System.out.println("################ Fold no " + i);
            Set<Integer> trainingSet = new HashSet<Integer>(sampleIndex);
            trainingSet.removeAll(foldList.get(i));

            // create the forest
            DecisionTreeController controller = new DecisionTreeController(trainingSet, ppiGraph, storeFile);
            MetroSampler sampler = new WeightedSamplerMH(controller);
            sampler.sample(maxIter, seedSize);
            sampler.printStatistics();
            sampler.chooseDiscriminatorySubgraph(threshold);
            OptimalForest forest = sampler.createForest(threshold, noTrees);

            // test using the forest created
            Set<Integer> testSet = foldList.get(i);
            String outputFile = outputPrefix + Integer.toString(i);
            PrintStream out = new PrintStream(new FileOutputStream(outputFile));

            for (Integer s : testSet) {
                ArrayList<String> sample = sampleInfoStore.get(s);
                List<String> features = sample.subList(1, sample.size());
                Integer classLabel = 0;
                if (storeFile[s][0]) {
                    classLabel = 1;
                }
                double prediction = forest.classify(features);
                out.println(classLabel + " " + prediction);
            }

            out.close();
        }
    }

    public void testWeighted(String prefix, int maxIter, int seedSize,
            double threshold, int noTrees) throws FileNotFoundException {
        if (noTrees == 0) {
            noTrees = Integer.MAX_VALUE;
        }
        double sumacc = 0.0;
//        double sumaccw = 0.0;
        double acc = 0.0;
//        double accw = 0.0;
        double prediction = 0.0;
        int realnoTrees = 0;
        for (int i = 0; i < noFold; i++) {
            System.out.println("################ Fold no " + i);
            Set<Integer> trainingSet = new HashSet<Integer>(sampleIndex);
            trainingSet.removeAll(foldList.get(i));

            // create the forest
            DecisionTreeController controller = new DecisionTreeController(trainingSet, ppiGraph, storeFile);
            MetroSampler sampler = new WeightedSamplerMH_Fast(controller);
            sampler.sample(maxIter, seedSize, threshold);

            OptimalForest forest = sampler.createForest(threshold, noTrees);
            realnoTrees += forest.forest.size();
            // test using the forest created
            Set<Integer> testSet = foldList.get(i);
//            String outputFile = prefix + Integer.toString(i);
//            PrintStream out = new PrintStream(new FileOutputStream(outputFile));

//            String outputFileWeighted = prefix + "weighted_" + Integer.toString(i);
//            PrintStream outWeighted = new PrintStream(new FileOutputStream(outputFileWeighted));

            acc = 0.0;
//            accw = 0.0;
            for (Integer s : testSet) {
                ArrayList<String> sample = sampleInfoStore.get(s);
                List<String> features = sample.subList(1, sample.size());
                Integer classLabel = 0;
                if (storeFile[s][0]) {
                    classLabel = 1;
                }
//                double prediction = forest.weightedClassify(features);
                prediction = forest.classify(features);
//                out.println(classLabel + " " + prediction);
                if (classLabel == 1 && prediction >= 0.5
                        || classLabel == 0 && prediction < 0.5) {
                    acc += 1;
                }

//                prediction = forest.weightedClassify(features);
//                outWeighted.println(classLabel + " " + prediction);
//                if (classLabel == 1 && prediction >= 0.5 ||
//                        classLabel == 0 && prediction < 0.5){
//                    accw += 1;
//                }
            }
            acc /= testSet.size();
            sumacc += acc;
//            accw /= testSet.size();
//            sumaccw += accw;

//            out.close();
//            outWeighted.close();
        }
        sumacc /= noFold;
//        sumaccw /= noFold;
        System.err.println("No Tree: " + (double) realnoTrees / noFold + " Accuracy: " + sumacc);
//        System.err.println("No Tree: " + (double)realnoTrees/noFold + " AccuracyW: " + sumaccw);
    }
    
    public void testNCDT(int maxIter, int seedSize,
            double threshold, int[] noModels) throws FileNotFoundException {
        double[] sumacc = new double[noModels.length];
        int[] realnoModels = new int[noModels.length];
        double prediction;
        int acc = 0;
        System.out.println("NoFolds: " + noFold);
        for (int i = 0; i < noFold; i++) {
            System.out.println("################ Fold no " + i);
            Set<Integer> trainingSet = new HashSet<Integer>(sampleIndex);
            trainingSet.removeAll(foldList.get(i));
            Set<Integer> testSet = foldList.get(i);
            
            // perform the sampling
            DecisionTreeController controller = new DecisionTreeController(trainingSet, ppiGraph, storeFile);
            //MetroSampler sampler = new WeightedSamplerMH_Fast(controller);
            MetroSampler sampler = new MH_Fast_DivRank(controller);
            sampler.doStat = false;
            sampler.sample(maxIter, seedSize, threshold);
            sampler.printStatistics();
            
            // create ensembler leaner
            for (int j = 0; j < noModels.length; j++){
                OptimalForest forest = sampler.createForest(threshold, noModels[j]);
                realnoModels[j] += forest.forest.size();
                // test using the forest created                
                acc = 0;
                for (Integer s : testSet) {
                    ArrayList<String> sample = sampleInfoStore.get(s);
                    List<String> features = sample.subList(1, sample.size());
                    Integer classLabel = 0;
                    if (storeFile[s][0]) {
                        classLabel = 1;
                    }
                    prediction = forest.classify(features);
                    if (classLabel == 1 && prediction >= 0.5
                            || classLabel == 0 && prediction < 0.5) {
                        acc++;
                    }
                }
                sumacc[j] += (double)acc/testSet.size();
                System.err.println("acc: " + (double)acc/testSet.size());
            }
        }
        for (int j = 0; j < noModels.length; j++){
            sumacc[j] /= noFold;
            System.err.println("NCDTForest No models: " + (double) realnoModels[j] / noFold + " Accuracy: " + sumacc[j]);    
        }
    }
    
    // get a subset of the svm_problem prob for the sub sample set
    public svm_problem extractSVMProblem(svm_problem org_prob, Set<Integer> sampleSet){
        svm_problem sub_prob;
        sub_prob = new svm_problem();
        sub_prob.l = sampleSet.size();
        int noFea = storeFile[0].length - 1;
        sub_prob.x = new svm_node[sub_prob.l][noFea];
        sub_prob.y = new double[sub_prob.l];
        
        int i = 0;
        for (Integer s : sampleSet){
            sub_prob.x[i] = org_prob.x[s];
            sub_prob.y[i] = org_prob.y[s];
            i++;
        }
        
        return sub_prob;
    }

    public void testSVM(int maxIter, int seedSize,
            double threshold, int[] noModels) throws FileNotFoundException {        
        
        // create the common SVM problem
        svm_problem all_prob = SVMHelper.createSVMProblem(storeFile);
        
        double[] sumacc = new double[noModels.length];
        int[] realnoModels = new int[noModels.length];
        double acc;
        System.out.println("NoFolds: " + noFold);
        for (int i = 0; i < noFold; i++) {
            System.out.println("################ Fold no " + i);
            Set<Integer> trainingSet = new HashSet<Integer>(sampleIndex);
            trainingSet.removeAll(foldList.get(i));
            Set<Integer> testSet = foldList.get(i);
            
            // create the forest
            DecisionTreeController controller = new DecisionTreeController(trainingSet, ppiGraph, storeFile);
            MetroSampler sampler = new WeightedSamplerMH_SVM(controller);
            sampler.sample(maxIter, seedSize, threshold);    
            
            for (int j = 0; j < noModels.length; j++){
                SVMEnsembler ensembler = sampler.createSVMEnsembler(threshold, noModels[j]);
                realnoModels[j] += ensembler.ensembler.size();
                // test using the forest created
                svm_problem sub_prob = extractSVMProblem(all_prob, testSet);
                acc = ensembler.classify(sub_prob);
                sumacc[j] += acc;
                System.err.println("acc: " + acc);
            }            
        }
        for (int j = 0; j < noModels.length; j++){
            sumacc[j] /= noFold;
            System.err.println("SVMEnsembler No models: " + (double) realnoModels[j] / noFold + " Accuracy: " + sumacc[j]);    
        }
        
    }

    public void testRF(String outputPrefix, int noTrees) throws FileNotFoundException {
        if (noTrees == 0) {
            noTrees = Integer.MAX_VALUE;
        }
        double sumacc = 0.0;
        double acc = 0.0;
        for (int i = 0; i < noFold; i++) {
            System.out.println("################ Fold no " + i);
            Set<Integer> trainingSet = new HashSet<Integer>(sampleIndex);
            trainingSet.removeAll(foldList.get(i));
            Set<Integer> testSet = foldList.get(i);
            
            // create the forest
            OptimalForest forest = new OptimalForest(storeFile, trainingSet, ppiGraph, noTrees);

            // test using the forest created            
            String outputFile = outputPrefix + Integer.toString(i);
            PrintStream out = new PrintStream(new FileOutputStream(outputFile));

            acc = 0.0;
            for (Integer s : testSet) {
                ArrayList<String> sample = sampleInfoStore.get(s);
                List<String> features = sample.subList(1, sample.size());
                Integer classLabel = 0;
                if (storeFile[s][0]) {
                    classLabel = 1;
                }
                double prediction = forest.classify(features);
                out.println(classLabel + " " + prediction);
                if (classLabel == 1 && prediction >= 0.5
                        || classLabel == 0 && prediction < 0.5) {
                    acc += 1;
                }
            }
            acc /= testSet.size();
            sumacc += acc;
            out.close();
        }
        sumacc /= noFold;
        System.err.println("RF NoTrees: " + noTrees + " Accuracy: " + sumacc);
    }
    
    public void testNGF(int[] noModels) throws FileNotFoundException {
        double[] sumacc = new double[noModels.length];
        int acc;
        double prediction;
        System.out.println("NoFolds: " + noFold);
        for (int i = 0; i < noFold; i++) {
            System.out.println("################ Fold no " + i);
            Set<Integer> trainingSet = new HashSet<Integer>(sampleIndex);
            trainingSet.removeAll(foldList.get(i));
            Set<Integer> testSet = foldList.get(i);

            for (int j = 0; j < noModels.length; j++){
                // create the forest
                OptimalForest forest = new OptimalForest(storeFile, trainingSet, ppiGraph, noModels[j]);

                // test using the forest created
                acc = 0;
                for (Integer s : testSet) {
                    ArrayList<String> sample = sampleInfoStore.get(s);
                    List<String> features = sample.subList(1, sample.size());
                    Integer classLabel = 0;
                    if (storeFile[s][0]) {
                        classLabel = 1;
                    }
                    prediction = forest.classify(features);
                    if (classLabel == 1 && prediction >= 0.5
                            || classLabel == 0 && prediction < 0.5) {
                        acc += 1;
                    }
                }
                sumacc[j] += (double)acc/testSet.size();
                System.err.println("acc: " + (double)acc/testSet.size());
            }            
        }
        for (int j = 0; j < noModels.length; j++){
            sumacc[j] /= noFold;
            System.err.println("NGFForest No models: " + noModels[j] + " Accuracy: " + sumacc[j]);    
        }
    }
    
    // find the most visited nodes & stat gene pair for NGF, to compare with those from MH
    public void statNGF(int noTrees, int noNodes){
        OptimalForest.statNGF(storeFile, sampleIndex, ppiGraph, noTrees, noNodes);       
    }

    // load data from files
    private void loadSamples(String fileProExp, String fileSampleLabel, String filePPI) {
        Boolean b1 = true;
        int linenum = 0; //to keep track of the number of rows in the file (which is equal to the number of samples)

        // map protein name to an index
        HashMap<String, Integer> protidMap = new HashMap();

        //to store class value true or false for every sampl
        HashMap<String, Boolean> SampleBinary = new HashMap();

        //this reads the entire file nto an arraylist of arraylists, this is used just once in th beginning
        sampleInfoStore = new ArrayList<ArrayList<String>>();
        sampleIndex = new HashSet<Integer>();

        try {
            BufferedReader br1 = new BufferedReader(new FileReader(fileProExp));
            int count = 1;
            while (br1.ready()) {
                String[] X = br1.readLine().split("\t");
                ArrayList<String> sampleInfoLine = new ArrayList();

                for (int i = 0; i < X.length - 1; i++) {
//                for (int i = 0; i < maxNoProtein; i++) {
                    sampleInfoLine.add(X[i].trim());
                    if (!protidMap.containsKey(X[i + 1].trim())) {
                        protidMap.put(X[i + 1].trim(), count++);
                    }
                }
                sampleInfoLine.add(X[X.length - 1]);  //adding the last protein
                linenum++;
                sampleInfoStore.add(sampleInfoLine);
            }
        } catch (Exception e) {
            System.out.println("Exeption: " + e);
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileSampleLabel));
            while (br.ready()) {
                String X = br.readLine();
                String[] Y = X.split("\t");
                if (Y[1].trim().equals("0")) {
                    b1 = false;
                } else if (Y[1].trim().equals("1")) {
                    b1 = true;
                }
                SampleBinary.put(Y[0].trim(), b1);

            }
        } catch (Exception e) {
            System.out.println(e);
        }


        storeFile = new boolean[linenum][protidMap.size() + 1];


        for (int i = 0; i < linenum; i++) {
            //initializing all protein expression values to false
            Arrays.fill(storeFile[i], false);
        }

        int countTrue = 0;
        //filling up storeFile:
        for (int i = 0; i < linenum; i++) {
            String Samp = sampleInfoStore.get(i).get(0); //getting sample name
            storeFile[i][0] = SampleBinary.get(Samp);  //the class labels
            if (storeFile[i][0]) {
                countTrue++;
            }

            for (int j = 0; j < sampleInfoStore.get(i).size() - 1; j++) {
                String prot = sampleInfoStore.get(i).get(j + 1);  //getting the 1 vlue proteins for this sample
                // System.out.println(String.valueOf(protid.get(prot)));
                Integer k = protidMap.get(prot);  //getting the index of this protein
                if (k != null) {
                    storeFile[i][k] = true;
                }
            }
        }

        // test for recomb, remove nodes with potential > 0.7

        // end test for recomb


        System.out.println("Num Proteins " + protidMap.size()
                +". Num events: " + storeFile.length);

        for (int i = 0; i < sampleInfoStore.size(); i++) {
            sampleIndex.add(i);
        }

        //building the PPI network
        ppiGraph = new Graph();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePPI));

            while (br.ready()) {
                String X = br.readLine();
                String[] Y = X.split("\t");

                String A = Y[0].trim();
                String B = Y[1].trim();

                ppiGraph.addEdge(A, B, protidMap);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("No nodes in Pruned Graph: " + ppiGraph.nodes.size());

        // create folds
        int countFalse = storeFile.length - countTrue;
        int posPerFold = countTrue / noFold;
        int negPerFold = countFalse / noFold;

        ArrayList<Set<Integer>> posFoldList = new ArrayList<Set<Integer>>();
        ArrayList<Set<Integer>> negFoldList = new ArrayList<Set<Integer>>();
        foldList = new ArrayList<Set<Integer>>();
        Set<Integer> curPosFold = new HashSet<Integer>();
        Set<Integer> curNegFold = new HashSet<Integer>();

        for (int i = 0; i < storeFile.length; i++) {
            if (storeFile[i][0]) {
                curPosFold.add(i);
                if (curPosFold.size() == posPerFold) {
                    posFoldList.add(curPosFold);
                    curPosFold = new HashSet<Integer>();
                }
            } else {
                curNegFold.add(i);
                if (curNegFold.size() == negPerFold) {
                    negFoldList.add(curNegFold);
                    curNegFold = new HashSet<Integer>();
                }
            }
        }

        for (int i = 0; i < noFold; i++) {
            Set<Integer> aFold = posFoldList.get(i);
            if (!negFoldList.isEmpty()) {
                aFold.addAll(negFoldList.get(i));
            } else {
                System.out.println("No Neg Samples!!!!!");
            }
            foldList.add(aFold);
        }
    }
}
