/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.sampler;

import gclassifier.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author minhhx
 */
public class DFSTree {

    private DFSNode root;
    private HashMap<Node, DFSNode> nodeMap;
    public Set<Node> removables;

    public DFSTree(Set<Node> nodeList) {
        removables = new HashSet<Node>();
        if (nodeList.isEmpty()) {
            root = null;
            return;
        }

        nodeMap = new HashMap<Node, DFSNode>();
        int labelCount = 0;
        Set<Node> visitedNodes = new HashSet<Node>();

        Node n = nodeList.iterator().next();
        visitedNodes.add(n);

        root = new DFSNode(n, null, labelCount++);
        nodeMap.put(n, root);

        DFSNode currentNode = root;

        while (currentNode != null) {
            Set<Node> toVisitNodes = new HashSet<Node>(currentNode.node.neighbors);
            toVisitNodes.retainAll(nodeList);
            toVisitNodes.removeAll(visitedNodes);

            if (toVisitNodes.isEmpty()) {
                currentNode = currentNode.parent;
            } else {
                n = toVisitNodes.iterator().next();
                visitedNodes.add(n);
                DFSNode newNode = new DFSNode(n, currentNode, labelCount++);
                currentNode.children.add(newNode);
                currentNode = newNode;
                nodeMap.put(n, currentNode);
            }
        }

        // build all the non-tree edges
        for (Node _node : nodeList) {
            DFSNode dfsNode = nodeMap.get(_node);
            if (dfsNode.parent != null) {
                Set<Node> neighbors = new HashSet<Node>(_node.neighbors);
                neighbors.retainAll(nodeList);

                neighbors.remove(dfsNode.parent.node);

                for (Node _n : neighbors) {
                    dfsNode.nonTreeNeighbors.add(nodeMap.get(_n));
                }
            }
        }

        // calculate lower and find removable vertices        
        calcLower(root);
        if (root.children.size() <= 1) {
            removables.add(root.node);
        }
    }

    private int calcLower(DFSNode currentNode) {
        int low = currentNode.dfsLabel;

        for (DFSNode nonTreeNeighbor : currentNode.nonTreeNeighbors) {
            low = Math.min(low, nonTreeNeighbor.dfsLabel);
        }
        for (DFSNode child : currentNode.children) {
            low = Math.min(low, calcLower(child));
        }
        currentNode.low = low;
        // check if current Node is removable
        for (DFSNode child : currentNode.children) {
            if (child.low >= currentNode.dfsLabel) {
                return low;
            }
        }
        removables.add(currentNode.node);
        return low;
    }
}
