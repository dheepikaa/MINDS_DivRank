/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gclassifier.sampler;

import gclassifier.Node;
import java.util.ArrayList;

/**
 *
 * @author minhhx
 */
public class DFSNode {
    Node node;
    int dfsLabel;
    DFSNode parent;
    ArrayList<DFSNode> children;
    ArrayList<DFSNode> nonTreeNeighbors;
    int low;

    public DFSNode(Node n, DFSNode _parent, int label){
        children = new ArrayList<DFSNode>();
        nonTreeNeighbors = new ArrayList<DFSNode>();
        parent = _parent;
        dfsLabel = label;
        node = n;
    }
}
