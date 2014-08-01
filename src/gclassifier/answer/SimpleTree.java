/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gclassifier.answer;

import gclassifier.Node;
import gclassifier.Tree;
import gclassifier.TreeRandForest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author minhhx
 */
public class SimpleTree implements Comparable{
    SimpleTreeNode root;
    public double weight;
    public Set<Node> featureList;
    public SimpleTree(Tree optimalTree){
        root = optimalTree.simplifyTree();
        weight = optimalTree.accuracy();
        featureList = new HashSet<Node>(optimalTree.featureList);
    }

    public SimpleTree(TreeRandForest randomTree){
        root = randomTree.simplifyTree();
    }
    
    // classify a sample inputed as a list of positive features
    public boolean classify(List<String> positiveFeatures){
        SimpleTreeNode curNode = root;
        while (curNode != null){
            if (curNode.feature() == null){
                // reach a leaf node, return class label
                return curNode.classLabel();
            }
            if (positiveFeatures.contains(curNode.feature())){
                // go to true child branch
                curNode = curNode.trueChild();
            } else {
                // go to false child branch
                curNode = curNode.falseChild();
            }
        }
        return false;
    }

    public boolean weightedClassify(){
        return true;
    }

    public void print() {
        System.out.println("accuracy " + weight);
        print("", root, true, false);
        System.out.println();
    }

    private void print(String prefix, SimpleTreeNode node, boolean isTail, boolean truebranch) {
        if (node != null){
            if (truebranch)
                System.out.print(prefix + (isTail ? "└─t─ " : "├─t─ "));
            else
                System.out.print(prefix + (isTail ? "└─f─ " : "├─f─ "));
            if (node.feature != null){
                System.out.println(node.feature);
                if (node.trueChild == null){
                    print(prefix + (isTail? "    " : "│   "), node.falseChild, true, false);
                } else if (node.falseChild == null){
                    print(prefix + (isTail? "    " : "│   "), node.trueChild, true, true);
                } else {
                    print(prefix + (isTail? "    " : "│   "), node.trueChild, false, true);
                    print(prefix + (isTail? "    " : "│   "), node.falseChild, true, false);
                }
            } else {
                System.out.println(node.classLabel);
            }
        }
    }

    public int compareTo(Object o) {
        SimpleTree tree = (SimpleTree)o;
        if (weight > tree.weight) {
            return 1;
        } else if (weight < tree.weight) {
            return -1;
        } else {
            return 0;
        }
    }
}
