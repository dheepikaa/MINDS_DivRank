/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier;

/**
 *
 * @author minhhx
 */
public class Edge {

    Node node1;
    Node node2;

    Edge() {
    }

    @Override
    public boolean equals(Object _obj){
        if (!(_obj instanceof Edge))
            return false;
        Edge e = (Edge)_obj;
        return (e.node1.equals(node1) && e.node2.equals(node2));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.node1 != null ? this.node1.hashCode() : 0);
        hash = 37 * hash + (this.node2 != null ? this.node2.hashCode() : 0);
        return hash;
    }
}
