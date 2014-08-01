/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author minhhx
 */
public class Node {

    public String label;  //to store the actual number of a node
    public int id;    // to store the location of the node in the nodes arraylist
    public Set<Node> neighbors;  //this contains the indices of the ArrayList "edges" which will give us the neighborhood of every node
    //static HashSet<String> protVal; //this contains the sample names for which the protein takes val 1

    public Node(int _id, String _label) {
        neighbors = new HashSet<Node>();
        //protVal = new HashSet();
        id = _id;
        label = _label;
    }

    @Override
    public boolean equals(Object u) {
        if (!(u instanceof Node)) {
            return false;
        }
        Node _u = (Node) u;
        return (label == null ? _u.label == null : label.equals(_u.label)) && id == _u.id;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.label != null ? this.label.hashCode() : 0);
        hash = 67 * hash + this.id;
        return hash;
    }

    @Override
    public String toString(){
        String _return = "";
        //_return += "( " + Integer.toString(id) + " , " + label + " ) ";
        _return += label;
        return _return;
    }
}
