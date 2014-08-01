/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tester;

import gclassifier.DecisionTreeController;
import gclassifier.sampler.MetroSampler;
import gclassifier.sampler.WeightedSamplerMH_Fast;

/**
 *
 * @author minhhx
 */
public class Nazli {
    public static void main(String[] args) {
        if (args.length < 1) {            
            System.out.println("Wrong number of input.");
            System.out.println("Sampling: 0 <node_label_file> <class_label_file> <network_file> <threshold> <seedSize> <maxiter>");
            System.out.println("Sampling PermuteGlobalState: 1 <node_label_file> <network_file> <maxiter>");
            System.out.println("Sampling PermuteExpressionLevel: 2 <node_label_file> <class_label_file> <network_file> <maxiter>");
            System.out.println("Sampling Permute edges 3: <node_label_file> <class_label_file> <network_file> <maxiter> <noise>");
            return;
        }
        int option = Integer.parseInt(args[0]);
        switch (option) {
            case 0:
                sampleFast(args);
                break;            
            case 1:
                permuteGlobalState(args);
                break;            
            case 2:
                permuteExpressionLevel(args);
                break;            
            case 3:
                permuteEdges(args);
                break;
        }
    }
    public static void sampleFast(String[] args) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int seedSize = 0;
        int maxIter = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        threshold = Double.parseDouble(args[4]);
        seedSize = Integer.parseInt(args[5]);
        maxIter = Integer.parseInt(args[6]);

        
        System.out.println("Input files:\n\t" + fileProteinExp + "\n\t" + fileSampletoBinary + "\n\t" + filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, true, 0);

        // sampling
        MetroSampler sampler = null;
        sampler = new WeightedSamplerMH_Fast(controller);

        time = System.currentTimeMillis();
        sampler.sample(maxIter, seedSize);  // sampling
        System.out.println("Sampling ... " + (System.currentTimeMillis() - time));
        
        sampler.visitCountStat();
        System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));
        
        // print results
        sampler.printChosenSubgraph(threshold); 
        
        // print chosen trees
        sampler.printChosenTrees(threshold);
    }
    
    public static void permuteGlobalState(String args[]) {
        // <proteinExp> <ppi> <threshold> <maxiter>
        String fileProteinExp = "";
        String filePPI = "";
        double threshold = 0.0;
        int maxIter = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        System.out.println("Sample with random labels...");
        fileProteinExp = args[1];
        filePPI = args[2];
        maxIter = Integer.parseInt(args[3]);
        System.out.println("Input files:\n\t" + fileProteinExp + "\n\t" + filePPI);

        for (int i = 0; i < 20; i++) {   // run for 20 times
            System.out.println("----------------------\nRun time: " + i);
            System.err.println("----------------------\nRun time: " + i);
            controller = new DecisionTreeController(fileProteinExp, filePPI);
            // MetroSampler sampler = new WeightedSamplerMH(controller);
            MetroSampler sampler = new WeightedSamplerMH_Fast(controller);

            sampler.sample(maxIter, 10);
            System.out.println("Sampling ... " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            sampler.visitCountStat();
            System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));

            sampler.chooseDiscriminatorySubgraph(threshold);
        }
    }

    public static void permuteExpressionLevel(String args[]) {
        // <proteinExp> <ppi> <threshold> <maxiter>
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int maxIter = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        System.out.println("Sample with random expression levels...");
        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        maxIter = Integer.parseInt(args[4]);
        //printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        for (int i = 0; i < 20; i++) {   // run for 20 times
            System.out.println("----------------------\nRun time: " + i);
            System.err.println("----------------------\nRun time: " + i);
            controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI);
            // MetroSampler sampler = new WeightedSamplerMH(controller);
            MetroSampler sampler = new WeightedSamplerMH_Fast(controller);

            sampler.sample(maxIter, 10);
            System.out.println("Sampling ... " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            sampler.visitCountStat();
            System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));

            sampler.chooseDiscriminatorySubgraph(threshold);
        }
    }
    
    public static void permuteEdges(String args[]) {
        // <proteinExp> <ppi> <threshold> <maxiter>
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int maxIter = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        System.out.println("Sample with random labels...");
        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        maxIter = Integer.parseInt(args[4]);
        double noise = new Double(args[5]);
        //printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        for (int i = 0; i < 20; i++) {   // run for 20 times
            System.out.println("--------------------\nRun time: " + i);
            System.err.println("--------------------\nRun time: " + i);
            controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, noise);

            MetroSampler sampler = new WeightedSamplerMH_Fast(controller);
            //MetroSampler sampler = new WeightedSamplerMH(controller);

            sampler.sample(maxIter, 1);
            System.out.println("Sampling ... " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            sampler.visitCountStat();
            System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));

            sampler.chooseDiscriminatorySubgraph(threshold);
        }
    }
}
