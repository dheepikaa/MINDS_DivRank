package gclassifier;

import gclassifier.answer.CrossValidation;
import gclassifier.sampler.MetroSampler;
import gclassifier.sampler.RandomSampler;
import gclassifier.sampler.UniformSampler;
import gclassifier.sampler.WeightedSamplerNoQ;
import gclassifier.sampler.WeightedSamplerMH;
import gclassifier.sampler.WeightedSamplerMHDA_Fast;
import gclassifier.sampler.WeightedSamplerMHDA_NewForm;
import gclassifier.sampler.WeightedSamplerMH_NewForm;
import gclassifier.sampler.WeightedSamplerMH_Pilot;
import gclassifier.sampler.WeightedSamplerMH_Fast;
import gclassifier.sampler.WeightedSamplerMH_SVM;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author minhhx
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        if (args.length < 1) {
            instruction();
            return;
        }
        int option = Integer.parseInt(args[0]);
        switch (option) {
            case 0:
                sample(args);
                break;
            case 1:
                randomSample(args);
                break;
            case 2:
                enumberate(args);
                break;
            case 3:
                testAccMH(args);
                break;
            case 4:
                statInfoGain(args);
                break;
            case 5:
                testAccNGF(args);
                break;
            case 6:
                permuteGlobalState(args);
                break;
            case 7:
                statPair(args);
                break;
            case 8:
                findATree(args);
                break;
            case 9:
                permuteExpressionLevel(args);
                break;
            case 10:
                convertFormat(args);
                break;
            case 11:
                createFoldSVM(args);
                break;
            case 12:
                sampleNewFormula(args);
                break;
            case 13:
                sampleWithPilot(args);
                break;
            case 14:
                sampleSVM(args);
                break;
            case 15:
                sampleMHDA(args);
                break;
            case 16:
                sampleFast(args);
                break;
            case 17:
                sampleFastMHDA(args);
                break;
            case 18:
                permuteEdges(args);
                break;
            case 19:
                testAccSVM(args);
                break;
            case 20:
                statNGF(args);
                break;
            default:
                instruction();
                break;
        }

//        MetroSampler sampler = new UniformSampler(c);
//        sampler.sample(10000, 5);
//        sampler.visitCountStat();
//        sampler.chooseDiscriminatorySubgraph(0.2);
//        c.startEnumSubgraph(0.2);
    }

    public static void instruction() {
        System.out.println("Wrong number of input.");
        System.out.println("Sampling: 0 <proteinExp> <sampletobinany> <ppi> <threshold> <maxNoProtein> <seedSize> <maxiter> <samplerType>");
        System.out.println("Random: 1 <proteinExp> <sampletobinany> <ppi> <seedSize> <maxiter>");
        System.out.println("Enumeration: 2 <proteinExp> <sampletobinany> <ppi> <threshold> <maxNoProtein> <maxSize>");
        System.out.println("XVali MH: 3 <proteinExp> <sampletobinany> <ppi> <threshold> <seedSize> <maxiter> <noFold> <noTreesLListComma> <removeHighNodes(1/0)>");
        System.out.println("Stat InfoGain: 4 <proteinExp> <sampletobinany> <ppi>");
        System.out.println("XVali NGF: 5 <proteinExp> <sampletobinany> <ppi> <noFold> <noTreesLListComma> <removeHighNodes(1/0)>");
        System.out.println("Sampling PermuteGlobalState: 6 <proteinExp> <ppi> <maxiter>");
        System.out.println("Stat Gene Pair: 7 <proteinExp> <sampletobinany> <ppi> <threshold> <seedSize> <maxiter> <samplerType> <noTree>");
        System.out.println("Find a Tree: 8 <proteinExp> <sampletobinany> <ppi> <threshold> <seedSize> <maxiter> <samplerType> <breast1-brain2>");
        System.out.println("Sampling PermuteExpressionLevel: 9 <proteinExp> <sampletobinany> <ppi> <maxiter>");
        System.out.println("ConvertFormat: 10 <proteinExp> <sampletobinany> <ppi> <1svm-2csv>");
        System.out.println("CreateFold: 11 orgDataFile noOfFold");
        System.out.println("Sampling New Formula: 12 <proteinExp> <sampletobinany> <ppi> <threshold> <maxNoProtein> <seedSize> <maxiter>");
        System.out.println("Sampling With Pilot: 13 <proteinExp> <sampletobinany> <ppi> <threshold> <maxNoProtein> <seedSize> <maxiter>");
        System.out.println("Sampling With SVM: 14 <proteinExp> <sampletobinany> <ppi> <threshold> <maxNoProtein> <seedSize> <maxiter> <doStat:0/1>");
        System.out.println("Sampling MHDA New Formula: 15 <proteinExp> <sampletobinany> <ppi> <threshold> <maxNoProtein> <seedSize> <maxiter>");
        System.out.println("Sampling Fast (Latest Formula): 16 <proteinExp> <sampletobinany> <ppi> <threshold> <maxNoProtein> <seedSize> <maxiter> <doStat:0/1>");
        System.out.println("Sampling Fast MHDA: 17 <proteinExp> <sampletobinany> <ppi> <threshold> <maxNoProtein> <seedSize> <maxiter>");
        System.out.println("Sampling Permute edges 18: <proteinExp> <ppi> <maxiter>");
        System.out.println("XVali SVMEnsembler 19: <proteinExp> <sampletobinany> <ppi> <threshold> <seedSize> <maxiter> <noFold> <noSVMsLListComma> <removeHighNodes(1/0)>");
        System.out.println("statNGF 20: <proteinExp> <sampletobinany> <ppi> <noTrees> <noNodes>");
        System.out.println("Sampler Type: 1) No Qij 2) With Qij");
    }

    public static void printInputFile(String fileProteinExp, String fileSampletoBinary, String filePPI) {
        System.out.println("Input files:\n\t" + fileProteinExp + "\n\t" + fileSampletoBinary + "\n\t" + filePPI);
    }

    public static void sample(String[] args) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int seedSize = 0;
        int maxIter = 0;
        int samplerType = 0;
        int maxNoProtein = 0;


        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        threshold = Double.parseDouble(args[4]);
        maxNoProtein = Integer.parseInt(args[5]);

        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, false, maxNoProtein);

        // sampling
        seedSize = Integer.parseInt(args[6]);
        maxIter = Integer.parseInt(args[7]);
        samplerType = Integer.parseInt(args[8]);

        if (samplerType != 1 && samplerType != 2) {
            System.out.println("Wrong Sampler Type!");
            System.out.println("Sampler Type: 1) No Qij 2) With Qij");
            return;
        }

        MetroSampler sampler = null;
        if (samplerType == 1) {
            sampler = new WeightedSamplerNoQ(controller);
        } else {
            sampler = new WeightedSamplerMH(controller);
        }

        time = System.currentTimeMillis();
        sampler.sample(maxIter, seedSize, threshold);
        System.out.println("Sampling ... " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        sampler.visitCountStat();
        System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        sampler.chooseDiscriminatorySubgraph(threshold);
//        System.out.println("Choosing subgraph ... " + (System.currentTimeMillis() - time));
//        int chosenSize[] = {4, 7, 9, 12, 14, 16};
//        for (int i = 0; i < chosenSize.length; i++) {
//            System.out.println("Print chosen tree size " + chosenSize[i] + " : ");
//            sampler.printSomeTree(chosenSize[i]);
//        }
    }

    public static void sampleNewFormula(String[] args) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int seedSize = 0;
        int maxIter = 0;
        int maxNoProtein = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        threshold = Double.parseDouble(args[4]);
        maxNoProtein = Integer.parseInt(args[5]);

        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, false, maxNoProtein);

        // sampling
        seedSize = Integer.parseInt(args[6]);
        maxIter = Integer.parseInt(args[7]);

        MetroSampler sampler = null;
        sampler = new WeightedSamplerMH_NewForm(controller);

        time = System.currentTimeMillis();
        sampler.sample(maxIter, seedSize, threshold);
        System.out.println("Sampling ... " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        sampler.visitCountStat();
        System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        sampler.chooseDiscriminatorySubgraph(threshold);
//        System.out.println("Choosing subgraph ... " + (System.currentTimeMillis() - time));
//        int chosenSize[] = {4, 7, 9, 12, 14, 16};
//        for (int i = 0; i < chosenSize.length; i++) {
//            System.out.println("Print chosen tree size " + chosenSize[i] + " : ");
//            sampler.printSomeTree(chosenSize[i]);
//        }
    }

    public static void sampleFast(String[] args) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int seedSize = 0;
        int maxIter = 0;
        int maxNoProtein = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        threshold = Double.parseDouble(args[4]);
        maxNoProtein = Integer.parseInt(args[5]);

        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, true, maxNoProtein);

        // sampling
        seedSize = Integer.parseInt(args[6]);
        maxIter = Integer.parseInt(args[7]);
        int doStat = Integer.parseInt(args[8]);

        MetroSampler sampler = null;
        sampler = new WeightedSamplerMH_Fast(controller);

        time = System.currentTimeMillis();
        if (doStat == 1){
            sampler.sample(maxIter, seedSize);
            System.out.println("Sampling ... " + (System.currentTimeMillis() - time));

            time = System.currentTimeMillis();
            sampler.visitCountStat();
            System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();

            sampler.chooseDiscriminatorySubgraph(threshold);
        } else {
            sampler.sample(maxIter, seedSize, threshold);
            System.out.println("Sampling ... " + (System.currentTimeMillis() - time));
        }
//        System.out.println("Choosing subgraph ... " + (System.currentTimeMillis() - time));
//        int chosenSize[] = {4, 7, 9, 12, 14, 16};
//        for (int i = 0; i < chosenSize.length; i++) {
//            System.out.println("Print chosen tree size " + chosenSize[i] + " : ");
//            sampler.printSomeTree(chosenSize[i]);
//        }
        // find best Nodes
        sampler.findMostVisitedNodes(100);
    }

    public static void sampleFastMHDA(String[] args) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int seedSize = 0;
        int maxIter = 0;
        int maxNoProtein = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        threshold = Double.parseDouble(args[4]);
        maxNoProtein = Integer.parseInt(args[5]);

        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, false, maxNoProtein);

        // sampling
        seedSize = Integer.parseInt(args[6]);
        maxIter = Integer.parseInt(args[7]);

        MetroSampler sampler = null;
        sampler = new WeightedSamplerMHDA_Fast(controller);

        time = System.currentTimeMillis();
        sampler.sample(maxIter, seedSize, threshold);
        System.out.println("Sampling ... " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        sampler.visitCountStat();
        System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        sampler.chooseDiscriminatorySubgraph(threshold);
    }

    public static void sampleMHDA(String[] args) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int seedSize = 0;
        int maxIter = 0;
        int maxNoProtein = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        threshold = Double.parseDouble(args[4]);
        maxNoProtein = Integer.parseInt(args[5]);

        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, false, maxNoProtein);

        // sampling
        seedSize = Integer.parseInt(args[6]);
        maxIter = Integer.parseInt(args[7]);

        MetroSampler sampler = null;
        sampler = new WeightedSamplerMHDA_NewForm(controller);

        time = System.currentTimeMillis();
        sampler.sample(maxIter, seedSize, threshold);
        System.out.println("Sampling ... " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        sampler.visitCountStat();
        System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        sampler.chooseDiscriminatorySubgraph(threshold);
//        System.out.println("Choosing subgraph ... " + (System.currentTimeMillis() - time));
//        int chosenSize[] = {4, 7, 9, 12, 14, 16};
//        for (int i = 0; i < chosenSize.length; i++) {
//            System.out.println("Print chosen tree size " + chosenSize[i] + " : ");
//            sampler.printSomeTree(chosenSize[i]);
//        }
    }

    public static void sampleWithPilot(String[] args) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int seedSize = 0;
        int maxIter = 0;
        int maxNoProtein = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        threshold = Double.parseDouble(args[4]);
        maxNoProtein = Integer.parseInt(args[5]);

        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, false, maxNoProtein);

        // sampling
        seedSize = Integer.parseInt(args[6]);
        maxIter = Integer.parseInt(args[7]);

        MetroSampler sampler = null;
        sampler = new WeightedSamplerMH_Pilot(controller);

        time = System.currentTimeMillis();
        sampler.sample(maxIter, seedSize, threshold);
        System.out.println("Sampling ... " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        sampler.visitCountStat();
        System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        sampler.chooseDiscriminatorySubgraph(threshold);
//        System.out.println("Choosing subgraph ... " + (System.currentTimeMillis() - time));
//        int chosenSize[] = {4, 7, 9, 12, 14, 16};
//        for (int i = 0; i < chosenSize.length; i++) {
//            System.out.println("Print chosen tree size " + chosenSize[i] + " : ");
//            sampler.printSomeTree(chosenSize[i]);
//        }
    }

    public static void sampleSVM(String[] args) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int seedSize = 0;
        int maxIter = 0;
        int maxNoProtein = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        threshold = Double.parseDouble(args[4]);
        maxNoProtein = Integer.parseInt(args[5]);

        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, true, maxNoProtein);

        // sampling
        seedSize = Integer.parseInt(args[6]);
        maxIter = Integer.parseInt(args[7]);
        int doStat = Integer.parseInt(args[8]);
        MetroSampler sampler = null;
        sampler = new WeightedSamplerMH_SVM(controller);

        time = System.currentTimeMillis();
        if (doStat == 1){
            sampler.sample(maxIter, seedSize);
            System.out.println("Sampling ... " + (System.currentTimeMillis() - time));

            time = System.currentTimeMillis();
            sampler.visitCountStat();
            System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();

            sampler.chooseDiscriminatorySubgraph(threshold);
        } else {
            sampler.sample(maxIter, seedSize, threshold);
            System.out.println("Sampling ... " + (System.currentTimeMillis() - time));
        }
//        System.out.println("Choosing subgraph ... " + (System.currentTimeMillis() - time));
//        int chosenSize[] = {4, 7, 9, 12, 14, 16};
//        for (int i = 0; i < chosenSize.length; i++) {
//            System.out.println("Print chosen tree size " + chosenSize[i] + " : ");
//            sampler.printSomeTree(chosenSize[i]);
//        }
    }

    public static void randomSample(String args[]) {
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

        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, false, 0);
        // sampling
        seedSize = Integer.parseInt(args[4]);
        maxIter = Integer.parseInt(args[5]);
        MetroSampler sampler = new RandomSampler(controller);
        sampler.sample(maxIter, seedSize);
        System.out.println("Randomize ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        sampler.visitCountStat();
        System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));

        sampler.chooseDiscriminatorySubgraph(threshold);

    }

    public static void enumberate(String args[]) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int maxNoProtein = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];

        threshold = Double.parseDouble(args[4]);
        maxNoProtein = Integer.parseInt(args[5]);
        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, true, maxNoProtein);

        int maxSize = Integer.parseInt(args[6]);
        controller.startEnumSubgraph(threshold, maxSize);
        System.out.println("Enumerating ... " + (System.currentTimeMillis() - time));

    }

    public static void testAccMH(String args[]) throws FileNotFoundException {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int seedSize = 0;
        int maxIter = 0;

        long time = System.currentTimeMillis();
        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];

        threshold = Double.parseDouble(args[4]);
        seedSize = Integer.parseInt(args[5]);
        maxIter = Integer.parseInt(args[6]);
        int noFold = Integer.parseInt(args[7]);
        StringTokenizer token = new StringTokenizer(args[8], ",");
        int[] noTrees = new int[token.countTokens()];
        int i = 0;
        while (token.hasMoreTokens()) {
            noTrees[i] = Integer.parseInt(token.nextToken());
            i++;
        }
        int removeHighNodes = Integer.parseInt(args[9]);

        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        CrossValidation validator = new CrossValidation(noFold, fileProteinExp,
                fileSampletoBinary, filePPI);

        // for recomb
        if (removeHighNodes == 1) {
            validator.clearHighPotentialNodes(getRemovedNodes(fileProteinExp, fileSampletoBinary, filePPI));
        }
        // end for recomd
        
        validator.testNCDT(maxIter, seedSize, threshold, noTrees);
    }

    public static void testAccSVM(String args[]) throws FileNotFoundException {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int seedSize = 0;
        int maxIter = 0;

        long time = System.currentTimeMillis();

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];

        threshold = Double.parseDouble(args[4]);
        seedSize = Integer.parseInt(args[5]);
        maxIter = Integer.parseInt(args[6]);
        int noFold = Integer.parseInt(args[7]);
        StringTokenizer token = new StringTokenizer(args[8], ",");
        int[] noSVMs = new int[token.countTokens()];
        int i = 0;
        while (token.hasMoreTokens()) {
            noSVMs[i] = Integer.parseInt(token.nextToken());
            i++;
        }

        int removeHighNodes = Integer.parseInt(args[9]);
        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        CrossValidation validator = new CrossValidation(noFold, fileProteinExp,
                fileSampletoBinary, filePPI);

        // for recomb
        if (removeHighNodes == 1) {
            validator.clearHighPotentialNodes(getRemovedNodes(fileProteinExp, fileSampletoBinary, filePPI));
        }
        // end for recomd

        validator.testSVM(maxIter, seedSize, threshold, noSVMs);
    }

    public static void statInfoGain(String args[]) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];

        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, true, 0);
        controller.statInfoGain();
    }

    public static Set<Integer> getRemovedNodes(String fileProteinExp, String fileSampletoBinary, String filePPI) {
        DecisionTreeController controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, true, 0);
        return controller.getHighPotentialNodes(0.7);
    }

    public static void testAccNGF(String args[]) throws FileNotFoundException {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];

        int noFold = Integer.parseInt(args[4]);
        StringTokenizer token = new StringTokenizer(args[5], ",");
        int[] noTrees = new int[token.countTokens()];
        int i = 0;
        while (token.hasMoreTokens()) {
            noTrees[i] = Integer.parseInt(token.nextToken());
            i++;
        }
        int removeHighNodes = Integer.parseInt(args[6]);
        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        CrossValidation validator = new CrossValidation(noFold, fileProteinExp,
                fileSampletoBinary, filePPI);

        // for recomb
        if (removeHighNodes == 1) {
            validator.clearHighPotentialNodes(getRemovedNodes(fileProteinExp, fileSampletoBinary, filePPI));
        }
        // end for recomd

        validator.testNGF(noTrees);
    }

    // stat the genepair and most visited Nodes for NGF
    public static void statNGF(String args[]) throws FileNotFoundException {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";


        long time = System.currentTimeMillis();

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];

        int noTrees = Integer.parseInt(args[4]);
        int noNode = Integer.parseInt(args[5]);
        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        CrossValidation validator = new CrossValidation(2, fileProteinExp,
                fileSampletoBinary, filePPI);

        validator.statNGF(noTrees, noNode);
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
        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        for (int i = 0; i < 10; i++) {   // run for 20 times
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
        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
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

    public static void genePairStat(String args[]) throws FileNotFoundException {
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

        String outputPrefix = args[4];
        threshold = Double.parseDouble(args[5]);
        seedSize = Integer.parseInt(args[6]);
        maxIter = Integer.parseInt(args[7]);
        int noFold = Integer.parseInt(args[8]);
        int noTrees = Integer.parseInt(args[9]);
        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        CrossValidation validator = new CrossValidation(noFold, fileProteinExp,
                fileSampletoBinary, filePPI);
        validator.testWeighted(outputPrefix, maxIter, seedSize, threshold, noTrees);
    }

    public static void statPair(String[] args) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int seedSize = 0;
        int maxIter = 0;
        int noTrees = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        threshold = Double.parseDouble(args[4]);

        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, false, 0);

        // sampling
        seedSize = Integer.parseInt(args[5]);
        maxIter = Integer.parseInt(args[6]);
        noTrees = Integer.parseInt(args[7]);

        MetroSampler sampler = new WeightedSamplerMH_Fast(controller);

        time = System.currentTimeMillis();
        sampler.sample(maxIter, seedSize, threshold);
        System.out.println("Sampling ... " + (System.currentTimeMillis() - time));
        sampler.statPair(noTrees);
    }

    public static void findATree(String[] args) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";
        double threshold = 0.0;
        int seedSize = 0;
        int maxIter = 0;
        int samplerType = 0;

        long time = System.currentTimeMillis();

        DecisionTreeController controller = null;

        System.out.println("Loading data ... " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        threshold = Double.parseDouble(args[4]);

        printInputFile(fileProteinExp, fileSampletoBinary, filePPI);
        controller = new DecisionTreeController(fileProteinExp, fileSampletoBinary, filePPI, false, 0);

        // sampling
        seedSize = Integer.parseInt(args[5]);
        maxIter = Integer.parseInt(args[6]);
        samplerType = Integer.parseInt(args[7]);
        int breastorbrain = Integer.parseInt(args[8]);
        if (samplerType != 1 && samplerType != 2) {
            System.out.println("Wrong Sampler Type!");
            System.out.println("Sampler Type: 1) No Qij 2) With Qij");
            return;
        }

        MetroSampler sampler = null;
        if (samplerType == 1) {
            sampler = new WeightedSamplerNoQ(controller);
        } else {
            sampler = new WeightedSamplerMH(controller);
        }

        time = System.currentTimeMillis();
        sampler.sample(maxIter, seedSize, threshold);
        System.out.println("Sampling ... " + (System.currentTimeMillis() - time));

        String brainLabelList[] = {"6774", "26039"};
        String breastLabelList[] = {"1051", "6776", "3297"};

        String devLabelList[] = {"3224", "4086", "7832", "3725", "57594"};

        if (breastorbrain == 1) {
            sampler.findATree(breastLabelList);

        } else if (breastorbrain == 2) {
            sampler.findATree(brainLabelList);
        } else {
            sampler.findATree(devLabelList);
        }
    }

    public static void convertFormat(String[] args) {
        String fileProteinExp = "";
        String fileSampletoBinary = "";
        String filePPI = "";

        long time = System.currentTimeMillis();

        time = System.currentTimeMillis();

        fileProteinExp = args[1];
        fileSampletoBinary = args[2];
        filePPI = args[3];
        int type = Integer.parseInt(args[4]);

        DecisionTreeController.convertFormat(fileProteinExp, fileSampletoBinary,
                getRemovedNodes(fileProteinExp, fileSampletoBinary, filePPI), type);
    }

    private static void createFoldSVM(String[] args) throws FileNotFoundException, IOException {
        String orgDataFile = args[1];
        int noFolds = Integer.parseInt(args[2]);
        BufferedReader br1 = new BufferedReader(new FileReader(orgDataFile));
        ArrayList<String> posData = new ArrayList<String>();
        ArrayList<String> negData = new ArrayList<String>();
        while (br1.ready()) {
            String Y = br1.readLine().trim();
            String[] X = Y.split(" ");
            if (X[0].contains("+1")) {
                posData.add(Y);
            } else {
                negData.add(Y);
            }
        }
        int testposSize = posData.size() / noFolds;
        int testnegSize = negData.size() / noFolds;

        Random generator = new Random();

        ArrayList<String> posTrainData = new ArrayList<String>();
        ArrayList<String> negTrainData = new ArrayList<String>();
        ArrayList<String> posTestData = new ArrayList<String>();
        ArrayList<String> negTestData = new ArrayList<String>();
        for (int fold = 0; fold < noFolds; fold++) {
            posTrainData.clear();
            posTrainData.addAll(posData);
            posTestData.clear();
            negTrainData.clear();
            negTrainData.addAll(negData);
            negTestData.clear();
            while (posTestData.size() < testposSize) {
                posTestData.add(posTrainData.remove(generator.nextInt(posTrainData.size())));
            }
            while (negTestData.size() < testnegSize) {
                negTestData.add(negTrainData.remove(generator.nextInt(negTrainData.size())));
            }
            // open test file
            String fileName = orgDataFile + "test" + Integer.toString(fold);
            FileWriter fstream = new FileWriter(fileName);
            BufferedWriter out = new BufferedWriter(fstream);
            for (String pos : posTestData) {
                out.write(pos + "\n");
            }
            for (String neg : negTestData) {
                out.write(neg + "\n");
            }
            out.close();
            //opentrainfile
            fileName = orgDataFile + "train" + Integer.toString(fold);
            fstream = new FileWriter(fileName);
            out = new BufferedWriter(fstream);
            for (String pos : posTrainData) {
                out.write(pos + "\n");
            }
            for (String neg : negTrainData) {
                out.write(neg + "\n");
            }
            out.close();
        }
    }
}
