/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gclassifier.answer;

import gclassifier.Graph;
import gclassifier.Node;
import gclassifier.TreeQualifier;
import gclassifier.TreeRandForest;
import gclassifier.quality.GenePair;
import gclassifier.quality.GenePairQuality;
import gclassifier.quality.GenePairRankerEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 *
 * @author minhhx
 */
public class OptimalForest {
    public Set<SimpleTree> forest;
    public OptimalForest(OptimallyDiscriminativeSet answerSet, TreeQualifier qualifier){
        forest = new HashSet<SimpleTree>();
        Collection<Subgraph> subgraphSet = answerSet.getAnswer();
        for (Subgraph sub : subgraphSet){
            forest.add(qualifier.buildSimpleTree(sub.nodes));
        }
    }

    public OptimalForest(Collection<Subgraph> subgraphSet, TreeQualifier qualifier){
        forest = new HashSet<SimpleTree>();
        double sumWeight = 0.0;
        for (Subgraph sub : subgraphSet){
            SimpleTree aTree = qualifier.buildSimpleTree(sub.nodes);
            forest.add(aTree);
            sumWeight += aTree.weight;
        }
        // normalize the weight of trees
        for (SimpleTree aTree : forest){
            aTree.weight /= sumWeight;
        }
    }

    // for Kasturi version of the tree
    public OptimalForest(boolean[][] storeFile, Set<Integer> sampleList, Graph ppiGraph, int noTrees){
        forest = new HashSet<SimpleTree>();
        for (int i = 0; i< noTrees; i++){
            TreeRandForest aTree = new TreeRandForest(storeFile, sampleList, ppiGraph.nodes);
            forest.add(new SimpleTree(aTree));
        }
    }

    // return the probability of class true
    public double classify(List<String> positiveFeatures){
        int countPositive = 0;
        for (SimpleTree tree : forest){
            if (tree.classify(positiveFeatures))
                countPositive++;
        }
        return (double)countPositive/forest.size();
    }

    public double weightedClassify(List<String> positiveFeatures){
        double weightedClass = 0.0;
        for (SimpleTree tree : forest){
            if (tree.classify(positiveFeatures))
                weightedClass += tree.weight;
        }
        return weightedClass;
    }
    
    // stat genepairs and most visited nodes for NGF
    public static void statNGF(boolean[][] storeFile, Set<Integer> sampleList, Graph ppiGraph, int noTrees, int noNodes){
        Set<TreeRandForest> ngfForest = new HashSet<TreeRandForest>();
        for (int i = 0; i< noTrees; i++){
            TreeRandForest aTree = new TreeRandForest(storeFile, sampleList, ppiGraph.nodes);
            ngfForest.add(aTree);            
        }
        statPair(ngfForest);
        findMostVisitedNodes(ngfForest, noNodes);
    }
    
    public static void statPair(Set<TreeRandForest> ngfForest) {
        PriorityQueue<GenePairRankerEntry> pairRanker = new PriorityQueue<GenePairRankerEntry>();

        HashMap<GenePair, GenePairQuality> genePairMap = new HashMap<GenePair, GenePairQuality>();

        for (TreeRandForest t : ngfForest){
            t.statPair(genePairMap);
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
           
            pairRanker.add(new GenePairRankerEntry(aPair.geneA, aPair.geneB, accuracy));
        }
        
        // print for fig 5a
        for (int i = 0; i < 20; i++) {
            System.err.print((i+1) * 0.05 + "\t");
            for (pairType = 0; pairType <= 2; pairType++) {
                System.err.print(noPairs[pairType][i] + "\t");
            }
            System.err.println();
        }
        
//        // print for fig 5b
//        int count = 0;
//        while (count < 100 && !pairRanker.isEmpty()){
//            count++;
//            GenePairRankerEntry entry = pairRanker.poll();
//            System.out.println(entry.geneA + "\t" + entry.geneB);
//        }
    }
    
    // find the most visited nodes in the forest created by NGF
    public static Set<Node> findMostVisitedNodes(Set<TreeRandForest> ngfForest, int noNodes){
        HashMap<Node, Integer> countNode = new HashMap<Node, Integer>(); // map Node to # visits
        for (TreeRandForest t : ngfForest){
            for (Node n : t.features()){
                if (countNode.containsKey(n)){
                    countNode.put(n, countNode.get(n)+1);
                } else {
                    countNode.put(n, 1);
                }
            }
        }
        
        // find the highest visited nodes
        Set<Node> bestNodes = new HashSet<Node>();
        PriorityQueue<NodeRanker> ranker = new PriorityQueue<NodeRanker>();
        for (Node n : countNode.keySet()){
            ranker.add(new NodeRanker(n, countNode.get(n)));
        }
        int i = 0;
        while (!ranker.isEmpty() && i < noNodes){
            bestNodes.add(ranker.poll().n);
            i++;
        }
        
        System.err.println("Best nodes NGF:");          
        for (Node n : bestNodes){
            System.err.println(n.label);
        }
        return bestNodes;
    }
}
