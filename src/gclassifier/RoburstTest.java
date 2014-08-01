/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier;

import gclassifier.answer.SimpleTree;
import gclassifier.sampler.MetroSampler;
import gclassifier.sampler.PogNode;
import gclassifier.sampler.WeightedSamplerMH;
import gclassifier.sampler.WeightedSamplerMH_Fast;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author minhhx
 */
public class RoburstTest {

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length < 1) {
            instruction();
            return;
        }
        int option = Integer.parseInt(args[0]);
        switch (option) {
            case 0:
                plantTreeNFind(args);
                break;
            case 1:
                plantTreeDelEdgeNFind(args);
                break;
            default:
                instruction();
        }
    }
      
    public static void instruction(){
        System.out.println("Robust Test... Wrong number of input!");
        System.out.println("Plant tree & find: 0 <ppi> <noSamples> <threshold> <maxiter> <minSeedSize> <maxSeedSize>");
        System.out.println("Plant tree, del edge, & find: 1 <ppi> <noSamples> <threshold> <maxiter> <seedSize> <noTimes>");        
    }
        
    
    public static void plantTreeNFind(String[] args){
        String filePPI = args[1];
        int noSamples = Integer.parseInt(args[2]);
        double threshold = Double.parseDouble(args[3]);
        int maxIter = Integer.parseInt(args[4]);
        int minSeedSize = Integer.parseInt(args[5]);
        int maxSeedSize = Integer.parseInt(args[6]);

        System.out.println("SeedSize: " + minSeedSize + " - " + maxSeedSize);

//        double[] maxCommon = new double[maxSeedSize - minSeedSize + 1];

        PogNode seedSubgraph = new PogNode(new HashSet<Node>(), 0);

        for (int seedSize = minSeedSize; seedSize <= maxSeedSize; seedSize++) {
            System.out.println("Current seed size: " + seedSize);
            long time = System.currentTimeMillis();
            DecisionTreeController controller = new DecisionTreeController(filePPI,
                    noSamples, seedSubgraph, seedSize);
            System.out.println("Loading data ... " + (System.currentTimeMillis() - time));

            MetroSampler sampler = new WeightedSamplerMH_Fast(controller);

            time = System.currentTimeMillis();
            sampler.sample(maxIter, 1);
            System.out.println("Sampling ... " + (System.currentTimeMillis() - time));

            time = System.currentTimeMillis();
            sampler.visitCountStat();
            System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));
            
            sampler.chooseDiscriminatorySubgraph(threshold);

            double maxCommonValue = sampler.findSubgraphFromSample(seedSubgraph, threshold);
            if (maxCommonValue < 0.0) {
                System.out.println("Subgraphs Not Found! SeedSize " + seedSize);
            }

//            maxCommon[seedSize-minSeedSize] = maxCommonValue;
        }

//        // cout result
//        String strSeedSize = "";
//        String strCommon = "";
//        for (int i = 0; i < maxCommon.length; i++){
//            strSeedSize += ","+Integer.toString(i+minSeedSize);
//            strCommon += "," + Double.toString(maxCommon[i]);
//        }

//        System.err.println("\n\nCommon vs SeedSize\n" + strSeedSize +"\n"
//                + strCommon);
    }
    
    public static void plantTreeDelEdgeNFind(String[] args){
        String filePPI = args[1];
        int noSamples = Integer.parseInt(args[2]);
        double threshold = Double.parseDouble(args[3]);
        int maxIter = Integer.parseInt(args[4]);
        int seedSize = Integer.parseInt(args[5]);
        int noTimes = Integer.parseInt(args[6]);

        System.out.println("SeedSize: " + seedSize);

//        double[] maxCommon = new double[maxSeedSize - minSeedSize + 1];

        PogNode seedSubgraph = new PogNode(new HashSet<Node>(), 0);

        for (int i = 0; i <= noTimes; i++) {
            System.out.println("Current seed size: " + seedSize);
            long time = System.currentTimeMillis();
            DecisionTreeController controller = new DecisionTreeController(filePPI,
                    noSamples, seedSubgraph, seedSize);
            System.out.println("Loading data ... " + (System.currentTimeMillis() - time));                                        
                        
            while (!checkImportantEdge(seedSubgraph.nodeList(), controller)){
                controller = new DecisionTreeController(filePPI,
                    noSamples, seedSubgraph, seedSize);
                System.out.println("Loading data ... " + (System.currentTimeMillis() - time)); 
            }
                        
            MetroSampler sampler = new WeightedSamplerMH_Fast(controller);

            time = System.currentTimeMillis();
            sampler.sample(maxIter, 1);
            System.out.println("Sampling ... " + (System.currentTimeMillis() - time));

            time = System.currentTimeMillis();
            sampler.visitCountStat();
            System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));

            sampler.chooseDiscriminatorySubgraph(threshold);
            
            double maxCommonValue = sampler.findSubgraphFromAnswer(seedSubgraph, threshold);
            if (maxCommonValue < 0.0) {
                System.out.println("Subgraphs Not Found! SeedSize " + seedSize);
            }
            
            // delete one important edge (one that seperate the tree into two
            deleteImportantEdge(seedSubgraph.nodeList(), controller);
            
            time = System.currentTimeMillis();
            sampler.sample(maxIter, 1);
            System.out.println("Sampling ... " + (System.currentTimeMillis() - time));

            time = System.currentTimeMillis();
            sampler.visitCountStat();
            System.out.println("Visit Count stat ... " + (System.currentTimeMillis() - time));
            
            sampler.chooseDiscriminatorySubgraph(threshold);

            maxCommonValue = sampler.findSubgraphFromAnswer(seedSubgraph, threshold);
            if (maxCommonValue < 0.0) {
                System.err.println("Subgraphs Not Found! SeedSize " + seedSize);
            }
        }
    }
    
    // find an edge that is a bridge, del this edge would make the subgraph disconnected
    // to do that, find a node that is the neighbor of exactly two other nodes.
    public static void deleteImportantEdge(Set<Node> nodelist, DecisionTreeController controller){
        for (Node n1 : nodelist){
            Set<Node> neighbors = new HashSet<Node>(nodelist);
            neighbors.remove(n1);
            SimpleTree aTree = controller.buildSimpleTree(neighbors);
            if (aTree.weight < 1.0){                
                neighbors = new HashSet<Node>(n1.neighbors);
                neighbors.retainAll(nodelist);
                if (neighbors.size() >= 2 && neighbors.size() <= 3){
                    System.err.println("Delete Lost: " + aTree.weight);
                    for (Node n2 :neighbors){
                        controller.deleteEdge(n1.id, n2.id);
                        System.err.println("Delete Edge " + n1.toString() + "\t" + n2.toString());
                    }
                    break;
                }
            }
        }
    }
    
    // find an edge that is a bridge, del this edge would make the subgraph disconnected
    // to do that, find a node that is the neighbor of exactly two other nodes.
    public static boolean checkImportantEdge(Set<Node> nodelist, DecisionTreeController controller){
        for (Node n1 : nodelist){
            Set<Node> neighbors = new HashSet<Node>(nodelist);
            neighbors.remove(n1);
            SimpleTree aTree = controller.buildSimpleTree(neighbors);
            if (aTree.weight < 1.0){                
                neighbors = new HashSet<Node>(n1.neighbors);
                neighbors.retainAll(nodelist);
                if (neighbors.size() >= 2 && neighbors.size() <= 3){
                    return true;
                }
            }
        }
        return false;
    }
}
