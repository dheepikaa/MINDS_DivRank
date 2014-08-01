/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.test;

import gclassifier.DecisionTreeController;
import gclassifier.Graph;
import gclassifier.Tree;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

/**
 *
 * @author minhhx
 */
public class TestSVM {
    static boolean[][] storeFile;
    static Set<Integer> sampleIndex;
    static public Graph ppiGraph;
    static ArrayList<ArrayList<String>> sampleInfoStore;
    
    public static void main(String[] args) throws IOException {
        String fileProExp = args[0];
        String fileSampleLabel = args[1];
        String filePPI = args[2];
        loadSamples(fileProExp, fileSampleLabel, filePPI);
        
        long sumTime = 0;
        for (int i = 0; i < 10; i++){
            long time = System.currentTimeMillis();
            testTimeSVM();
            //testTimeNCDT();
            sumTime += System.currentTimeMillis() - time;            
        }
        sumTime /= 10.0;
        System.out.println("Average time: " + sumTime);
    }
    
    private static void testTimeNCDT(){
        Tree currentTree = new Tree(storeFile, sampleIndex, ppiGraph.nodes);   
    }
    
    private static void testTimeSVM(){
        svm_problem prob = new svm_problem();
        prob.l = storeFile.length;
        int noFea = storeFile[0].length-1;
        prob.x = new svm_node[prob.l][noFea];
        prob.y = new double[prob.l];
        for (int i = 0; i <prob.l; i++){
            svm_node[] nodeList = prob.x[i];
            for (int j = 1; j <= noFea; j++){
                svm_node aNode = new svm_node();
                aNode.index = j-1;
                aNode.value = storeFile[i][j]?1:0;
                nodeList[j-1] = aNode;
            }
            prob.y[i] = storeFile[i][0]?1:-1;
        }
        
        // set param
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.degree = 3;
        param.gamma = 0;	// 1/num_features
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 100;
        param.C = 1;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 0;
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];

        svm_model model = svm.svm_train(prob, param);
        //System.println()
        
//        svm.svm_save_model("abc.model", model);
        
        double[] dim=new double[noFea];
        for (int i = 0; i < model.SV.length; i++){
            for (int j = 0; j < noFea; j++){
                dim[j] += model.SV[i][j].value * model.sv_coef[0][i];                
            }
        }
        for (int j = 0; j < noFea; j++){
            System.out.println(dim[j]);
        }
    }
    
    private static void loadSamples(String fileProExp, String fileSampleLabel, String filePPI) {
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


        System.out.println("number of proteins to work with " + protidMap.size());

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

        
    }
}
