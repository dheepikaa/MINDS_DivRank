/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gclassifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.*;

public class RandForestNode {
    Node feature;
    int id;
    // left is true, right is false
    RandForestNode trueChild;
    RandForestNode falseChild;
    Set<Integer> sampleSetTrue;
    Set<Integer> sampleSetFalse;
    double GiniIndex;
    int noMisclassified;
    
    RandForestNode parent;    // hx: parent of the current node. rootnode has parent = null    
//    
   double trueProb;
//    String classLabel;
    

    public RandForestNode(Node chosenFeature, RandForestNode _parent, double GiniVal) {
        feature = chosenFeature;
        trueChild = null;
        falseChild = null;
        //classLabel = false;
        sampleSetTrue = null;
        sampleSetFalse = null;
        
        parent = _parent;
        this.GiniIndex = GiniVal;
        noMisclassified = 0;
        trueProb = 0.0;
    }

    public Set<Node> getusedFeatures(){
        Set<Node> usedFeatures = new HashSet<Node>();
        usedFeatures.add(feature);
        if (parent != null)
            usedFeatures.addAll(parent.getusedFeatures());
        return usedFeatures;
    }


    public void getusedFeatures(Set<Node> _return){
        if (feature != null)
            _return.add(feature);
        if (parent != null)
            parent.getusedFeatures(_return);
    }
    
    public void getpreviousUsedFeatures(Set<Node> _return){
        if (parent != null)
            parent.getusedFeatures(_return);
    }

    //random chosen an index with given distribution
    private int randomIndex(Random generator, ArrayList<Double> weight) {
        double r = generator.nextDouble();
        double sum = 0.0;
        for (int i = 0; i < weight.size(); i++) {
            sum += weight.get(i);
            if (r < sum) {
                return i;
            }
        }
        return 0;
    }

    // usedFeatures should contain "this" node's feature
    public Set<Node> updateCandidateFeatures(Set<Node> currentCandidates, Set<Node> usedFeatures, Node chosen){
        Set<Node> newCandidates = new HashSet<Node>(chosen.neighbors);
        newCandidates.removeAll(currentCandidates);
        newCandidates.removeAll(usedFeatures);

        if (newCandidates.size() <= 5){
            newCandidates.addAll(currentCandidates);
            return newCandidates;
        }

        ArrayList<Node> candidateArray = new ArrayList<Node>(newCandidates);
        ArrayList<Double> weight = new ArrayList<Double>();
        double weightSum = 0.0;

        for(int i = 0; i < candidateArray.size(); i++){
            Node n = candidateArray.get(i);
            Set<Node> neighbors = new HashSet<Node>(n.neighbors);
            neighbors.retainAll(usedFeatures);
            weight.add(new Double(neighbors.size()));
            weightSum += neighbors.size();
        }

        for (Double d : weight){
            d /= weightSum;
        }

        Set<Node> chosenNodes = new HashSet<Node>();
        Random generator = new Random();
        int count = 0;
        while (count < 5){
            int chosenIndex = randomIndex(generator, weight);
            weight.remove(chosenIndex);
            chosenNodes.add(candidateArray.remove(chosenIndex));
            count++;
        }

        chosenNodes.addAll(currentCandidates);
        return chosenNodes;
    }


    public void insertChild(RandForestNode child, boolean isTrueChild){
        if (isTrueChild){
            trueChild = child;
        }
        else
            falseChild = child;
        if (child != null)
            child.parent = this;
    }

    public boolean checkTree(Set<Node> featureList){
        boolean check = true;
        if (feature!= null && !featureList.contains(feature))
            return false;
        if (trueChild != null && trueChild.parent != this)
            return false;
        else if (trueChild != null)
            check &= trueChild.checkTree(featureList);

        if (falseChild != null && falseChild.parent != this)
            return false;
        else if (falseChild != null)
            check &= falseChild.checkTree(featureList);

        return check;
    }
}
