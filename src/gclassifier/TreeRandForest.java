//latest version

package gclassifier;

import gclassifier.answer.SimpleTreeNode;
import gclassifier.quality.GenePair;
import gclassifier.quality.GenePairQuality;
import java.util.*;
import java.lang.*;

public class TreeRandForest {

    RandForestNode rootNode;
    private static boolean[][] storeFile;
    Set<Node> usedFeatures;
    private double trueProb;
    //private static double prevGini = 0.0;
   
    public TreeRandForest(boolean[][] _storeFile, Set<Integer> sampleList, Set<Node> nodeList) {
        storeFile = _storeFile;
        //System.out.println("samplelist size" + sampleList.size());
        usedFeatures = new HashSet<Node>();
        Node seedNode = pickSeed(nodeList, sampleList);
	
         
        if (seedNode == null){
            rootNode = null;
            return;
        }

        buildDecTree(sampleList, seedNode);
    }

    public Set<Node> features(){
        return usedFeatures;
    }
    
    // simplify the tree to get a simple version of it for testing
    public SimpleTreeNode simplifyTree(){
        if (rootNode == null){
            SimpleTreeNode newRoot = new SimpleTreeNode(trueProb>=0.5);
            return newRoot;
        }
        return simplifyBranch(rootNode);
    }

    // simplify the branch to get a simple version of it for testing
    private SimpleTreeNode simplifyBranch(RandForestNode branch){
        if (branch == null)
            return null;
        if (branch.feature == null){
            SimpleTreeNode newNode = new SimpleTreeNode(branch.trueProb>=0.5);
            return newNode;
        } else {
            SimpleTreeNode newNode = new SimpleTreeNode(branch.feature.label);
            newNode.setChild(simplifyBranch(branch.trueChild), simplifyBranch(branch.falseChild));
            return newNode;
        }
    }

//    private HashSet<Node> geteligibleFeatures(HashSet<Node> possibleNodes){  //getting the features for which the neighborhood size is at least 5
//         HashSet<Node> eligibleFeatures = new HashSet();
//         eligibleFeatures.clear();
//         for(Node n: possibleNodes){
//             if(n.neighbors.size()>=5){
//                 eligibleFeatures.add(n);
//             }
//         }
//         return eligibleFeatures;
//     }

    private Node pickSeed(Set<Node> nodeList, Set<Integer> sampleList){
        int N = nodeList.size(); //number of  proteins
        int setSize = (int)Math.sqrt(N);
        //System.out.println("required num of proteins " + setSize);
        HashSet<Node> eligibleFeatures = new HashSet();
        //eligibleFeatures.addAll(nodeList);
        //Set<Integer> rand = new HashSet<Integer>();

        Random generator = new Random();
        int count = 0;
        while (eligibleFeatures.size() < setSize){
            int j = generator.nextInt(N);
            for(Node n: nodeList){
                if(n.id == j && n.neighbors.size()>=5){
//                    System.out.println("found one randomly selected node!!" + n.label);
                    if(!eligibleFeatures.contains(n))
                        eligibleFeatures.add(n);
                        
                    }
                }
            }
        
        //System.out.println("num of features tochoose from " + eligibleFeatures.size());
            //rand.add(j);
        

//        HashSet<Node> possibleNodes = new HashSet();
//        for(Node n: nodeList){  //really bad way of doing this, but can't help but iterate over the entire HashSet since I don't wnat to change the implementation
//            if(rand.contains(n.id))
//                possibleNodes.add(n);
//        }
//
      //Node selectedNode = null;
//        HashSet<Node> eligibleFeatures = geteligibleFeatures(possibleNodes);
        
        Node selectedNode = null;
        double GiniTemp = 1.0;
        for(Node m: eligibleFeatures){
            double temp = GiniFeature(m, sampleList);
            //System.out.println(m.label+"'s Gini index is " + temp);
            
            if(temp<GiniTemp){
                GiniTemp = temp;
                selectedNode = m;
            }
        }
        
       // if(selectedNode!=null)
        //System.out.println("root node picked: " + selectedNode.label + " and it's gini index is : " + GiniTemp);
        //prevGini = GiniTemp;
        return selectedNode;
    }

    public boolean isEmpty() {
        if (rootNode == null) {
            return true;
        } else {
            return false;
        }
    }

    public void InOrderTraversal(RandForestNode node) {
        if (node != null) {
            InOrderTraversal(node.trueChild);
            System.out.println("Traversed node " + node.feature.label);
            InOrderTraversal(node.falseChild);
        }
    }

    public void PreOrderTraversal() {
        System.out.println("Pre-order Traversed tree");
        PreOrderTraversal(rootNode);
        System.out.println("End Traversing!\n");
    }

    private void PreOrderTraversal(RandForestNode node) {
        if (node != null) {
            if (node.feature != null) {
                System.out.println(node.feature.label);
                System.out.print("left: ");
                PreOrderTraversal(node.trueChild);
                System.out.print("right: ");
                PreOrderTraversal(node.falseChild);
            } else {    // leaf node
                System.out.println("leaf");
                int classLabel;
                if(node.trueProb>0.5){
                    classLabel = 1;
                }
                else classLabel = 0;
                System.out.println("classlabel: " + classLabel);
            }
        }
    }

    //computing the Gini index of a feature
//    public static double GiniIndex(Node n, Set<Integer> samples){
//        int id = n.id;
////        int Trueclass0 = 0; //the count of the samples which have "true" value for the feature, but class label 0
////        int Trueclass1 = 0;
////        int Falseclass0 = 0;
////        int Falseclass1 = 0;
//        int TrueClass = 0; int FalseClass = 0;
//        int Set1TrueClass = 0; int Set0TrueClass = 0;
//        int Set1FalseClass = 0; int Set0FalseClass = 0;
//        for(int i : samples){
//            
//                    if(storeFile[i][0]==true){
//                        TrueClass ++;
//                        if(storeFile[i][id]==true)
//                            Set1TrueClass++;
//                        else Set0TrueClass++;
//                    }
//                    else {
//                        FalseClass ++;
//                        if(storeFile[i][id]==true)
//                            Set1FalseClass++;
//                        else Set0FalseClass++;
//                    }
////                n.sampleSetTrue.add(i);
//            }
//            
//
//        
//        double temp1 = (double)Set1TrueClass/(TrueClass);
//        double temp2 = (double)Set0TrueClass/(TrueClass);
//        double temp3 = (double)Set1FalseClass/(FalseClass);
//        double temp4 = (double)Set0FalseClass/(FalseClass);
//
//        double GiniTrue = 1.0 - (temp1*temp1 + temp2*temp2);
//        double GiniFalse = 1.0 - (temp3*temp3 + temp4*temp4);
//        double impurity = (TrueClass  * GiniTrue + FalseClass* GiniFalse)/samples.size();
//
//        return impurity;
//    }

    
    public static double Gini(Set<Integer> samples){
       int TrueClass = 0; int FalseClass = 0; 
        double t1 = 0.0; double t2 = 0.0;
        for(int i : samples){
            if(storeFile[i][0]==true)
                TrueClass++;
            else FalseClass++;
        }
//        System.out.println(" TrueClass size" + TrueClass);
//      System.out.println(" FalseClass size" + FalseClass);
      //System.out.println("testing basic math knowledge of the machine: " + TrueClass/samples.size());
        t1 = (1.0*TrueClass/samples.size());
        t2 =  (1.0*FalseClass/samples.size());
//        System.out.println("t1 "+ t1);
//        System.out.println("t2 "+ t2);
        
        double GiniSet = (double)(1.0 - (t1*t1+t2*t2));
        //double GiniSet = (double)(1.0 - (t1*t2));
        return GiniSet;
    }
    
    public static double GiniFeature(Node n, Set<Integer> samples){
        int _id = n.id;
//        System.out.println(" node id" + n.id);
        double GiniSet1 = 0.0; 
        double GiniSet0 = 0.0;
        Set<Integer> Set1 = new HashSet();
        Set<Integer> Set0 = new HashSet();
        
        for(int i : samples){
            
            if(storeFile[i][_id]==true)
                Set1.add(i);
            else Set0.add(i);
        }
        //System.out.println("samples size " + samples.size());
//        System.out.println("Set1 size " + Set1.size());
//        System.out.println("Set0 size " + Set0.size());
        if(Set1.size()>0)
            GiniSet1 = Gini(Set1);
        if(Set0.size()>0)
            GiniSet0 = Gini(Set0);
        
        double GiniF = (double)((1.0* Set1.size()/samples.size())*GiniSet1 + (1.0*Set0.size()/samples.size())*GiniSet0);
        
        return GiniF;
        
        
    }
    
    public Node chooseFeature(Set<Integer> sampleList, Set<Node> _candidateList, double prevGini) {
        double minGini = 1.0; Node chosenFeature = null;
        for(Node n: _candidateList){
            double gini = GiniFeature(n, sampleList);
          //  System.out.println("**********Gini for node " + n.label + " is "+ gini);
            if(gini<minGini){
                minGini = gini;
                chosenFeature = n;
            }
        }
//        System.out.println("module choose feature returns " + chosenFeature.label);
      // System.out.println("minGini " + minGini);
      //  System.out.println("prevgini " + prevGini);
//        System.out.println(" testing" + (minGini-prevGini));
        if (prevGini - minGini<= 0.02){
            return null;
        }
        
        //prevGini = minGini;
        
        return chosenFeature;
    }

    // given a feature ID and a sample list
    // split the sample list based on the value of that feature
    // output are store in two arraylists
    private void splitSampleOnFeature(int featureID, Set<Integer> sampleList,
            Set<Integer> trueSamples, Set<Integer> falseSamples) {
        for (Integer i : sampleList) {
            if (storeFile[i][featureID]) {
                trueSamples.add(i);
            } else {
                falseSamples.add(i);
            }
        }
    }
    
       
    // build the whole decision tree given a subgraph
    // output: return the misclassified number of the subtree
    private void buildDecTree(Set<Integer> sampleList, Node seedFeature) {
        // choose a feature from the entire subgraphNodeList
        Node chosenFeature = seedFeature;;
        

        // get the majority class in the sampleList
        int noTrue = getNoTrueClass(sampleList);
         double currentGini = GiniFeature(chosenFeature, sampleList);

        // if all samples are in the same class or no more feature to use, no need to split
        if (noTrue == 0 || noTrue == sampleList.size() || chosenFeature == null) {
            rootNode = null;
            trueProb = (double)noTrue / sampleList.size();
            return;
        }

        // otherwise, build a tree from scratch
        rootNode = new RandForestNode(chosenFeature, null, currentGini);
//        rootNode.infoGain = Gain;   //storing the information gain associated with the root
        usedFeatures.add(chosenFeature);

        Set<Integer> trueSamples = new HashSet<Integer>();
        Set<Integer> falseSamples = new HashSet<Integer>();

        // split the sample list
        splitSampleOnFeature(chosenFeature.id, sampleList, trueSamples, falseSamples);
        rootNode.sampleSetTrue = trueSamples;
        rootNode.sampleSetFalse = falseSamples;
        //System.out.println("size of truesamples of node"+ chosenFeature.label+ " is " + trueSamples.size());
        // update the current candidate node list
        Set<Node> candidates = rootNode.updateCandidateFeatures(new HashSet<Node>(), usedFeatures, chosenFeature);

        // continue to split node
        splitTreeNode(rootNode, candidates, true);
        splitTreeNode(rootNode, candidates, false);
    }

    
    
    // build the decision tree rooted at currentNode, given the curCandidate set
    // choose a new feature to split
    // output: return the misclassified number of the subtree
    private void splitTreeNode(RandForestNode currentNode, Set<Node> curCandidates, boolean isTrueChild) {
        Set<Integer> sampleList = null;
        if (isTrueChild) {
            sampleList = currentNode.sampleSetTrue;
        } else {
            sampleList = currentNode.sampleSetFalse;
        }
//        System.out.println("new sample list size for "+ isTrueChild + "class for node " + currentNode.feature.label + " is "+ sampleList.size());
//        System.out.println(" the smaple list is:");
//        for(int i: sampleList)
//            System.out.println(i);
//        System.out.println("current candidate list size " + curCandidates.size());
//        for(Node n: curCandidates)
//            System.out.println(n.label);
        Node chosenFeature = chooseFeature(sampleList, curCandidates, currentNode.GiniIndex);
        double currentGini = -1;
        //System.out.println("receied a chosen feature " + chosenFeature.label);
        if(chosenFeature!=null){
             currentGini = GiniFeature(chosenFeature, sampleList);
            //System.out.println("feature " + chosenFeature.label + "Gini value: " + currentNode.GiniIndex);
        }

        // get the majority class in the sampleList
        int _noTrue = getNoTrueClass(sampleList);
       /// System.out.println("num of trues " + _noTrue);
        // if all samples are in the same class or no more feature to use, no need to split
        if (_noTrue == 0 || _noTrue == sampleList.size() || chosenFeature == null) {
           // System.out.println("haha");
            RandForestNode newNode = new RandForestNode(null, currentNode, currentGini);
            newNode.trueProb = (double)_noTrue / sampleList.size();
            currentNode.insertChild(newNode, isTrueChild);
            return;
        }
        
        // other wise keep splitting
        Set<Integer> trueSamples = new HashSet<Integer>();
        Set<Integer> falseSamples = new HashSet<Integer>();

        // split the sample list
        splitSampleOnFeature(chosenFeature.id, sampleList, trueSamples, falseSamples);

        RandForestNode newNode = new RandForestNode(chosenFeature, currentNode, currentGini);
        usedFeatures.add(chosenFeature);

        // insert child into current node
        currentNode.insertChild(newNode, isTrueChild);

        // find the new candidate node list
        Set<Node> candidates = newNode.updateCandidateFeatures(curCandidates, usedFeatures, chosenFeature);

        if (!trueSamples.isEmpty()) {
            newNode.sampleSetTrue = trueSamples;
            // continue to split node
            splitTreeNode(newNode, candidates, true);
        }

        if (!falseSamples.isEmpty()) {
            newNode.sampleSetFalse = falseSamples;
            // continue to split node
            splitTreeNode(newNode, candidates, false);
        }
    }

    private int getNoTrueClass(Set<Integer> sampleList) {
        int noLabelTrue = 0;
        for (Integer i : sampleList) {
            if (storeFile[i][0]) {
                noLabelTrue++;
            }
        }
      return noLabelTrue;
    }
    
    // get the stat for gene pair A=root, B=direct childrent of A
    public void statPair(HashMap<GenePair, GenePairQuality> genePairMap){
        if (rootNode == null || rootNode.feature == null)
            return;
        RandForestNode child = null;
        GenePairQuality quality = null;
        Set<Integer> sampleSet = null;
        int noTrue = 0;
        if (rootNode.trueChild != null && rootNode.trueChild.feature != null){
            child = rootNode.trueChild;
            if (child.trueChild != null && child.trueChild.feature!= null){
                GenePair aPair = new GenePair(rootNode.feature.id, child.feature.id, true, true);
                noTrue = 0;
                sampleSet = child.sampleSetTrue;
                for (Integer i : sampleSet){
                    if (storeFile[i][0]) {
                        noTrue++;
                    }
                }
                quality = genePairMap.get(aPair);
                if (quality == null){
                    quality = new GenePairQuality();
                    quality.sumAcc = (double)(noTrue +1)/ (sampleSet.size()+2);
                    quality.noTrees = 1;
                    genePairMap.put(aPair,quality);
                } else {
                    quality.sumAcc += (double)(noTrue +1)/ (sampleSet.size()+2);
                    quality.noTrees++;
                }
            }
            if (child.falseChild != null && child.falseChild.feature != null){
                GenePair aPair = new GenePair(rootNode.feature.id, child.feature.id, true, false);
                noTrue = 0;
                sampleSet = child.sampleSetFalse;
                for (Integer i : sampleSet){
                    if (storeFile[i][0]) {
                        noTrue++;
                    }
                }
                quality = genePairMap.get(aPair);
                if (quality == null){
                    quality = new GenePairQuality();
                    quality.sumAcc = (double)(noTrue +1)/ (sampleSet.size()+2);
                    quality.noTrees = 1;
                    genePairMap.put(aPair,quality);
                } else {
                    quality.sumAcc += (double)(noTrue +1)/ (sampleSet.size()+2);
                    quality.noTrees++;
                }
            }
        }
        if (rootNode.falseChild != null && rootNode.falseChild.feature != null &&
                rootNode.falseChild.falseChild != null && rootNode.falseChild.falseChild.feature != null){
            child = rootNode.falseChild;
            GenePair aPair = new GenePair(rootNode.feature.id, child.feature.id, false, false);
            noTrue = 0;
            sampleSet = child.sampleSetFalse;
            for (Integer i : sampleSet){
                if (storeFile[i][0]) {
                    noTrue++;
                }
            }
            quality = genePairMap.get(aPair);
            if (quality == null){
                quality = new GenePairQuality();
                quality.sumAcc = (double)(noTrue +1)/ (sampleSet.size()+2);
                quality.noTrees = 1;
                genePairMap.put(aPair,quality);
            } else {
                quality.sumAcc += (double)(noTrue +1)/ (sampleSet.size()+2);
                quality.noTrees++;
            }
        }
    }
}
