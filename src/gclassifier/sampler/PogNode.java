/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.sampler;

import gclassifier.Node;
import gclassifier.SubGraphFeature;
import gclassifier.Tree;
import gclassifier.TreeQualifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import libsvm.SVMHelper;

/**
 *
 * @author minhhx
 */
public class PogNode {

    public Set<Node> nodeList;
    private Set<Node> addableList;
    private Set<Node> removableList;
    private int noEdges;
    public double infoDensity; // the percentage of correctly classified samples
    private double[] addWeight;
    private double[] removeWeight;
    private HashMap<Node, Integer> candidateMapAddRemove;
    private Node[] candidateArrayAdd;
    private Node[] candidateArrayRemove;
    private Node lastEditedNode; // the last edited Node when choosing a neighbor, could be either an addable or removable node
    private Node candidateEditedNode; // the edited Node when choosing a neighbor, could become lastEditedNode later if the state is accepted
    public boolean isNotMinimal; // signal if a deletion can be made without reducing the potential

    public PogNode(Set<Node> _nodeList, double _infoDensity, TreeQualifier qualifier) {
        isNotMinimal = false;
        nodeList = _nodeList;
        lastEditedNode = null;
        // calculate addable, removable list, potential score simultaneously
        prepareInfo(qualifier);
        infoDensity = _infoDensity;
        calcTransitWeight(qualifier);
    }

    public PogNode(Set<Node> _nodeList, Set<Node> _addableNodes,
            int _noEdges, double _infoDensity, TreeQualifier qualifier) {
        isNotMinimal = false;
        nodeList = _nodeList;
        lastEditedNode = null;
        addableList = filterAddableList(_addableNodes, qualifier);
        calcRemovableList();
        noEdges = _noEdges;

        infoDensity = _infoDensity;
        calcTransitWeight(qualifier);
    }

    public PogNode(Set<Node> _nodeList, double _infoDensity) {
        isNotMinimal = false;
        nodeList = new HashSet<Node>(_nodeList);
        lastEditedNode = null;
        infoDensity = _infoDensity;
    }

    public void updateLastEditedNode() {
        lastEditedNode = candidateEditedNode;
    }

    public void updatePogNode(Set<Node> _nodeList, double _infoDensity) {
        nodeList = new HashSet<Node>(_nodeList);
        infoDensity = _infoDensity;
    }

    public Set<Node> nodeList() {
        return nodeList;
    }

    public double getTransitProb(PogNode toPogNode) {
        if (size() == 0){
            return 1.0;
        }
        Node editedFeature = null;
        if (toPogNode.size() == size() - 1) {
            Set<Node> different = new HashSet<Node>(nodeList);
            different.removeAll(toPogNode.nodeList);
            editedFeature = different.iterator().next();
            Integer index = candidateMapAddRemove.get(editedFeature);
            if (index == null) {
                return 0.0;
            }
            return removeWeight[index];
        } else if (toPogNode.size() == size() + 1) {
            Set<Node> different = new HashSet<Node>(toPogNode.nodeList);
            different.removeAll(nodeList);
            editedFeature = different.iterator().next();
            Integer index = candidateMapAddRemove.get(editedFeature);
            if (index == null) {
                return 0.0;
            }
            return addWeight[index];
        } else {
            return 0.0;
        }
    }

    // get transit prob given the pilot distribution
    // the transit prob only depends on the target node
    // inProb: the prob to favor the boundary zone
    public double getTransitProb_Pilot(PogNode target, double inProb) {
        int size = size(); // size of current/source subgraph
        int noIn = 0, noOut = 0; // number of neighbors in and out of the desired zone (boundary)
        if (Tree.inZone(size - 1)) {
            noIn += getNoNeighborDown();
        } else {
            noOut += getNoNeighborDown();
        }
        if (Tree.inZone(size + 1)) {
            noIn += getNoNeighborUp();
        } else {
            noOut += getNoNeighborUp();
        }

        if (Tree.inZone(target.size())) {
            return inProb / noIn;
        } else {
            return (1.0 - inProb) / noOut;
        }
    }

    public int getNoNeighborUp() {
        return addableList.size();
    }

    public int getNoNeighborDown() {
        return removableList.size();
    }

    private void calcTransitWeight(TreeQualifier qualifier) {
        if (MetroSampler.useSVM) {
            calcTransitWeight_SVM(qualifier);
        } else if (MetroSampler.useFastVersion) {
            calcTransitWeight_Fast(qualifier);
        } else if (MetroSampler.useGamma) {
            calcTransitWeight_Gamma(qualifier);
        } else if(MetroSampler.useFastVersionDivRank) {
        	calcTransitWeight_Fast_DivRank(qualifier);
        } else {
            calcTransitWeight_NoGamma(qualifier);
        }
    }

    private void calcTransitWeight_NoGamma(TreeQualifier qualifier) {
        candidateMapAddRemove = new HashMap<Node, Integer>();
        candidateArrayAdd = new Node[addableList.size()];
        candidateArrayRemove = new Node[removableList.size()];
        addWeight = new double[addableList.size()];
        removeWeight = new double[removableList.size()];

        double weightSum = 0.0;
        int count = 0;
        double weightSum1 = 0.0;
        double weightSum2 = 0.0;
        double infoGain = 0.0;

        for (Node n : addableList) {
            // weight of a addable node is proportional to the infomation gain
            // on the unclassified list
            double gainAdded = qualifier.getInfoGainAdded(n);
//            if (gainAdded < 0.0){
//                System.out.print('+');
//            } 
            infoGain = Math.max(gainAdded, 0.0);
            addWeight[count] = infoGain;
            candidateMapAddRemove.put(n, count);
            candidateArrayAdd[count] = n;
            weightSum1 += infoGain;

            count++;
        }

        count = 0;
        for (Node n : removableList) {
            // weight of a removable node is reversely proportional to info gain of that node
            double gainRemoved = qualifier.getInfoGainRemoved(n);
//            if (gainRemoved < 0.0){
//                System.out.print('*');
//            } else if (gainRemoved > 0.0){
//                
//            } else {
//                System.out.print('@');
//            }
            infoGain = 1.0 / (Math.max(0.0, gainRemoved) + 0.01);
            removeWeight[count] = infoGain;
            candidateMapAddRemove.put(n, count);
            candidateArrayRemove[count] = n;
            weightSum2 += infoGain;
            count++;
        }

        if (weightSum1 == 0.0) {
            double uniform = 1.0 / addWeight.length;
            for (int i = 0; i < addWeight.length; i++) {
                addWeight[i] = uniform;
            }
        } else {
            for (int i = 0; i < addWeight.length; i++) {
                addWeight[i] /= weightSum1;
            }
        }

        if (weightSum2 == 0.0) {
            double uniform = 1.0 / removeWeight.length;
            for (int i = 0; i < removeWeight.length; i++) {
                removeWeight[i] = uniform;
            }
        } else {
            for (int i = 0; i < removeWeight.length; i++) {
                removeWeight[i] /= weightSum2;
            }
        }
    }

    // calc the transit weight for the new formula
    // using gamma: the prob to move toward border
    private void calcTransitWeight_Gamma(TreeQualifier qualifier) {
        candidateMapAddRemove = new HashMap<Node, Integer>();
        candidateArrayAdd = new Node[addableList.size()];
        candidateArrayRemove = new Node[removableList.size()];
        addWeight = new double[addableList.size()];
        removeWeight = new double[removableList.size()];

        double gamma = MetroSampler.calcLeavingBorderProb(this);

        // set of index in the addWeight and removeWeight array for good and bad candidate
        Set<Integer> goodAddCandidate = new HashSet<Integer>();
        Set<Integer> badAddCandidate = new HashSet<Integer>();
        Set<Integer> goodRemoveCandidate = new HashSet<Integer>();
        Set<Integer> badRemoveCandidate = new HashSet<Integer>();

        // the denominators in the formula to calc the normalized weight
        double sumBadAddCandidate = 0, sumGoodAddCandidate = 0,
                sumBadRemoveCandidate = 0, sumGoodRemoveCandidate = 0;

        int count = 0;
        double infoGain = 0.0;

        for (Node n : addableList) {
            // weight of a addable node is proportional to the infomation gain
            // on the unclassified list
            double gainAdded = qualifier.getInfoGainAdded(n);
            if (gainAdded > 0.0) { // good add
                infoGain = gainAdded;
                sumGoodAddCandidate += infoGain;
                goodAddCandidate.add(count);
            } else { // bad add
                infoGain = 1.0 / (Math.abs(gainAdded) + 0.001);
                //infoGain = 1.0 + gainAdded;
                sumBadAddCandidate += infoGain;
                badAddCandidate.add(count);
            }
            addWeight[count] = infoGain;
            candidateMapAddRemove.put(n, count);
            candidateArrayAdd[count] = n;
            count++;
        }

        count = 0;
        for (Node n : removableList) {
            // weight of a removable node is reversely proportional to info gain of that node
            double gainRemoved = qualifier.getInfoGainRemoved(n);
            if (gainRemoved > 0.0) { // bad remove
                infoGain = 1.0 / gainRemoved;
                sumBadRemoveCandidate += infoGain;
                badRemoveCandidate.add(count);
            } else { // good remove
                infoGain = 0.001 + Math.abs(gainRemoved);
                sumGoodRemoveCandidate += infoGain;
                goodRemoveCandidate.add(count);
            }
            removeWeight[count] = infoGain;
            candidateMapAddRemove.put(n, count);
            candidateArrayRemove[count] = n;
            count++;
        }

        // normalize the weight
        for (Integer index : goodAddCandidate) {
            addWeight[index] = addWeight[index] / sumGoodAddCandidate * (1.0 - gamma);
        }
        for (Integer index : badAddCandidate) {
            addWeight[index] = addWeight[index] / sumBadAddCandidate * gamma;
        }
        for (Integer index : goodRemoveCandidate) {
            removeWeight[index] = removeWeight[index] / sumGoodRemoveCandidate * (1.0 - gamma);
        }
        for (Integer index : badRemoveCandidate) {
            removeWeight[index] = removeWeight[index] / sumBadRemoveCandidate * gamma;
        }

        // check if this subgraph is not minimal
        if (!goodRemoveCandidate.isEmpty() || !badAddCandidate.isEmpty()) {
            isNotMinimal = true;
        }
    }

    private void calcTransitWeight_Fast_DivRank(TreeQualifier qualifier) {
    	
    	calcTransitWeight_Fast(qualifier);
    	
    	int count = 0;
    	double sum = 0;
    	ArrayList<Integer> nodeArrayList = new ArrayList<Integer>();
    	for (Node n : nodeList) {
    		nodeArrayList.add(n.id);
    	}
    	for (Node n: addableList) {
    		int visitCount = getVisitCount(nodeArrayList, n, true);
    		addWeight[count] = visitCount*addWeight[count];
    		sum += addWeight[count];
    		count++;
    	}
    	count=0;
    	for (Node n: removableList) {
    		int visitCount = getVisitCount(nodeArrayList, n, false);
    		removeWeight[count] = visitCount*removeWeight[count];
    		sum += removeWeight[count];
    		count++;
    	}
    	for (int i=0; i<addWeight.length; i++) {
    		addWeight[i] /= sum;
    	}
    	for (int i=0; i<removeWeight.length; i++) {
    		removeWeight[i] /= sum;
    	}
    }
    private int getVisitCount(ArrayList<Integer> nodeArrayList, Node n, boolean add) {
		// TODO Auto-generated method stub
    	if (n != null)
			if(add)
				nodeArrayList.add(n.id);
			else
				nodeArrayList.remove((Integer)n.id);
		Collections.sort(nodeArrayList);
		if (MetroSampler.visit_count.containsKey(nodeArrayList)) {
			return MetroSampler.visit_count.get(nodeArrayList);
		}
		if (n != null)
			if(!add)
				nodeArrayList.add(n.id);
			else
				nodeArrayList.remove((Integer)n.id);
		
		return 1;
	}

    private void updatePotential(double potential) {
    	ArrayList<Integer> nodeArrayList = new ArrayList<Integer>();
		for (Node n_iter : nodeList) {
			nodeArrayList.add(n_iter.id);
		}
		Collections.sort(nodeArrayList);
		Integer count = 0;
		if (MetroSampler.visit_count.containsKey(nodeArrayList)) {
			count = MetroSampler.visit_count.get(nodeArrayList);
		}
		
		count++;
		MetroSampler.visit_count.put(nodeArrayList, (Integer)count);
		MetroSampler.nodePotential.put(nodeArrayList, potential);
		MetroSampler.featureQueue.add(new SubGraphFeature(nodeArrayList, potential));
    }
    // calc the transit weight for the new formula
    // 
    private void calcTransitWeight_Fast(TreeQualifier qualifier) {
        candidateMapAddRemove = new HashMap<Node, Integer>();
        candidateArrayAdd = new Node[addableList.size()];
        candidateArrayRemove = new Node[removableList.size()];
        addWeight = new double[addableList.size()];
        removeWeight = new double[removableList.size()];       

        // set of index in the addWeight and removeWeight array for good and bad candidate
        Set<Integer> goodAddCandidate = new HashSet<Integer>();
        Set<Integer> badAddCandidate = new HashSet<Integer>();
        
        // the denominators in the formula to calc the normalized weight
        double sumBadAddCandidate = 0, sumGoodAddCandidate = 0, sumRemoveCandidate = 0;

        int count = 0;
        double infoGain = 0.0;

        for (Node n : addableList) {
            // weight of a addable node is proportional to the infomation gain
            // on the unclassified list
            double gainAdded = qualifier.getInfoGainAdded(n);
            if (gainAdded > 0.0) { // good add
                infoGain = gainAdded;
                sumGoodAddCandidate += infoGain;
                goodAddCandidate.add(count);
            } else { // bad add
                infoGain = 1.0 / (Math.abs(gainAdded) + 0.001);
                sumBadAddCandidate += infoGain;
                badAddCandidate.add(count);                
            }
            addWeight[count] = infoGain;
            candidateMapAddRemove.put(n, count);
            candidateArrayAdd[count] = n;
            count++;
        }

        count = 0;
        for (Node n : removableList) {
            // weight of a removable node is reversely proportional to info gain of that node
            infoGain = 1.0 / removableList.size();
            sumRemoveCandidate += infoGain;
            removeWeight[count] = infoGain;
            candidateMapAddRemove.put(n, count);
            candidateArrayRemove[count] = n;
            count++;
        }

        // normalize the weight for each type of edit       
        for (Integer index : goodAddCandidate) {
            addWeight[index] = addWeight[index] / sumGoodAddCandidate;
        }
        for (Integer index : badAddCandidate) {
            addWeight[index] = addWeight[index] / sumBadAddCandidate;
        }
        for (int index = 0; index < removeWeight.length; index++) {
            removeWeight[index] = removeWeight[index] / sumRemoveCandidate;
        }
        
        // normalize so that sum of all prob is 1.
        if (!goodAddCandidate.isEmpty() || !removableList.isEmpty()) {
            double badAddProb =  1.0 / qualifier.entireNodeList().size() * badAddCandidate.size();           
            double leftProb = 1.0;
            if (!badAddCandidate.isEmpty()) {                
                for (Integer index : badAddCandidate) {
                    addWeight[index] = addWeight[index] * badAddProb;
                }
                leftProb -= badAddProb;
            }       
            if (!goodAddCandidate.isEmpty() && !removableList.isEmpty()) {
                double lowerBeta = (double)removableList.size()/getNoNeighborUp();
                double beta = MetroSampler.calcDeletionProb(this) * (1-lowerBeta) + lowerBeta;
                beta *= leftProb;
                for (int index = 0; index < removeWeight.length; index++) {
                    removeWeight[index] = removeWeight[index] * beta;
                }
                double goodAddProb = 1.0 - beta - badAddProb;
                
                for (Integer index : goodAddCandidate) {
                    addWeight[index] = addWeight[index] * goodAddProb;
                }
                
            } else if (!goodAddCandidate.isEmpty()) {
                for (Integer index : goodAddCandidate) {
                    addWeight[index] = addWeight[index] * leftProb;
                }
            } else {
                for (int index = 0; index < removeWeight.length; index++) {
                    removeWeight[index] = removeWeight[index] * leftProb;
                }
            }
        }
        
    }     

    // calc the transit weight using weights from SVM
    private void calcTransitWeight_SVM(TreeQualifier qualifier) {
        candidateMapAddRemove = new HashMap<Node, Integer>();
        candidateArrayAdd = new Node[addableList.size()];
        candidateArrayRemove = new Node[removableList.size()];
        addWeight = new double[addableList.size()];
        removeWeight = new double[removableList.size()];

        int count = 0;
        double weightSum1 = 0.0;
        double weightSum2 = 0.0;

        // weight of an addable node is proportional to the weight in the SVM model 
        // built upon (current subgraph + addable nodes)
        Set<Node> addedSubgraph = new HashSet<Node>();
        addedSubgraph.addAll(nodeList);
        addedSubgraph.addAll(addableList);
        SVMHelper svmhelper = new SVMHelper(qualifier.getStoreFile(), addedSubgraph);
        HashMap<Integer, Double> svmWeights = svmhelper.getWeights(addableList);
        for (Node n : addableList) {
            addWeight[count] = svmWeights.get(n.id);
            candidateMapAddRemove.put(n, count);
            candidateArrayAdd[count] = n;
            weightSum1 += addWeight[count];
            count++;
        }

        // remove weight = 1 / (weight of node in the current svm model)
        svmWeights = qualifier.getCurrentSVM().getWeights(removableList);
        count = 0;
        for (Node n : removableList) {
            removeWeight[count] = 1.0 / (svmWeights.get(n.id) + 0.001);
            candidateMapAddRemove.put(n, count);
            candidateArrayRemove[count] = n;
            weightSum2 += removeWeight[count];
            count++;
        }

        if (weightSum1 == 0.0) {
            double uniform = 1.0 / addWeight.length;
            for (int i = 0; i < addWeight.length; i++) {
                addWeight[i] = uniform;
            }
        } else {
            for (int i = 0; i < addWeight.length; i++) {
                addWeight[i] /= weightSum1;
            }
        }

        if (weightSum2 == 0.0) {
            double uniform = 1.0 / removeWeight.length;
            for (int i = 0; i < removeWeight.length; i++) {
                removeWeight[i] = uniform;
            }
        } else {
            for (int i = 0; i < removeWeight.length; i++) {
                removeWeight[i] /= weightSum2;
            }
        }
                
        if (!addableList.isEmpty() && !removableList.isEmpty()){
            double lowerBeta = (double)removableList.size()/getNoNeighborUp();
            double beta = MetroSampler.calcDeletionProb(this) * (1-lowerBeta) + lowerBeta;
            
            for (int i = 0; i < removeWeight.length; i++) {
                removeWeight[i] = removeWeight[i] * beta;
            }
            
            for (int i = 0; i < addWeight.length; i++) {
                addWeight[i] = addWeight[i] * (1.0 - beta);
            }
        }
    }

    // get the potential v(G) for the subgraph
    // if subgraph is not minimal, potential is epsilon (very small)
    // otherwise, potential = infoDensity
    public double getPotential() {
        if (isNotMinimal) {
            return MetroSampler.epsilon * infoDensity;
            //return infoDensity;
        } //return MetroSampler.epsilon;
        else {
            return infoDensity;
        }
        //return 1;
    }

    // get the potential v(G) for the subgraph
    // this is a rough estimate, always return the accuracy
    // then, based on additional information, the potential can be updated to be more accurate
    public double getPotential_Fast() {
        return infoDensity;
    }

    // get the potential given the pilot distribution
    public double getPotential_Pilot() {
        if (Tree.inZone(this)) {
            return infoDensity;
        } else {
            return MetroSampler.epsilon;
        }
    }

    public int size() {
        return nodeList.size();
    }

    // given a new node to be added
    // recalculate additional info of the graph, including
    // + addable node list
    // + no of Edges in the graph
    // + potential score
    private PogNode recalcInfoAdded(Node editedNode, TreeQualifier qualifier) {
        Set<Node> newNodeList = new HashSet<Node>(nodeList);
        Set<Node> newAddables = new HashSet<Node>(addableList);

        //-------- recalculate the addable list
        if (nodeList.isEmpty()) {
            newAddables.clear();
        }
        newAddables.addAll(editedNode.neighbors);
        newAddables.removeAll(nodeList);
        newAddables.remove(editedNode);

        //------- recalculate no of edges
        Set<Node> affectedNodes = new HashSet<Node>(editedNode.neighbors);
        affectedNodes.retainAll(nodeList);
        affectedNodes.remove(editedNode);
        int newNoEdges = noEdges + affectedNodes.size();

        //------- newNodeList
        newNodeList.add(editedNode);

        if (newNodeList.size() != nodeList.size() + 1) {
            System.out.println("Do mat day!");
        }

        // try adding the node        
        double _infoDensity = qualifier.updateClassifier(editedNode, true);

        // if all features are used --> connected & maybe maximal
        if (qualifier.useAllFeatures()) {
            return new PogNode(newNodeList, newAddables, newNoEdges,
                    _infoDensity, qualifier);
        } else {  // not maximal && not connected
            qualifier.reverseChange();  // do not sample
            return null;
        }
    }

    private PogNode recalcInfoRemovedDivRank(Node editedNode, TreeQualifier qualifier) {
        Set<Node> newNodeList = new HashSet<Node>(nodeList);
        Set<Node> newAddables = new HashSet<Node>(addableList);

        //----------- new node list
        newNodeList.remove(editedNode);

        if (newNodeList.size() != nodeList.size() - 1) {
            System.out.println("Ouch! Did you really remove or not?");
        }

        //--------- rebuild the addable list && no of Edges
        int newNoEdges = 0;
        if (newNodeList.isEmpty()) {
            //--------- rebuild the addable list
            newAddables.clear();
            newAddables.addAll(qualifier.entireNodeList());
            // no edges
            newNoEdges = 0;
        } else {
            //--------- rebuild the addable list
            Set<Node> affectedAddables = new HashSet<Node>(editedNode.neighbors);
            affectedAddables.retainAll(addableList);

            // nodes in addable that are only connected to the removed node --> become un-addable
            for (Node n : affectedAddables) {
                Set<Node> intersect = new HashSet<Node>(n.neighbors);
                intersect.retainAll(nodeList);

                // if it is only connected to the removed node --> become un-addable
                if (intersect.size() == 1) {
                    newAddables.remove(n);
                }
            }
            // edited node become addable
            newAddables.add(editedNode);

            //---------- update no of Edges
            Set<Node> affectedNodes = new HashSet<Node>(editedNode.neighbors);
            affectedNodes.retainAll(nodeList);
            newNoEdges = noEdges - affectedNodes.size();
        }


        // try adding the node
        
        // if all features are used --> connected & maybe maximal
            return new PogNode(newNodeList, newAddables, newNoEdges,
                    0, qualifier);
        }

    // given a new node to be remove
    // recalculate additional info of the graph, including
    // + addable node list
    // + no of Edges in the graph
    // + potential score
    private PogNode recalcInfoRemoved(Node editedNode, TreeQualifier qualifier) {
        Set<Node> newNodeList = new HashSet<Node>(nodeList);
        Set<Node> newAddables = new HashSet<Node>(addableList);

        //----------- new node list
        newNodeList.remove(editedNode);

        if (newNodeList.size() != nodeList.size() - 1) {
            System.out.println("Ouch! Did you really remove or not?");
        }

        //--------- rebuild the addable list && no of Edges
        int newNoEdges = 0;
        if (newNodeList.isEmpty()) {
            //--------- rebuild the addable list
            newAddables.clear();
            newAddables.addAll(qualifier.entireNodeList());
            // no edges
            newNoEdges = 0;
        } else {
            //--------- rebuild the addable list
            Set<Node> affectedAddables = new HashSet<Node>(editedNode.neighbors);
            affectedAddables.retainAll(addableList);

            // nodes in addable that are only connected to the removed node --> become un-addable
            for (Node n : affectedAddables) {
                Set<Node> intersect = new HashSet<Node>(n.neighbors);
                intersect.retainAll(nodeList);

                // if it is only connected to the removed node --> become un-addable
                if (intersect.size() == 1) {
                    newAddables.remove(n);
                }
            }
            // edited node become addable
            newAddables.add(editedNode);

            //---------- update no of Edges
            Set<Node> affectedNodes = new HashSet<Node>(editedNode.neighbors);
            affectedNodes.retainAll(nodeList);
            newNoEdges = noEdges - affectedNodes.size();
        }


        // try adding the node
        double _infoDensity = qualifier.updateClassifier(editedNode, false);

        // if all features are used --> connected & maybe maximal
        if (qualifier.useAllFeatures()) {
            return new PogNode(newNodeList, newAddables, newNoEdges,
                    _infoDensity, qualifier);
        } else {  // not maximal && not connected
            qualifier.reverseChange();  // do not sample
            return null;
        }
    }

    // keep only subgraphs with info gain >0 in the addablelist
    private Set<Node> filterAddableList(Set<Node> _addableList, TreeQualifier qualifier) {
        if (MetroSampler.limitEditMap) {
            Set<Node> newAddable = new HashSet<Node>();
            for (Node n : _addableList) {
                double infoGain = qualifier.getInfoGainAdded(n);
                if (infoGain < 0.0) {
//                    System.out.print('+');
                } else if (infoGain > 0.0) {
                    newAddable.add(n);
                } else {
//                    System.out.print('-');
                }
            }
            return newAddable;
        } else {
            return _addableList;
        }
    }

    // from scratch,
    // given a new graph calculate all the additional info about it, including
    // + addable node list
    // + removable node list
    // + no of Edges in the graph
    // + potential score
    private void prepareInfo(TreeQualifier qualifier) {
        addableList = new HashSet<Node>();
        noEdges = 0;

        for (Node n : nodeList) {
            addableList.addAll(n.neighbors);
            Set<Node> intersect = new HashSet<Node>(n.neighbors);
            intersect.retainAll(nodeList);
            noEdges += intersect.size();
        }

        addableList.removeAll(nodeList);
        addableList = filterAddableList(addableList, qualifier);
        noEdges /= 2;

        // calc removable list
        calcRemovableList();
    }

    private void calcRemovableList() {
        DFSTree dfsTree = new DFSTree(nodeList);
        removableList = dfsTree.removables;
    }

    // randomly pick a neigbor pattern using uniform distribution
    public PogNode chooseNeighborUniform(TreeQualifier qualifier, Random generator) {
        Node editedNode = null;
        boolean isAdded = false;
        if (addableList.isEmpty() && removableList.isEmpty()) { // choose a node to add from the whole dataset
            isAdded = true;
            Set<Node> entireNodeList = qualifier.entireNodeList();
            int chosenIndex = generator.nextInt(entireNodeList.size());
            int i = 0;
            for (Node n : entireNodeList) {
                if (i++ == chosenIndex) {
                    editedNode = n;
                    break;
                }
            }
        } else { // pick from the candidate set
            int maxRandom = addableList.size() + removableList.size();

            // choose a node to add or remove
            int chosenIndex = generator.nextInt(maxRandom);

            if (chosenIndex < addableList.size()) {
                isAdded = true;
                int i = 0;
                for (Node n : addableList) {
                    if (i++ == chosenIndex) {
                        editedNode = n;
                        break;
                    }
                }
            } else {
                chosenIndex -= addableList.size();
                int i = 0;
                for (Node n : removableList) {
                    if (i++ == chosenIndex) {
                        editedNode = n;
                        break;
                    }
                }
            }
        }

        // create a node with the new infomation
        if (isAdded) {
            return recalcInfoAdded(editedNode, qualifier);
        } else {
            return recalcInfoRemoved(editedNode, qualifier);
        }
    }

    // randomly pick a neigbor pattern using uniform distribution
    // with the appearance of alpha to indicate favor over addition or removal
    public PogNode chooseNeighborUniform2(TreeQualifier qualifier, Random generator, double alpha) {
        Node editedNode = null;
        boolean isAdded = false;
        if (addableList.isEmpty() && removableList.isEmpty()) { // choose a node to add from the whole dataset
            isAdded = true;
            Set<Node> entireNodeList = qualifier.entireNodeList();
            int chosenIndex = generator.nextInt(entireNodeList.size());
            int i = 0;
            for (Node n : entireNodeList) {
                if (i++ == chosenIndex) {
                    editedNode = n;
                    break;
                }
            }
        } else { // pick from the candidate set
            // decide whether add or remove
            if (generator.nextDouble() < alpha && !addableList.isEmpty()) { // add
                int chosenIndex = generator.nextInt(addableList.size());
                isAdded = true;
                int i = 0;
                for (Node n : addableList) {
                    if (i++ == chosenIndex) {
                        editedNode = n;
                        break;
                    }
                }
            } else {    // remove
                int chosenIndex = generator.nextInt(removableList.size());
                int i = 0;
                for (Node n : removableList) {
                    if (i++ == chosenIndex) {
                        editedNode = n;
                        break;
                    }
                }
            }
        }

        // create a node with the new infomation
        if (isAdded) {
            return recalcInfoAdded(editedNode, qualifier);
        } else {
            return recalcInfoRemoved(editedNode, qualifier);
        }
    }

    // randomly pick a neigbor pattern using the weight on edges
    // beta is prob that a vertex is removed from the subgraph
    public PogNode chooseNeighborWeighted(TreeQualifier qualifier, Random generator, double beta) {
        Node editedNode = null;
        boolean isAdded = false;
        if (addableList.isEmpty() && removableList.isEmpty()) { // this is a null node; choose a node to add from the whole dataset
            isAdded = true;
            Set<Node> entireNodeList = qualifier.entireNodeList();
            int chosenIndex = generator.nextInt(entireNodeList.size());
            int i = 0;
            for (Node n : entireNodeList) {
                if (i++ == chosenIndex) {
                    editedNode = n;
                    break;
                }
            }
        } else { // pick from the candidate set
            // decide whether add or remove
            if (generator.nextDouble() > beta && !addableList.isEmpty()) {  // add a vertex to the subgraph   
                int chosenIndex = randomIndex(generator, addWeight);
                isAdded = true;
                editedNode = candidateArrayAdd[chosenIndex];
            } else {    // remove a vertex from the subgraph
                int chosenIndex = randomIndex(generator, removeWeight);
                editedNode = candidateArrayRemove[chosenIndex];
            }
        }

        // create a node with the new infomation
        PogNode q = null;
        if (isAdded) {
            q = recalcInfoAdded(editedNode, qualifier);
        } else {
            q = recalcInfoRemoved(editedNode, qualifier);
        }

        if (q != null) {
            candidateEditedNode = editedNode;
        }
        return q;
    }

    public PogNode chooseNeighborWeighted_Fast_DivRank(TreeQualifier qualifier, Random generator) {
    	PogNode p = chooseNeighborWeighted_Fast(qualifier, generator);
    	double outerSum = 0;
    	double transitProb = 0;
    	double avgNr = 0;
//    	HashMap<Integer, Integer> addAddnodeMap = new HashMap<Integer, Integer>();
//    	HashMap<Integer, Integer> addRemovenodeMap = new HashMap<Integer, Integer>();
//    	HashMap<Integer, Integer> removeAddnodeMap = new HashMap<Integer, Integer>();
//    	HashMap<Integer, Integer> removeRemovenodeMap = new HashMap<Integer, Integer>();
    	//System.out.println("Avg size starting "+avgNr);
//    	int count=0;
//    	boolean nothingAdded = true;
    	for (Node n: p.addableList) {
//    		if(p.addWeight[count++] < 0.01) {
//    			continue;
//    			
//    		}
//    		nothingAdded = false;
    		PogNode pn = p.recalcInfoAddedDivRank(n, qualifier);
//    		avgNr += pn.addableList.size();
//    		avgNr += pn.removableList.size();
//    		double innerSum =0;
    		double wt = getPotential(pn);
//    		int i=0;
//    		for(Node pnn : pn.addableList) {
//    			addAddnodeMap.put(pnn.id, 0);
//    			innerSum += pn.addWeight[i]; //* getVisitCount(pn,pnn);
//    			i++;
//        	}
//    		i=0;
//    		for(Node pnn : pn.removableList) {
//    			addRemovenodeMap.put(pnn.id, 0);
//    			innerSum += pn.removeWeight[i]* getVisitCount(pn,pnn);
//    			i++;
//        	}
			transitProb = pn.getTransitProb(p);
    		//qualifier.reverseChange();
    		outerSum += (transitProb*wt);
    	}
    	for (Node n: p.removableList) {
    		
    		PogNode pn = p.recalcInfoRemovedDivRank(n, qualifier);
//    		avgNr += pn.addableList.size();
//    		avgNr += pn.removableList.size();
//    		double innerSum =0;
    		double wt = getPotential(pn);
//    		int i=0;
//    		for(Node pnn : pn.addableList) {
//    			removeAddnodeMap.put(pnn.id, 0);
//    			innerSum += pn.addWeight[i]* getVisitCount(pn,pnn);
//    			i++;
//        	}
//    		i=0;
//    		for(Node pnn : pn.removableList) {
//    			removeRemovenodeMap.put(pnn.id, 0);
//    			innerSum += pn.removeWeight[i]* getVisitCount(pn,pnn);
//    			i++;
//        	}
//			System.out.println(innerSum + "inner sum");
			transitProb = pn.getTransitProb(p);
    		//qualifier.reverseChange();
    		outerSum += (transitProb*wt);
    	}
    	System.out.println(" Avg size is "+avgNr);
//    	System.out.println( addAddnodeMap.size() +" " + addRemovenodeMap.size()
//    			+" " +removeAddnodeMap.size()+" " +removeRemovenodeMap.size());
    	outerSum *= getVisitCount(p, null);
    	p.updatePotential(outerSum);
    	return p;
    }
    
    private PogNode recalcInfoAddedDivRank(Node editedNode, TreeQualifier qualifier) {
		Set<Node> newNodeList = new HashSet<Node>(nodeList);
        Set<Node> newAddables = new HashSet<Node>(addableList);

        //-------- recalculate the addable list
        if (nodeList.isEmpty()) {
            newAddables.clear();
        }
        newAddables.addAll(editedNode.neighbors);
        newAddables.removeAll(nodeList);
        newAddables.remove(editedNode);

        //------- recalculate no of edges
        Set<Node> affectedNodes = new HashSet<Node>(editedNode.neighbors);
        affectedNodes.retainAll(nodeList);
        affectedNodes.remove(editedNode);
        int newNoEdges = noEdges + affectedNodes.size();

        //------- newNodeList
        newNodeList.add(editedNode);

        if (newNodeList.size() != nodeList.size() + 1) {
            System.out.println("Do mat day!");
        }

        return new PogNode(newNodeList, newAddables, newNoEdges,
                0, qualifier);
	}

	private double getPotential(PogNode pn) {
		// TODO Auto-generated method stub
    	ArrayList<Integer> nodeArrayList = new ArrayList<Integer>();
		for (Node n_iter : pn.nodeList) {
			nodeArrayList.add(n_iter.id);
		}
		Collections.sort(nodeArrayList);
		if (MetroSampler.nodePotential.containsKey(nodeArrayList)) {
			return MetroSampler.nodePotential.get(nodeArrayList);
		}
		return 1;
	}

    private double getVisitCount(PogNode pn, Node n) {
		// TODO Auto-generated method stub
    	ArrayList<Integer> nodeArrayList = new ArrayList<Integer>();
		for (Node n_iter : pn.nodeList) {
			nodeArrayList.add(n_iter.id);
		}
		if(n != null)
			nodeArrayList.add(n.id);
		Collections.sort(nodeArrayList);
		if (MetroSampler.nodePotential.containsKey(nodeArrayList)) {
			return MetroSampler.visit_count.get(nodeArrayList);
		}
		return 1;
	}

	// randomly pick a neigbor pattern using the weight on edges
    // beta is prob that a vertex is removed from the subgraph
    public PogNode chooseNeighborWeighted_Fast(TreeQualifier qualifier, Random generator) {
        Node editedNode = null;
        boolean isAdded = false;
        if (addableList.isEmpty() && removableList.isEmpty()) { // this is a null node; choose a node to add from the whole dataset
            isAdded = true;
            Set<Node> entireNodeList = qualifier.entireNodeList();
            int chosenIndex = generator.nextInt(entireNodeList.size());
            int i = 0;
            for (Node n : entireNodeList) {
                if (i++ == chosenIndex) {
                    editedNode = n;
                    break;
                }
            }
        } else {
            double[] w = new double[addWeight.length + removeWeight.length];
            int j = 0;
            for (int i = 0; i < addWeight.length; i++){
                w[j++] = addWeight[i];
            }
            for (int i = 0; i < removeWeight.length; i++){
                w[j++] = removeWeight[i];
            }
            int chosenIndex = randomIndex(generator, w);
            if (chosenIndex < addWeight.length){ // add a vertex
                isAdded = true;
                editedNode = candidateArrayAdd[chosenIndex];
            } else { // remove a vertex
                editedNode = candidateArrayRemove[chosenIndex - addWeight.length];
            }
        }

        // create a node with the new infomation
        PogNode q = null;
        if (isAdded) {
            q = recalcInfoAdded(editedNode, qualifier);
        } else {
            q = recalcInfoRemoved(editedNode, qualifier);
        }

        if (q != null) {
            candidateEditedNode = editedNode;
        }
        return q;
    }

    // used for MHDA
    // randomly pick a neigbor (except the desertedNeighbor) uniform at random
    public PogNode chooseNeighborUniform_DA(TreeQualifier qualifier, Random generator) {
        if (lastEditedNode == null) { // first seed, no need to worry
            return chooseNeighborUniform(qualifier, generator);
        }

        Node editedNode = null;
        boolean isAdded = false;
        if (addableList.isEmpty() && removableList.isEmpty()) { // this is a null node; choose a node to add from the whole dataset
            isAdded = true;
            Set<Node> entireNodeList = new HashSet<Node>(qualifier.entireNodeList());
            entireNodeList.remove(lastEditedNode);  // remove lastEditedNode from the candidate set
            int chosenIndex = generator.nextInt(entireNodeList.size());
            int i = 0;
            for (Node n : entireNodeList) {
                if (i++ == chosenIndex) {
                    editedNode = n;
                    break;
                }
            }
        } else { // pick from the candidate set       
            int noAddable = addableList.size();
            int noRemovable = removableList.size();
            boolean lastAdded = false;
            if (addableList.contains(lastEditedNode)) {
                noAddable--;
                lastAdded = true;
            } else {
                noRemovable--;
            }
            int noCandidate = noAddable + noRemovable;
            int chosenIndex = generator.nextInt(noCandidate);
            if (chosenIndex < noAddable) { // add a node
                if (lastAdded && chosenIndex >= candidateMapAddRemove.get(lastEditedNode)) { // avoid last edited node
                    chosenIndex++;
                }
                editedNode = candidateArrayAdd[chosenIndex];
                isAdded = true;
            } else { // remove a node
                if (!lastAdded && chosenIndex >= candidateMapAddRemove.get(lastEditedNode)) { // avoid last edited node
                    chosenIndex++;
                }
                editedNode = candidateArrayRemove[chosenIndex];
            }
        }

        // create a node with the new infomation
        PogNode q = null;
        if (isAdded) {
            q = recalcInfoAdded(editedNode, qualifier);
        } else {
            q = recalcInfoRemoved(editedNode, qualifier);
        }

        if (q != null) {
            candidateEditedNode = editedNode;
        }
        return q;
    }

    // used for MHDA
    // randomly pick a neigbor (except the desertedNeighbor) using the weight on edges
    // beta is prob that a vertex is removed from the subgraph
    public PogNode chooseNeighborWeighted_DA(TreeQualifier qualifier, Random generator, double beta) {
//        if (lastEditedNode == null){ // first seed, no need to worry
//            return chooseNeighborWeighted(qualifier, generator, beta);
//        }

        Node editedNode = null;
        boolean isAdded = false;
        if (addableList.isEmpty() && removableList.isEmpty()) { // this is a null node; choose a node to add from the whole dataset
            isAdded = true;
            Set<Node> entireNodeList = new HashSet<Node>(qualifier.entireNodeList());
            entireNodeList.remove(lastEditedNode);  // remove lastEditedNode from the candidate set
            int chosenIndex = generator.nextInt(entireNodeList.size());
            int i = 0;
            for (Node n : entireNodeList) {
                if (i++ == chosenIndex) {
                    editedNode = n;
                    break;
                }
            }
        } else { // pick from the candidate set
            int chosenIndex;
            // decide whether add or remove
            if (generator.nextDouble() > beta && !addableList.isEmpty()) {  // add a vertex to the subgraph  
                if (addableList.contains(lastEditedNode)) {
                    chosenIndex = randomIndex(generator, addableList.size(), candidateMapAddRemove.get(lastEditedNode));
                } else {
                    chosenIndex = generator.nextInt(addableList.size());
                }
                isAdded = true;
                editedNode = candidateArrayAdd[chosenIndex];
            } else {    // remove a vertex from the subgraph
                if (removableList.contains(lastEditedNode)) {
                    chosenIndex = randomIndex(generator, removableList.size(), candidateMapAddRemove.get(lastEditedNode));
                } else {
                    chosenIndex = generator.nextInt(removableList.size());
                }
                editedNode = candidateArrayRemove[chosenIndex];
            }
        }

        // create a node with the new infomation
        PogNode q = null;
        if (isAdded) {
            q = recalcInfoAdded(editedNode, qualifier);
        } else {
            q = recalcInfoRemoved(editedNode, qualifier);
        }

        if (q != null) {
            candidateEditedNode = editedNode;
        }
        return q;
    }

    // randomly pick a neigbor pattern using distribution get from the pilot sampling
    // inProb is the prob to favor subgraphs in the zone
    public PogNode chooseNeighborPilot(TreeQualifier qualifier, Random generator, double inProb) {
        Node editedNode = null;
        boolean isAdded = false;
        if (addableList.isEmpty() && removableList.isEmpty()) { // this is a null node; choose a node to add from the whole dataset
            isAdded = true;
            Set<Node> entireNodeList = qualifier.entireNodeList();
            int chosenIndex = generator.nextInt(entireNodeList.size());
            int i = 0;
            for (Node n : entireNodeList) {
                if (i++ == chosenIndex) {
                    editedNode = n;
                    break;
                }
            }
        } else { // pick from the candidate set
            // create the In and Out sets            
            int size = size();
            boolean downZone = Tree.inZone(size - 1);
            boolean upZone = Tree.inZone(size + 1);
            if (downZone && upZone && generator.nextDouble() <= (double) getNoNeighborDown() / (getNoNeighborDown() + getNoNeighborUp())
                    || downZone && generator.nextDouble() <= inProb
                    || upZone && generator.nextDouble() > inProb) { // removal                
                int chosenIndex = generator.nextInt(removableList.size());
                int i = 0;
                for (Node n : removableList) {
                    if (i++ == chosenIndex) {
                        editedNode = n;
                        break;
                    }
                }
            } else { // addition
                int chosenIndex = generator.nextInt(addableList.size());
                int i = 0;
                for (Node n : addableList) {
                    if (i++ == chosenIndex) {
                        editedNode = n;
                        break;
                    }
                }
                isAdded = true;
            }
        }

        // create a node with the new infomation
        if (isAdded) {
            return recalcInfoAdded(editedNode, qualifier);
        } else {
            return recalcInfoRemoved(editedNode, qualifier);
        }
    }

    //random chosen an index with given distribution
    private int randomIndex(Random generator, double[] weight) {
        double r = generator.nextDouble();
        double sum = 0.0;
        for (int i = 0; i < weight.length; i++) {
            sum += weight[i];
            if (r < sum) {
                return i;
            }
        }
        return 0;
    }

    //random chosen an index with given distribution
    // sum of all weights is sumWeight instead of 1
    private int randomIndex_unnormalized(Random generator, double[] weight) {
        double sumWeight = 0;
        for (int i = 0; i < weight.length; i++) {
            sumWeight += weight[i];
        }
        double r = generator.nextDouble() * sumWeight;
        double sum = 0.0;
        for (int i = 0; i < weight.length; i++) {
            sum += weight[i];
            if (r < sum) {
                return i;
            }
        }
        return 0;
    }

    //random chosen an index with given distribution
    // except the desertedIndex, corresponding to a deserted editedNode
    private int randomIndex(Random generator, double[] weight, int desertedIndex) {
        double r = generator.nextDouble() * (1 - weight[desertedIndex]);
        double sum = 0.0;
        for (int i = 0; i < weight.length; i++) {
            if (i == desertedIndex) {
                continue;
            }
            sum += weight[i];
            if (r < sum) {
                return i;
            }
        }
        return 0;
    }

    // choose an index uniform at random
    // except the desertedIndex, corresponding to a deserted editedNode
    private int randomIndex(Random generator, int noCandidate, int desertedIndex) {
        int r = generator.nextInt(noCandidate - 1);
        if (r < desertedIndex) {
            return r;
        } else {
            return r + 1;
        }
    }

    // unused
    public boolean isConnected() {
        if (nodeList.isEmpty()) {
            return true;
        } else {
            return isGraphConnected(nodeList);
        }
    }

    // unused
    private boolean isGraphConnected(Set<Node> nodeList) {
        Iterator<Node> iter = nodeList.iterator();

        if (!iter.hasNext()) {
            return false;
        }
        ArrayList<Node> toVisitNodes = new ArrayList<Node>();
        toVisitNodes.add(iter.next());
        Set<Node> visitedNodes = new HashSet<Node>();
        while (!toVisitNodes.isEmpty()) {
            Node n = toVisitNodes.get(0);
            toVisitNodes.remove(0);
            Set<Node> neighbors = new HashSet<Node>(n.neighbors);
            neighbors.retainAll(nodeList);
            neighbors.removeAll(visitedNodes);

            visitedNodes.add(n);

            toVisitNodes.addAll(neighbors);
        }
        if (visitedNodes.size() == nodeList.size()) {
            return true;
        }
        return false;
    }

    // unused
    public int degree(TreeQualifier qualifier) {
        if (nodeList.isEmpty()) {
            return qualifier.entireNodeList().size();
        } else {
            return addableList.size() + removableList.size();
        }
    }

    public int degreeUp(TreeQualifier qualifier) {
        if (nodeList.isEmpty()) {
            return qualifier.entireNodeList().size();
        } else {
            return addableList.size();
        }
    }

    public int degreeDown(TreeQualifier qualifier) {
        return removableList.size();
    }

    @Override
    public boolean equals(Object p) {
        if (!(p instanceof PogNode)) {
            return false;
        }
        PogNode _p = (PogNode) p;
        return nodeList.equals(_p.nodeList);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.nodeList != null ? this.nodeList.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return nodeList.toString();
    }
}
