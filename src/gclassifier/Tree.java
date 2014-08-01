package gclassifier;

import gclassifier.answer.SimpleTreeNode;
import gclassifier.quality.GenePair;
import gclassifier.quality.GenePairQuality;
import gclassifier.sampler.PogNode;
import gclassifier.sampler.WeightedSamplerMH_Pilot;
import java.util.*;

public class Tree {

    TreeNode rootNode;
    private double Gain;
    private boolean[][] storeFile;
    public Set<Node> featureList;
    Set<Node> usedFeatures;
    private int noCorrect;
    private boolean classLabel;

    HashMap<Integer,Set<TreeNode>> treeNodeMap;
    Set<Integer> unclassified;
    
    // used for stat of avg potential per size
    public static HashMap<Integer, Double> sumPotential = new HashMap<Integer, Double>(); // graph size -> sum of potential
    public static HashMap<Integer, Integer> numGraphs = new HashMap<Integer, Integer>(); // graph size -> num of graphs
    public static double maxPotential = 0.0;
    public static double minPotential = 1.0;
    
    // get the average potential for a given size
    public static double getAvgPotential(int size){
        if (sumPotential.containsKey(size)){
            return sumPotential.get(size)/numGraphs.get(size);
        } else {
            return 0.0;
        }
    }
    
    // check if a given size is in the desirable zone (the boundary)
    public static boolean inZone(int size){
        if (!Tree.sumPotential.containsKey(size)){
            return false;
        }
        double potential = Tree.getAvgPotential(size);
        double lastPotential = Tree.getAvgPotential(size - 1);
        if (potential >= WeightedSamplerMH_Pilot.minPotentialRatio * Tree.maxPotential &&
                potential - lastPotential >= WeightedSamplerMH_Pilot.minDeltaPotentialRatio * Tree.maxPotential){
            return true;
        } else {
            return false;
        }
    }
    
    // check if a given graph is in the desirable zone (the boundary)
    public static boolean inZone(PogNode q){
        int size = q.size();
        if (!Tree.sumPotential.containsKey(size)){
            return false;
        }
        double potential = q.infoDensity;
        double lastPotential = Tree.getAvgPotential(size - 1);
        if (potential >= WeightedSamplerMH_Pilot.minPotentialRatio * Tree.maxPotential &&
                potential - lastPotential >= WeightedSamplerMH_Pilot.minDeltaPotentialRatio * Tree.maxPotential){
            return true;
        } else {
            return false;
        }
    }

    public Tree(boolean[][] _storeFile, Set<Integer> sampleList, Set<Node> _subgraphNodeList) {
        storeFile = _storeFile;
        usedFeatures = new HashSet<Node>();
        featureList = new HashSet<Node>(_subgraphNodeList);
        treeNodeMap = new HashMap<Integer, Set<TreeNode>>();
        unclassified = new HashSet<Integer>();
        
        if (featureList.isEmpty()){            
            noCorrect = getNoCorrect(sampleList);
            if (noCorrect < 0){
                classLabel = false;
                noCorrect = -noCorrect;
            } else {
                classLabel = true;
            }
            unclassified.addAll(sampleList);
            rootNode = null;
        } else {
            noCorrect = buildDecTree(sampleList);
            
            // update the statistics of avg potential vs size
            double potential = (double)noCorrect/sampleList.size();
            if (maxPotential < potential){
                maxPotential = potential;               
            }
            if (minPotential > potential){
                minPotential = potential;
            }
            if (!sumPotential.containsKey(featureList.size())){
                sumPotential.put(featureList.size(), potential);
                numGraphs.put(featureList.size(),1);                        
            } else {
                double curPo = sumPotential.get(featureList.size());
                sumPotential.put(featureList.size(), curPo + potential);
                int curNo = numGraphs.get(featureList.size());
                numGraphs.put(featureList.size(),curNo + 1); 
            }
        }        
    }

    Tree(Tree _tree){
        treeNodeMap = new HashMap<Integer, Set<TreeNode>>();
        rootNode = _tree.cloneBranch(_tree.rootNode, null, treeNodeMap);
        storeFile = _tree.storeFile;
        featureList = new HashSet<Node>(_tree.featureList);
        usedFeatures = new HashSet<Node>(_tree.usedFeatures);
        unclassified = new HashSet<Integer>(_tree.unclassified);
        noCorrect = _tree.noCorrect;
        if (!useAllFeatures()){
            System.out.println("Copy from faulty object");
        }
    }
    
    public void statPair(HashMap<GenePair, GenePairQuality> genePairMap){
        if (rootNode == null || rootNode.feature == null)
            return;
        TreeNode child = null;
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

    public boolean useAllFeatures(){
//        if (usedFeatures.size() != featureList.size()){
////            usedFeatures.removeAll(featureList);
//            System.out.println("Oi gioi dat thanh than oi! " + usedFeatures.size() + " " + featureList.size());
//        }
        //return (usedFeatures.size() == featureList.size());
        return true;
    }

    public boolean isEmpty() {
        if (rootNode == null) {
            return true;
        } else {
            return false;
        }
    }

    private void insertIntoTreeNodeMap(HashMap<Integer,Set<TreeNode>> _treeNodeMap, TreeNode treeNode){
        if (treeNode.feature != null){
            Set<TreeNode> treeNodeList = _treeNodeMap.get(treeNode.feature.id);
            if (treeNodeList != null){
                treeNodeList.add(treeNode);
            } else {
                treeNodeList = new HashSet<TreeNode>() {};
                treeNodeList.add(treeNode);
                _treeNodeMap.put(treeNode.feature.id, treeNodeList);
            }            
        }
    }

    public TreeNode cloneBranch(TreeNode branch, TreeNode parent, HashMap<Integer,Set<TreeNode>> _treeNodeMap){
        if (branch == null)
            return null;
        TreeNode newNode = new TreeNode(branch);
        insertIntoTreeNodeMap(_treeNodeMap, newNode);
        newNode.parent = parent;
        if (branch.trueChild != null)
            newNode.trueChild = cloneBranch(branch.trueChild, newNode, _treeNodeMap);
        if (branch.falseChild != null)
            newNode.falseChild = cloneBranch(branch.falseChild, newNode, _treeNodeMap);
        return newNode;
    }

    // simplify the tree to get a simple version of it for testing
    public SimpleTreeNode simplifyTree(){
        if (rootNode == null){
            SimpleTreeNode newRoot = new SimpleTreeNode(classLabel);
            return newRoot;
        }
        return simplifyBranch(rootNode);
    }

    // simplify the branch to get a simple version of it for testing
    private SimpleTreeNode simplifyBranch(TreeNode branch){
        if (branch == null)
            return null;
        if (branch.feature == null){            
            SimpleTreeNode newNode = new SimpleTreeNode(branch.classLabel);
            return newNode;
        } else {
            SimpleTreeNode newNode = new SimpleTreeNode(branch.feature.label);
            newNode.setChild(simplifyBranch(branch.trueChild), simplifyBranch(branch.falseChild));
            return newNode;
        }
    }

    // add a new feature to the tree
    // return the new quality
    public double addFeature(Node newFeature, Set<Integer> sampleList){
        featureList.add(newFeature);
        usedFeatures.clear();
        unclassified.clear();
        if (rootNode == null){
            noCorrect = buildDecTree(sampleList);
        } else {
            treeNodeMap.clear();
            noCorrect = checkGainAdd(newFeature, rootNode, sampleList);
        }
        checkUnclassified(sampleList);
        return noCorrect;
    }

    // remove a feature from the tree
    // return the new quality
    public double removeFeature(Node removedFeature, Set<Integer> sampleList){
        featureList.remove(removedFeature);
        usedFeatures.clear();
        unclassified.clear();
        treeNodeMap.clear();
        
        if (featureList.isEmpty()){            
            unclassified.addAll(sampleList);
            rootNode = null;
            noCorrect = getNoCorrect(sampleList);
            if (noCorrect < 0){
                classLabel = false;
                noCorrect = -noCorrect;
            } else {
                classLabel = true;
            }
        }
        else {
            noCorrect = checkGainRemove(removedFeature, rootNode, sampleList);
        }
        checkUnclassified(sampleList);
        return noCorrect;
    }

    private void checkUnclassified(Set<Integer> sampleList) {
		// TODO Auto-generated method stub
		if(unclassified.isEmpty()) {
			unclassified.addAll(sampleList);
		}
	}

	private static double Info(Set<Integer> sampleIndex, boolean[][] _storeFile) {
        int class0 = 0;
        int class1 = 0;
        double p0;
        double p1;
        double info = 0.0;
        for (Integer i : sampleIndex) {
            if (_storeFile[i][0]) {
                class1++;
            } else {
                class0++;
            }
        }
        p0 = (double) class0 / (double) (class1 + class0);
        p1 = (double) class1 / (double) (class1 + class0);
        if (p0 != 0.0 && p1 != 0.0) {
            info = -(p0 * (Math.log(p0) / Math.log(2)) + p1 * (Math.log(p1) / Math.log(2)));
        } else if (p0 != 0.0 && p1 == 0.0) {
            info = -(p0 * (Math.log(p0) / Math.log(2)));
        } else if (p1 != 0.0 && p0 == 0.0) {
            info = -(p1 * (Math.log(p1) / Math.log(2)));
        } else if (p0 == 0.0 && p1 == 0.0) {
            info = 0.0;
        }

        return info;
    }

    private static double InfoFeature(int featureIndex, Set<Integer> sampleIndex, boolean[][] _storeFile) {
        Set<Integer> Val0 = new HashSet<Integer>();
        Set<Integer> Val1 = new HashSet<Integer>();
        for (Integer i : sampleIndex) {
            if (_storeFile[i][featureIndex]) {
                Val1.add(i);
            } else {
                Val0.add(i);
            }
        }

        if (Val0.isEmpty() || Val1.isEmpty()) {
            return 1;
        } else {
            double infofeature = ((double) Val1.size() / (Val1.size() + Val0.size())) * Info(Val1, _storeFile)
                    + ((double) Val0.size() / (Val1.size() + Val0.size())) * Info(Val0, _storeFile);
            return infofeature;
        }
    }

    public double calcTantativeInfoGain(Node feature, boolean[][] _storeFile){
        if (unclassified.isEmpty())
            return 0.0;
        return Info(unclassified, _storeFile) - InfoFeature(feature.id, unclassified, _storeFile);
    }

    // minhhx
    // choose a feature to split from the _candidateList
    // first time, _candidateList is the whole subgraph
    public Node chooseFeature(Set<Integer> sampleList, Set<Node> _candidateList, boolean[][] _storeFile) {
        double info_m = Info(sampleList, _storeFile);
        Gain = Double.NEGATIVE_INFINITY;
        Node chosenFeature = null;

        // iterate through the list of candidate to choose one with the greatest gain
        for (Node candidate : _candidateList) {
            double featureGain = info_m - InfoFeature(candidate.id, sampleList, _storeFile);
            if (featureGain > Gain) {
                Gain = featureGain;
                chosenFeature = candidate;
            }
        }
/*
        // if gain <= 0.0, check if it worth going forward
        // by calculating the gain for other nodes in the nodes list that are not candidate
        if (Gain <= 0.0){
            Set<Node> futureCandidate = new HashSet<Node>(featureList);
            futureCandidate.removeAll(_candidateList);
            for (Node feature : futureCandidate){
                double featureGain = info_m - InfoFeature(feature.id, sampleList, _storeFile);
                if (featureGain > 0.0) {
                    return chosenFeature;
                }
            }
            return null;
        }
*/
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

    public int calcNoCorrect(Node feature, Set<Integer> sampleList){
        Set<Integer> trueSamples = new HashSet<Integer>();
        Set<Integer> falseSamples = new HashSet<Integer>();
        splitSampleOnFeature(feature.id, sampleList, trueSamples, falseSamples);
        return sampleList.size() - getNoMisclassified(trueSamples) - getNoMisclassified(falseSamples);
    }

//    public int getNoCorrect(Node feature){
//        TreeNode treeNode = treeNodeMap.get(feature.id);
//        if (treeNode == null){
//            System.out.println("Loi con me no roi!");
//            return 0;
//        }
//        return treeNode.noCorrect;
//    }

    public double getInfoGainRemoved(Node feature){
        Set<TreeNode> treeNodeList = treeNodeMap.get(feature.id);
        if (treeNodeList != null){
            double _gain = 0.0;
            for (TreeNode n : treeNodeList){
                _gain += n.infoGain;
                //_gain += n.noCorrect;
            }
            return _gain;
        }
        return 0.0;
    }

    // build the whole decision tree given a subgraph
    // output: return the correct number of the subtree
    private int buildDecTree(Set<Integer> sampleList) {
        treeNodeMap.clear();

        // choose a feature from the entire subgraphNodeList
        Node chosenFeature = chooseFeature(sampleList, featureList, storeFile);

        // get the majority class in the sampleList
        int _noCorrect = getNoCorrect(sampleList);

        // if all samples are in the same class or no more feature to use, no need to spit
        if (Math.abs(_noCorrect) == sampleList.size() || chosenFeature == null) {            
            if (_noCorrect < 0){
                classLabel = false;
                noCorrect = -_noCorrect;
            } else {
                classLabel = true;
                noCorrect = _noCorrect;
            }
            if (noCorrect != sampleList.size())
                unclassified.addAll(sampleList);
            return noCorrect;
        }

        // otherwise, build a tree from scratch
        rootNode = new TreeNode(chosenFeature, null);
        rootNode.infoGain = Gain;   //storing the information gain associated with the root
        usedFeatures.add(chosenFeature);
        insertIntoTreeNodeMap(treeNodeMap, rootNode);

        Set<Integer> trueSamples = new HashSet<Integer>();
        Set<Integer> falseSamples = new HashSet<Integer>();

        // split the sample list
        splitSampleOnFeature(chosenFeature.id, sampleList, trueSamples, falseSamples);
        rootNode.sampleSetTrue = trueSamples;
        rootNode.sampleSetFalse = falseSamples;

        // update the current candidate node list
        Set<Node> candidates = rootNode.updateCandidateFeatures(new HashSet<Node>(), featureList);

        _noCorrect = 0;

        // continue to spit node
        _noCorrect += splitTreeNode(rootNode, candidates, true);
        _noCorrect += splitTreeNode(rootNode, candidates, false);

        rootNode.noCorrect = _noCorrect;
        return _noCorrect;
    }

    // build the decision tree rooted at currentNode, given the curCandidate set
    // choose a new feature to split
    // output: return the no of correct samples of the subtree
    private int splitTreeNode(TreeNode currentNode, Set<Node> curCandidates, boolean isTrueChild) {
        Set<Integer> sampleList = null;
        if (isTrueChild) {
            sampleList = currentNode.sampleSetTrue;
        } else {
            sampleList = currentNode.sampleSetFalse;
        }

        Node chosenFeature = chooseFeature(sampleList, curCandidates, storeFile);

        // if all samples are in the same class or no more feature to use, no need to spit
        if ( chosenFeature == null) {
            TreeNode newNode = new TreeNode(null, currentNode);
            // the change made
        	int _noCorrect = update_unclassified(sampleList);
            
            if (_noCorrect < 0){
                newNode.noCorrect = -_noCorrect;
                newNode.classLabel = false;
            } else {
                newNode.noCorrect = _noCorrect;
                newNode.classLabel = true;
            }
            currentNode.insertChild(newNode, isTrueChild);
            if (newNode.noCorrect != sampleList.size())
                unclassified.addAll(sampleList);
            return newNode.noCorrect;
        }

        // get the majority class in the sampleList
        int _noCorrect = getNoCorrect(sampleList);

        if(Math.abs(_noCorrect) == sampleList.size()) {
            TreeNode newNode = new TreeNode(null, currentNode);
 
            if (_noCorrect < 0){
                newNode.noCorrect = -_noCorrect;
                newNode.classLabel = false;
            } else {
                newNode.noCorrect = _noCorrect;
                newNode.classLabel = true;
            }
            currentNode.insertChild(newNode, isTrueChild);
            
            return newNode.noCorrect;
         }
        // other wise keep splitting
        Set<Integer> trueSamples = new HashSet<Integer>();
        Set<Integer> falseSamples = new HashSet<Integer>();

        // split the sample list
        splitSampleOnFeature(chosenFeature.id, sampleList, trueSamples, falseSamples);

        TreeNode newNode = new TreeNode(chosenFeature, currentNode);
        newNode.infoGain = Gain;                    //storing the information gain associated with the other nodes
        usedFeatures.add(chosenFeature);
        insertIntoTreeNodeMap(treeNodeMap, newNode);

        // insert child into current node
        currentNode.insertChild(newNode, isTrueChild);

        // find the new candidate node list
        Set<Node> candidates = newNode.updateCandidateFeatures(curCandidates, featureList);

        _noCorrect = 0;      

        // continue to spit node
        if (!trueSamples.isEmpty()) {
            newNode.sampleSetTrue = trueSamples;            
            _noCorrect += splitTreeNode(newNode, candidates, true);
        }

        if (!falseSamples.isEmpty()) {
            newNode.sampleSetFalse = falseSamples;
            _noCorrect += splitTreeNode(newNode, candidates, false);
        }
        newNode.noCorrect = _noCorrect;
        return _noCorrect;
    }

    private int update_unclassified(Set<Integer> sampleList) {
		// TODO Auto-generated method stub
        int noLabelTrue = 0;
        Set<Integer> true_samples, false_samples;
        
        true_samples = new HashSet<Integer>();
        false_samples = new HashSet<Integer>();
        
        for (Integer i : sampleList) {
            if (storeFile[i][0]) {
                noLabelTrue++;
                true_samples.add(i);
            } else {
            	false_samples.add(i);
            }
        }
        int noLabelFalse = sampleList.size() - noLabelTrue;
        if (noLabelTrue >= noLabelFalse) {
        	unclassified.addAll(false_samples);
            return noLabelTrue;	
        } else {
         	unclassified.addAll(true_samples);  
            return noLabelFalse;
    	}
   }

    // in case a new feature is added to the subgraph
    // check each node if the new node is a candidate and whether the gain is higher than the current feature
    // if yes, replace the current feature with the new feature and rebuild the subtree rooted there
    // input :
    //      newFeature: the new feature to be added (new added Node)
    //      currentNode: the current node to be examine in the tree
    //      sampleList: the list of sample to be divided in the current Node
    //                  for root node sampleList is all samples
    //      _subgraphNodeList: list of nodes in the new subgraph, inluding the new feature
    // output: return the correct sample number of the subtree
    private int checkGainAdd(Node newFeature, TreeNode currentNode,
            Set<Integer> sampleList) {
        if (currentNode == null) {
            return 0;
        }
        
        // if toCompareNode is leaf
        // and sample list has already been perfectly divided
        // do nothing
        if (currentNode.feature == null && currentNode.noCorrect == sampleList.size() ) {
            return currentNode.noCorrect;
        }

        // calc info gain for addnode
        double nodeGain = Info(sampleList, storeFile) - InfoFeature(newFeature.id, sampleList, storeFile); //gain of this node

        // if
        //      for internal node: information gain by newFeature is greater than toCompareNode
        // or   for leaf node that has not been perfect
        // then replace toCompareNode info with the info of the newFeature
        // and rebuild the decision subtree rooted at toCompareNode
        if (nodeGain > currentNode.infoGain || currentNode.feature == null) {
            if (currentNode.parent == null) {    // current node is root node
                // current is root node, rebuild the whole tree
                return buildDecTree(sampleList);
            } else {
                // find all features that has already been used up in the tree
                Set<Node> previousUsedFeatures = new HashSet<Node>();
                currentNode.getpreviousUsedFeatures(previousUsedFeatures);

                // compute the new candidate list of the parent of current
                Set<Node> candidates = new HashSet<Node>();
                for (Node n : previousUsedFeatures) {
                    candidates.addAll(n.neighbors);
                }
                candidates.retainAll(featureList);

                // if newFeature is in the candidate list
                // then replace toCompareNode by newFeature
                // otherwise, cascade to children
                if (candidates.contains(newFeature)) {
                    // add newFeature to the usedFeatures list
                    usedFeatures.add(newFeature);
                    insertIntoTreeNodeMap(treeNodeMap, currentNode);

                    candidates.addAll(newFeature.neighbors);
                    candidates.retainAll(featureList);

                    currentNode.falseChild = null;
                    currentNode.trueChild = null;
                    currentNode.feature = newFeature;
                    currentNode.infoGain = nodeGain;
                    currentNode.noCorrect = 0;

                    // split the sample set
                    Set<Integer> trueSamples = new HashSet<Integer>();
                    Set<Integer> falseSamples = new HashSet<Integer>();

                    // split the sample list
                    splitSampleOnFeature(newFeature.id, sampleList, trueSamples, falseSamples);

                    // continue to spit node
                    if (!trueSamples.isEmpty()) {
                        currentNode.sampleSetTrue = trueSamples;
                        currentNode.noCorrect += splitTreeNode(currentNode, candidates, true);
                    }

                    if (!falseSamples.isEmpty()) {
                        currentNode.sampleSetFalse = falseSamples;                        
                        currentNode.noCorrect += splitTreeNode(currentNode, candidates, false);
                    }
                    return currentNode.noCorrect;
                }
            }
        }

        // hasn't used new feature yet
        // add feature on currentNode to used feature list
        if (currentNode.feature != null){
            usedFeatures.add(currentNode.feature);
            insertIntoTreeNodeMap(treeNodeMap, currentNode);
        }

        currentNode.noCorrect = 0;
        // cascade to children
        if (currentNode.trueChild != null) {
            currentNode.noCorrect += checkGainAdd(newFeature, currentNode.trueChild, currentNode.sampleSetTrue);
        }
        if (currentNode.falseChild != null) {
            currentNode.noCorrect += checkGainAdd(newFeature, currentNode.falseChild, currentNode.sampleSetFalse);
        }
        return currentNode.noCorrect;
    }

    // in case a feature is removed from the decision tree
    // traverse through the tree to find where that feature was used
    // if found, then remove it and rebuild the subtree
    // input:
    //      removedFeature: the feature to be removed from the tree
    //      currentNode:    the current Node to be examined
    //      sampleList:     list of sample to be divided in currentNode
    //      _subgraphNodeList: the node list of the subgraph without removedFeature
    // output: return the correct sample number of the subtree
    private int checkGainRemove(Node removedFeature, TreeNode currentNode,
            Set<Integer> sampleList) {
        if (currentNode == null) {
            return 0;
        }

        if (currentNode.feature == null){
            if (currentNode.noCorrect != sampleList.size())
                unclassified.addAll(sampleList);
            return currentNode.noCorrect;
        }

        // if this is the feature we are looking for
        // then rebuild the subtree rooted here
        if (currentNode.feature.equals(removedFeature)) {
            treeNodeMap.remove(currentNode.feature.id);
            if (currentNode.parent != null) {
                // find all features that has already been used up in the tree
                Set<Node> previousUsedFeatures = new HashSet<Node>();
                currentNode.getpreviousUsedFeatures(previousUsedFeatures);

                // compute the new candidate list of the parent of currentNode
                Set<Node> candidates = new HashSet<Node>();
                for (Node n : previousUsedFeatures) {
                    candidates.addAll(n.neighbors);
                }
                candidates.retainAll(featureList);

                // choose a feature and split this node again
                TreeNode parent = currentNode.parent;
                int _noCorrect = 0;
                if (currentNode == parent.trueChild) {
                    parent.trueChild = null;
                    _noCorrect = splitTreeNode(parent, candidates, true);
                } else {
                    parent.falseChild = null;
                    _noCorrect = splitTreeNode(parent, candidates, false);
                }
                return _noCorrect;
            } else {    // current is root node, rebuild the whole tree
                return buildDecTree(sampleList);
            }
        } else {       // keep looking in two child
            // add currentNode feature to the usedFeatures list
            usedFeatures.add(currentNode.feature);
            insertIntoTreeNodeMap(treeNodeMap, currentNode);

            currentNode.noCorrect = 0;
            // cascade to children
            if (currentNode.trueChild != null) {
                currentNode.noCorrect += checkGainRemove(removedFeature, currentNode.trueChild, currentNode.sampleSetTrue);
            }
            if (currentNode.falseChild != null) {
                currentNode.noCorrect += checkGainRemove(removedFeature, currentNode.falseChild, currentNode.sampleSetFalse);
            }
            return currentNode.noCorrect;
        }
    }

    // given a sample set
    // return the number of correctly classified samples
    // which belongs to the majority class
    // return positive if class is true, otherwise negative
    private int getNoCorrect(Set<Integer> sampleList){
        int noLabelTrue = 0;
        for (Integer i : sampleList) {
            if (storeFile[i][0]) {
                noLabelTrue++;
            }
        }
        int noLabelFalse = sampleList.size() - noLabelTrue;
        return (noLabelTrue >= noLabelFalse ? noLabelTrue : -noLabelFalse);
    }

    // given a sample set
    // return the number of misclassified samples
    // return value can be used to determine the label of the whole node, which is the label of the majority samples
    // in other word, the majority class is the sign of the return value
    // if return > 0 --> label true. Otherwise --> label false
    private int getNoMisclassified(Set<Integer> sampleList) {
        int noLabelTrue = 0;
        for (Integer i : sampleList) {
            if (storeFile[i][0]) {
                noLabelTrue++;
            }
        }
        int noLabelFalse = sampleList.size() - noLabelTrue;
        return (noLabelTrue >= noLabelFalse ? noLabelFalse : noLabelTrue);
    }
    
    //----- in case the feature List is empty
    // the sample list would be the whole data set
    public int getMisclassifiedList(Set<Integer> _misList) {
        Set<Integer> trueList = new HashSet<Integer>();
        Set<Integer> falseList = new HashSet<Integer>();
        for (int i = 0; i< storeFile.length; i++) {
            if (storeFile[i][0]) {
                trueList.add(i);
            } else
                falseList.add(i);
        }

        if (trueList.size() < falseList.size()){
            _misList.addAll(trueList);
        } else {
            _misList.addAll(falseList);
        }
        return _misList.size();
    }

    private int getMisclassifiedList(Set<Integer> sampleList, Set<Integer> _misList) {
        Set<Integer> trueList = new HashSet<Integer>();
        Set<Integer> falseList = new HashSet<Integer>();

        for (Integer i : sampleList) {
            if (storeFile[i][0]) {
                trueList.add(i);
            } else
                falseList.add(i);
        }
        if (trueList.size() < falseList.size()){
            _misList.addAll(trueList);
        } else {
            _misList.addAll(falseList);
        }
        return _misList.size();
    }

    public int noCorect(){
        return noCorrect;
    }

    public double accuracy(){
        return (double)noCorrect/storeFile.length;
    }

    // test
    public void InOrderTraversal(TreeNode node) {
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

    private void PreOrderTraversal(TreeNode node) {
        if (node != null) {
            if (node.feature != null) {
                System.out.println(node.feature.label + "\tgain: " + node.infoGain);
                System.out.print("left: ");
                PreOrderTraversal(node.trueChild);
                System.out.print("right: ");
                PreOrderTraversal(node.falseChild);
            } else {    // leaf node
                System.out.println("Correct : " + node.noCorrect);
            }
        }
    }
}
