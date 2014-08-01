/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gclassifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author minhhx
 */
public class TreeNode {
    Node feature;
    // left is true, right is false
    TreeNode trueChild;
    TreeNode falseChild;
    Set<Integer> sampleSetTrue;
    Set<Integer> sampleSetFalse;
    double infoGain;

    int noCorrect;
    boolean classLabel;

    TreeNode parent;    // hx: parent of the current node. rootnode has parent = null

    public TreeNode(Node chosenFeature, TreeNode _parent) {
        feature = chosenFeature;
        trueChild = null;
        falseChild = null;
        infoGain = 0;
        //classLabel = false;
        sampleSetTrue = null;
        sampleSetFalse = null;

        parent = _parent;
        noCorrect = 0;
        classLabel = false;
    }

    public TreeNode(TreeNode n){
        feature = n.feature;
        trueChild = null;
        falseChild = null;
        sampleSetFalse = n.sampleSetFalse;
        sampleSetTrue = n.sampleSetTrue;
        parent = null;
        infoGain = n.infoGain;
        noCorrect = n.noCorrect;
        classLabel = n.classLabel;
    }

    public Set<Node> getusedFeatures(){
        Set<Node> usedFeatures = new HashSet<Node>();
        usedFeatures.add(feature);
        if (parent != null)
            usedFeatures.addAll(parent.getusedFeatures());
        return usedFeatures;
    }

//    public Set<Node> getpreviousUsedFeatures(){
//        Set<Node> usedFeatures = new HashSet<Node>();
//        if (parent != null)
//            usedFeatures.addAll(parent.getusedFeatures());
//        return usedFeatures;
//    }

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

    public Set<Node> updateCandidateFeatures(Set<Node> currentCandidates, Set<Node> subgraphNodes){
        // update the candidateNodes list to include all neighbors of the chosen feature
        Set<Node> candidateNodeList = new HashSet<Node>(subgraphNodes);
        candidateNodeList.retainAll(feature.neighbors);
        candidateNodeList.addAll(currentCandidates);
        Set<Node> usedFeatures = new HashSet<Node>();
        getusedFeatures(usedFeatures);
        candidateNodeList.removeAll(usedFeatures);
        
        return candidateNodeList;
    }

    public void insertChild(TreeNode child, boolean isTrueChild){
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
