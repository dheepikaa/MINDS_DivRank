/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.sampler;

import gclassifier.FixedSizePriorityQueue;
import gclassifier.Node;
import gclassifier.SubGraphFeature;
import gclassifier.TreeQualifier;
import gclassifier.answer.CrossValidation;
import gclassifier.answer.OptimalForest;
import gclassifier.answer.OptimallyDiscriminativeSet;
import gclassifier.answer.SVMEnsembler;
import gclassifier.answer.SimpleTree;
import gclassifier.answer.Subgraph;
import gclassifier.quality.GenePair;
import gclassifier.quality.GenePairQuality;
import gclassifier.quality.GenePairRankerEntry;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import org.apache.commons.collections4.queue.CircularFifoQueue;

/**
 *
 * @author minhhx
 */
public abstract class MetroSampler {

    protected Random generator = new Random();
    protected HashMap<PogNode, Integer> pogVisitCount = null;
    protected static double theta; // potential threshold for answer set
    
    protected int totalVisitCount;
    protected TreeQualifier qualifier;
    protected static OptimallyDiscriminativeSet answerSet = null;
    public static double normK = 1e4; // the normalization factor K in equation 13. Used to compute removing prob
    public static double normGamma = 1e5; // the normalization factor K in equation for gamma. Used to compute border prob
    public static boolean useGamma = false; // signal if using gamma (for bolder prob) or not    
    public static double minGamma = 0.7;
    public static boolean limitEditMap = false; // signal if the edit map is limitted, i.e. filter the addable list from Nodes that give no gain.
    public static double epsilon = 1.e-20; // used for potential v(g), if the graph is not minimal
    public static double normBetaExp = 10; // normalization factor for the exp function to calc deletion prob
    public static double normGammaExp = 50; // normalization factor for the exp function to calc border prob
    public static boolean useSVM = false; // signal if use SVM to compute potential and edge weights
    public static boolean useFastVersion = false;
    public static boolean useFastVersionDivRank = false;

    public static boolean doStat= true; // signal if store results into pogVisitCount for stat or not

    public static HashMap<ArrayList<Integer>, Integer> visit_count = null;
    public static HashMap<ArrayList<Integer>, Double> nodePotential = null;
    public static FixedSizePriorityQueue featureQueue;
    public static CircularFifoQueue<Integer> potentialQueue;

    // perform the sampling
    public abstract void sample(int maxIter, int seedSize);

    public void printStatistics() {
    	
    }
    public void sample(int maxIter, int seedSize, double threshold) {
    }

    // calculate beta : the probability of removing a vertex from the subgraph
    // currentPogNode.infoDensity: the percentage of correctly classified samples
    public static double calcDeletionProb(PogNode currentPogNode) {
        if (currentPogNode.size() == 0) {
            return 0.0;
        }
        
        //return 1.0 - Math.log(1.0 + (1.0-currentPogNode.infoDensity) * normK) / Math.log(1.0 + normK);
        return Math.exp(normBetaExp * currentPogNode.infoDensity) / Math.exp(normBetaExp) ;
                //* currentPogNode.getNoNeighborDown()/currentPogNode.getNoNeighborUp();
    }
    
    // calculate gamma: the probability to move toward the boundary
    // currentPogNode.infoDensity: the percentage of correctly classified samples
    public static double calcLeavingBorderProb(PogNode currentPogNode) {
        if (currentPogNode.size() == 0) {
            return 0.0;
        }        
        //return 1.0 - Math.log(1.0 + (1.0-currentPogNode.infoDensity) * normGamma) / Math.log(1.0 + normGamma) * (1.0-minGamma) + minGamma;    
        return Math.exp(normGammaExp * currentPogNode.infoDensity) / Math.exp(normGammaExp);
//        return Math.exp(normGammaExp * currentPogNode.infoDensity) / Math.exp(normGammaExp) * (1.0-minGamma) + minGamma;
        //return currentPogNode.getNoNeighborDown()/currentPogNode.getNoNeighborUp();
    }

    public void visitCountStat() {
        visitCountStat(pogVisitCount);
    }

    public static void visitCountStat(HashMap<PogNode, Integer> _pogVisitCount) {
        // individual count
        int maxCount = 500;
        int[] individualCount = new int[maxCount];
        int[] visitPerSize = new int[maxCount];
        int[] visitPerPotential = new int[101];
        int[] noPotential = new int[101];
        int[] noSize = new int[maxCount];
        double[] avgPotentialPerSize = new double[maxCount];
        Arrays.fill(individualCount, 0);
        Arrays.fill(visitPerSize, 0);
        Arrays.fill(visitPerPotential, 0);
        Arrays.fill(noPotential, 0);
        Arrays.fill(noSize, 0);
        Arrays.fill(avgPotentialPerSize, 0.0);

        int maxVisitCount = 0;

        System.out.println("Start ... ");

        for (Map.Entry<PogNode, Integer> entry : _pogVisitCount.entrySet()) {
            PogNode key = entry.getKey();
            Integer value = entry.getValue();

            if (value > maxCount) {
                value = maxCount - 1;
            }
            if (maxVisitCount < value) {
                maxVisitCount = value;
            }
            individualCount[value - 1]++;

            int size = key.size();
            if (size > maxCount) {
                size--;
            }
            visitPerSize[size] += value;
            noSize[size]++;
            avgPotentialPerSize[size] += key.infoDensity;

            int correctBucket = (int) (key.infoDensity * 100);
            visitPerPotential[correctBucket] += value;
            noPotential[correctBucket]++;
        }

        String strNoVisits = "";
        String strNoPatterns = "";
        for (int i = 0; i < maxVisitCount; i++) {
            if (individualCount[i] == 0) {
                continue;
            }
            strNoVisits += "," + Integer.toString(i + 1);
            strNoPatterns += "," + Integer.toString(individualCount[i]);
        }

        String strSizeHistogram = "";
        String strSize = "";
        String strVisitPerSizeAvg = "";
        String strCorrectPerSizeAvg = "";
        for (int i = 0; i < maxCount; i++) {
            if (visitPerSize[i] == 0) {
                continue;
            }
            strSize += "," + Integer.toString(i);
            strSizeHistogram += "," + Integer.toString(noSize[i]);
            strVisitPerSizeAvg += "," + Double.toString((double) visitPerSize[i] / noSize[i]);
            strCorrectPerSizeAvg += "," + Double.toString(avgPotentialPerSize[i] / noSize[i]);
        }

        String strCorrectBucket = "";
        String strVisitPerCorrectAvg = "";
        String strVisitPerCorrectSum = "";
        for (int i = 0; i < 101; i++) {
            if (visitPerPotential[i] == 0) {
                continue;
            }
            strCorrectBucket += "," + Double.toString(i / 100.0);
            strVisitPerCorrectAvg += "," + Double.toString((double) visitPerPotential[i] / noPotential[i]);
            strVisitPerCorrectSum += "," + Integer.toString(visitPerPotential[i]);
        }               

//        System.err.println("Individual Count:\n" + strNoVisits + "\n" + strNoPatterns + "\n");
//        System.err.println("Avg Visit per size:\n" + strSize + "\n" + strVisitPerSizeAvg
//                + "\nAvg Potential persize:\n" + strCorrectPerSizeAvg
//                + "\nSize Histogram:\n" + strSizeHistogram + "\n");
//        System.err.println("Avg Visit per Potential:\n" + strCorrectBucket + "\n" + strVisitPerCorrectAvg
//                + "\nVisit per Potential:\n" + strVisitPerCorrectSum + "\n");
        
        
        System.err.println("size\nvisitPerSize\npotentialPerSize\nSizeHist\nPotentialBucket\nPotentialHist");
        System.err.println( strSize + "\n" + strVisitPerSizeAvg + "\n" + strCorrectPerSizeAvg + "\n" + strSizeHistogram + "\n");
        System.err.println( strCorrectBucket + "\n" + strVisitPerCorrectSum + "\n");
        
        // get the final answerset
//        answerSet = chooseDiscriminatorySubgraph(_pogVisitCount, theta);
    }

    public void visitCountStat(double threshold) {
        visitCountStat(pogVisitCount, threshold);
    }

    public static void visitCountStat(HashMap<PogNode, Integer> _pogVisitCount, double threshold) {
        // individual count
        int maxCount = 500;
        int[] individualCount = new int[maxCount];
        int[] visitPerSize = new int[maxCount];
        int[] visitPerPotential = new int[101];
        int[] noPotential = new int[101];
        int[] noSize = new int[maxCount];
        double[] avgPotentialPerSize = new double[maxCount];
        Arrays.fill(individualCount, 0);
        Arrays.fill(visitPerSize, 0);
        Arrays.fill(visitPerPotential, 0);
        Arrays.fill(noPotential, 0);
        Arrays.fill(noSize, 0);
        Arrays.fill(avgPotentialPerSize, 0.0);

        int maxVisitCount = 0;

        System.out.println("Start ... ");

        int counter = 0;
        for (Map.Entry<PogNode, Integer> entry : _pogVisitCount.entrySet()) {
            PogNode key = entry.getKey();
            Integer value = entry.getValue();

            if (key.infoDensity < threshold) {
                continue;
            }

            if (value > maxCount) {
                value = maxCount - 1;
            }
            if (maxVisitCount < value) {
                maxVisitCount = value;
            }
            individualCount[value - 1]++;

            int size = key.size();
            if (size > maxCount) {
                size--;
            }
            visitPerSize[size] += value;
            noSize[size]++;
            avgPotentialPerSize[size] += key.infoDensity;

            int correctBucket = (int) (key.infoDensity * 100);
            visitPerPotential[correctBucket] += value;
            noPotential[correctBucket]++;
        }

        String strNoVisits = "";
        String strNoPatterns = "";
        for (int i = 0; i < maxVisitCount; i++) {
            if (individualCount[i] == 0) {
                continue;
            }
            strNoVisits += "," + Integer.toString(i + 1);
            strNoPatterns += "," + Integer.toString(individualCount[i]);
        }

        String strSizeHistogram = "";
        String strSize = "";
        String strVisitPerSizeAvg = "";
        String strCorrectPerSizeAvg = "";
        for (int i = 0; i < maxCount; i++) {
            if (visitPerSize[i] == 0) {
                continue;
            }
            strSize += "," + Integer.toString(i);
            strSizeHistogram += "," + Integer.toString(noSize[i]);
            strVisitPerSizeAvg += "," + Double.toString((double) visitPerSize[i] / noSize[i]);
            strCorrectPerSizeAvg += "," + Double.toString(avgPotentialPerSize[i] / noSize[i]);
        }

        String strCorrectBucket = "";
        String strVisitPerCorrectAvg = "";
        String strVisitPerCorrectSum = "";
        for (int i = 0; i < 101; i++) {
            if (visitPerPotential[i] == 0) {
                continue;
            }
            strCorrectBucket += "," + Double.toString(i / 100.0);
            strVisitPerCorrectAvg += "," + Double.toString((double) visitPerPotential[i] / noPotential[i]);
            strVisitPerCorrectSum += "," + Integer.toString(visitPerPotential[i]);
        }

        System.err.println("Individual Count:\n" + strNoVisits + "\n" + strNoPatterns + "\n");
        System.err.println("Avg Visit per size:\n" + strSize + "\n" + strVisitPerSizeAvg
                + "\nAvg Potential persize:\n" + strCorrectPerSizeAvg
                + "\nSize Histogram:\n" + strSizeHistogram + "\n");
        System.err.println("Avg Visit per Potential:\n" + strCorrectBucket + "\n" + strVisitPerCorrectAvg
                + "\nVisit per Potential:\n" + strVisitPerCorrectSum + "\n");
    }

    public void chooseDiscriminatorySubgraph(double threshold) {
        answerSet.clear();
        long time = System.currentTimeMillis();
        for (PogNode key : pogVisitCount.keySet()) {
            if (key.infoDensity >= threshold) {
                answerSet.addSubgraph(key.nodeList(), key.infoDensity);
            }
        }
        System.out.println("\nNum discriminative subgraphs: " + answerSet.size() + ". Time: " + (System.currentTimeMillis() - time));
        answerSet.potentialStat();
//        answerSet.print();
    }
    
    public void printChosenSubgraph(double threshold){
        answerSet.clear();
        long time = System.currentTimeMillis();
        for (PogNode key : pogVisitCount.keySet()) {
            if (key.infoDensity >= threshold) {
                answerSet.addSubgraph(key.nodeList(), key.infoDensity);
            }
        }
        System.out.println("\nNum discriminative subgraphs: " + answerSet.size() + ". Time: " + (System.currentTimeMillis() - time));        
        answerSet.print();
    }
    
    public void printChosenTrees(double threshold){        
        for (Subgraph g: answerSet.getAnswer()) {
            g.print();
            SimpleTree aTree = qualifier.buildSimpleTree(g.nodes);                
            aTree.print();
        }        
    }
    
    public static OptimallyDiscriminativeSet chooseDiscriminatorySubgraph(HashMap<PogNode, Integer> pogVisitCount, double threshold) {
        long time = System.currentTimeMillis();
        OptimallyDiscriminativeSet _answer = new OptimallyDiscriminativeSet();
        for (PogNode key : pogVisitCount.keySet()) {
            if (key.infoDensity >= threshold) {
                _answer.addSubgraph(key.nodeList(), key.infoDensity);
            }
        }
        System.out.println("\nNum discriminative subgraphs: " + _answer.size() + ". Time: " + (System.currentTimeMillis() - time));
        _answer.potentialStat();
        return _answer;
//        answerSet.print();
    }

    public void findATree(String nodeLabelList[]){
        // change label list to node list
        Set<Node> targetNodeList = new HashSet<Node>();
        for (String i : nodeLabelList){
            Node u = qualifier.getNodeByLabel(i);
            if (u!=null)
                targetNodeList.add(u);
        }
        // check if they are neighbors
        boolean areNeighbors = false;
        for (Node u : targetNodeList){
            for (Node v : targetNodeList){
                if (u.neighbors.contains(v)){
                    areNeighbors = true;
                    System.out.println("Neighbors: " + u.label + " " + v.label);
                    break;
                }
            }
        }
        if (!areNeighbors){
            System.out.println("No neighbors at all!");
        }
        
        Set<Node> intersection = new HashSet<Node>();
        for (Map.Entry<PogNode, Integer> entry : pogVisitCount.entrySet()) {
            PogNode key = entry.getKey();
            intersection.clear();
            intersection.addAll(targetNodeList);
            intersection.retainAll(key.nodeList());
            if (intersection.size() == targetNodeList.size()){
                SimpleTree aTree = qualifier.buildSimpleTree(key.nodeList());                
                aTree.print();
            }
        }
    }
    
    public double findSubgraphFromSample(PogNode p, double threshold) {
        System.out.println("Seed Subgraphs: " + p.toString()
                + " expected Info " + p.infoDensity);

//        chooseDiscriminatorySubgraph(threshold);
//        for (Subgraph q : answerSet.getAnswer()){
//            Set<Node> intersection = new HashSet<Node>(p.nodeList());
//            intersection.retainAll(q.nodes);
//            if (!intersection.isEmpty()){
//                System.out.print("Found Subgraph: ");
//                q.print();
//                result = true;
//            }
//        }

        double maxOverlapSize = -1.0; // size of the overlap compared to size of the found subgaph
        double maxOverlapInfo = -1.0;
        double maxCommonValue = -1.0;
        int maxIntersection = -1;
        PogNode maxNodeCommon = null;
        PogNode maxNodeIntersect = null;
        PogNode maxNodeOverlap = null;

        for (PogNode key : pogVisitCount.keySet()) {
            if (key.infoDensity >= threshold) {//key.equals(p)){
                Set<Node> intersection = new HashSet<Node>(p.nodeList());
                intersection.retainAll(key.nodeList());
                if (!intersection.isEmpty()) {
                    double common = (double) intersection.size()
                            / (p.nodeList().size() + key.nodeList().size() - intersection.size());
                    if (maxCommonValue < common) {
                        maxCommonValue = common;
                        maxNodeCommon = key;
                    }
                    if (maxIntersection < intersection.size()
                            || maxIntersection == intersection.size()
                            && maxNodeIntersect.size() > key.size()) {
                        maxIntersection = intersection.size();
                        maxNodeIntersect = key;
                    }
                    if (maxOverlapSize < (double)intersection.size()/key.size() && maxOverlapInfo <= key.infoDensity) {
                        maxOverlapSize = (double)intersection.size()/key.size();
                        maxOverlapInfo = key.infoDensity;
                        maxNodeOverlap = key;
                    }
                }
            }
        }
        if (maxNodeCommon != null) {
            System.err.println("Seed " + p.size() + " Max Jacquard: " + maxCommonValue + " "
                    + " info " + maxNodeCommon.infoDensity + " size " + maxNodeCommon.size()
                    + " " + maxNodeCommon.toString());
        }

        if (maxNodeIntersect != null) {
            System.err.println("Seed " + p.size() + " Max Intersection: " + maxIntersection + " "
                    + " info " + maxNodeIntersect.infoDensity + " size " + maxNodeIntersect.size()
                    + " " + maxNodeIntersect.toString());
        }
        if (maxNodeOverlap != null) {
            System.err.println("Seed " + p.size() + " Max Overlap/FoundSize: " + maxOverlapSize + " "
                    + " info " + maxNodeOverlap.infoDensity + " size " + maxNodeOverlap.size()
                    + " " + maxNodeOverlap.toString());
        }

        return maxCommonValue;
    }
    
    public double findSubgraphFromAnswer(PogNode p, double threshold) {
        System.err.println("Seed Size:" + p.size()
                + "\tInfo:" + p.infoDensity
                + "\t" + p.toString());

        //chooseDiscriminatorySubgraph(threshold);
        double maxScore = 0;
        int minScoreSize = Integer.MAX_VALUE;
        double maxOverlapRatio = 0;
        int maxOverlapSize = 0;
        double maxOverlapSizeJacquard = 0;
        double maxJacquard = 0;
        int minOverlapGraphSize = Integer.MAX_VALUE;
        int minJacquardGraphSize = Integer.MAX_VALUE;
        double maxJacquardScore = 0;
                
        Subgraph maxNodeOverlapSize = null;
        Subgraph maxNodeJacquard = null;
        Subgraph maxNodeScore = null;
        
        double jacquard = 0, overlapRatio = 0;
        int overlapSize = 0;
        
        //chooseDiscriminatorySubgraph(threshold);
        for (Subgraph q : answerSet.getAnswer()){
            Set<Node> intersection = new HashSet<Node>(p.nodeList());
            intersection.retainAll(q.nodes);
            
            overlapSize = intersection.size();
            jacquard = (double)overlapSize / (p.size() + q.size() - overlapSize);
            overlapRatio = (double)overlapSize / q.size();
//            
            if (maxOverlapSize < overlapSize ||
                    maxOverlapSize == overlapSize &&  q.size() < minOverlapGraphSize ||
                    maxOverlapSize == overlapSize &&  q.size() == minOverlapGraphSize && maxOverlapSizeJacquard < jacquard){
                maxOverlapSize = overlapSize;
                minOverlapGraphSize = q.size();
                maxOverlapSizeJacquard = jacquard;
                maxNodeOverlapSize = q;
            }
            
            if (q.score >= maxJacquardScore &&
                    (maxJacquard < jacquard ||
                    maxJacquard == jacquard && q.size() < minJacquardGraphSize)){
                maxJacquard = jacquard;
                minJacquardGraphSize = q.size();
                maxNodeJacquard = q;
                maxJacquardScore = q.score;
            }   
            
            if (q.score > maxScore ||
                    q.score == maxScore && maxOverlapRatio < overlapRatio ||
                    q.score == maxScore && maxOverlapRatio == overlapRatio && q.size() < minScoreSize){
                maxScore = q.score;
                minScoreSize = q.size();
                maxOverlapRatio = overlapRatio;                
                maxNodeScore = q;
            }
        }
        
        if (maxNodeOverlapSize != null) {
            System.err.println("Seed Over:"
                    + "\tSize:" + maxNodeOverlapSize.size()
                    + "\tOverlap:" + maxOverlapSize 
                    + "\tinfo:" + maxNodeOverlapSize.score  
                    + "\tjacquard:" + maxOverlapSizeJacquard                                 
                    );//+ "\t" + maxNodeOverlapSize.toString());
        }
//        
        if (maxNodeJacquard != null) {
            Set<Node> intersection = new HashSet<Node>(p.nodeList());
            intersection.retainAll(maxNodeJacquard.nodes);
            System.err.println("Seed Jacq:" 
                    + "\tSize:" + maxNodeJacquard.size()
                    + "\tOverlap:" + intersection.size() 
                    + "\tinfo:" + maxNodeJacquard.score                      
                    + "\tjacquard:" + maxJacquard                   
                    );//+ "\t" + maxNodeJacquard.toString());
        }
        
        if (maxNodeScore != null) {
            Set<Node> intersection = new HashSet<Node>(p.nodeList());
            intersection.retainAll(maxNodeScore.nodes);
            jacquard = (double)intersection.size() / (p.size() + maxNodeScore.size() - intersection.size());
            System.err.println("Seed Rati:" 
                    + "\tSize:" + maxNodeScore.size()
                    + "\tOverlap:" + intersection.size() 
                    + "\tinfo:" + maxNodeScore.score                         
                    + "\tjacquard:" + jacquard                
                    );//+ "\t" + maxNodeScore.toString());
        }
        return maxNodeScore.score;
    }

    public OptimalForest createForest(double threshold) {
        chooseDiscriminatorySubgraph(threshold);
        return new OptimalForest(answerSet, qualifier);
    }

    public OptimalForest createForest(double threshold, int noSubgraphs) {
//        chooseDiscriminatorySubgraph(threshold);
        PriorityQueue<Subgraph> pQueue = answerSet.sortAnswer();
        ArrayList<Subgraph> chosenSubgraphs = new ArrayList<Subgraph>();
        if (noSubgraphs > pQueue.size()) {
            chosenSubgraphs.addAll(pQueue);
        } else {
            for (int i = 0; i < noSubgraphs; i++) {
                chosenSubgraphs.add(pQueue.poll());
            }
        }
        System.out.println("No of trees in forest: " + chosenSubgraphs.size());
//        System.out.print("Graph size: ");
//        for (Subgraph g : chosenSubgraphs) {
//            System.out.print(g.size() + " ");
//        }
//        System.out.println();
        return new OptimalForest(chosenSubgraphs, qualifier);
    }
    
    public SVMEnsembler createSVMEnsembler(double threshold, int noSubgraphs) {
        //chooseDiscriminatorySubgraph(threshold);
        PriorityQueue<Subgraph> pQueue = answerSet.sortAnswer();
        ArrayList<Subgraph> chosenSubgraphs = new ArrayList<Subgraph>();
        if (noSubgraphs > pQueue.size() || noSubgraphs== 0) {
            chosenSubgraphs.addAll(pQueue);
        } else {
            for (int i = 0; i < noSubgraphs; i++) {
                chosenSubgraphs.add(pQueue.poll());
            }
        }
        System.out.println("No of subgraphs in svm ensembler: " + chosenSubgraphs.size());
//        System.out.print("Graph size: ");
//        for (Subgraph g : chosenSubgraphs) {
//            System.out.print(g.size() + " ");
//        }
//        System.out.println();
        
        return new SVMEnsembler(chosenSubgraphs, qualifier);
    }

    public void statPair(int noTrees) {
//        chooseDiscriminatorySubgraph(threshold);
        PriorityQueue<Subgraph> pQueue = answerSet.sortAnswer();
        PriorityQueue<GenePairRankerEntry> pairRanker = new PriorityQueue<GenePairRankerEntry>();

        HashMap<GenePair, GenePairQuality> genePairMap = new HashMap<GenePair, GenePairQuality>();
        if (noTrees > pQueue.size()) {
            noTrees = pQueue.size();
        }
        Subgraph g = null;
        for (int i = 0; i < noTrees; i++) {
            g = pQueue.poll();
            qualifier.buildTreeStatPair(g.nodes, genePairMap);
        }
        GenePair aPair = null;
        GenePairQuality quality = null;
        int pairType = 0;
        int noPairs[][] = new int[3][20];
        double accuracy = 0;
        for (Map.Entry<GenePair, GenePairQuality> entry : genePairMap.entrySet()) {
            aPair = entry.getKey();
            quality = entry.getValue();
            if (aPair.geneAExpression && aPair.geneBExpression) {
                pairType = 2;
            } else if (aPair.geneAExpression && !aPair.geneBExpression) {
                pairType = 0;
            } else if (!aPair.geneAExpression && !aPair.geneBExpression) {
                pairType = 1;
            }
            accuracy = quality.accuracy();
//            System.err.println(pairType + "," + quality.accuracy());
            for (int i = 1; i <= 20; i++) {
                if (accuracy > i * 0.05) {
                    noPairs[pairType][i - 1]++;
                }
            }
            
            // print out all the pairs for fig 5b
//            System.out.println(aPair.geneA + "\t" + aPair.geneB + "\t" + aPair.geneAExpression +"\t"+ aPair.geneBExpression);
            pairRanker.add(new GenePairRankerEntry(aPair.geneA, aPair.geneB, accuracy));
        }
        
        // print for fig 5a
        System.out.println("GenePairType stat: ");
        for (int i = 0; i < 20; i++) {
            System.err.print((i+1) * 0.05 + "\t");
            for (pairType = 0; pairType <= 2; pairType++) {
                System.err.print(noPairs[pairType][i] + "\t");
            }
            System.err.println();
        }
        
        // print for fig 5b
        System.out.println("GenePair: ");
        int count = 0;
        while (count < 100 && !pairRanker.isEmpty()){
            count++;
            GenePairRankerEntry entry = pairRanker.poll();
            System.out.println(entry.geneA + "\t" + entry.geneB);
        }
    }

    public void printSomeTree(int size) {
        PriorityQueue<SimpleTree> pqueue = new PriorityQueue<SimpleTree>();
        for (PogNode key : pogVisitCount.keySet()) {
            if (key.size() != size) {
                continue;
            }
            SimpleTree aTree = qualifier.buildSimpleTree(key.nodeList());
            if (!pqueue.isEmpty() && pqueue.size() >= 5) {
                if (pqueue.peek().weight < aTree.weight) {
                    pqueue.poll().featureList.clear();
                    pqueue.add(aTree);
                }
            } else {
                pqueue.add(aTree);
            }
        }
        System.out.println("No of tree: " + pqueue.size());
        Set<Node> intersection = new HashSet<Node>();
        Set<String> nodeLabelList = new HashSet<String>();
        int count = 1;
        while (!pqueue.isEmpty()) {
            // print the tree
            System.out.println("Tree size: " + size + " no: " + count);
            SimpleTree aTree = pqueue.poll();
            aTree.print();
//             print the ppi subgraph
            System.out.println("Tree size: " + size + " no: " + count);
            for (Node aFeature : aTree.featureList) {
                nodeLabelList.add(aFeature.label);
                intersection.addAll(aFeature.neighbors);
                intersection.retainAll(aTree.featureList);
                for (Node aNode : intersection) {
                    if (aFeature.id < aNode.id) {
                        System.out.println(aFeature.label + "\t" + aNode.label);
                    }
                }
                intersection.clear();
            }
            count++;
        }
        //System.err.println("\nNode ID list:");
        for (String n : nodeLabelList){
            System.err.println(n);
        }
    }
    
    public void findMostVisitedNodes(int size) {
        System.err.println("Best nodes:");
        Set<Node> nodes = answerSet.findMostVisitedNodes(size);               
        for (Node n : nodes){
            System.err.print(n.label+" ");
        }
    }
}
