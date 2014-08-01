package gclassifier.answer;

import gclassifier.Node;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Subgraph implements Comparable{

    public double score;
    int id;
    public Set<Node> nodes;

    public Subgraph(Set<Node> nodes, double score, int id) {
        this.id = id;
        this.score = score;
        this.nodes = nodes;
    }

    public int size() {
        return nodes.size();
    }

    public void print() {
        // TODO Auto-generated method stub
        System.err.print(id + " Score: " + score + " Nodes: ");
        for (Node n : nodes) {
            System.err.print(n.label + ",");
        }
        System.err.println();

    }

    public int compareTo(Object obj) {
        Subgraph item = (Subgraph) obj;
        if (score < item.score) {
            return 1;
        } else if (score > item.score) {
            return -1;
        } else {
            return 0;
        }
    }
    
    public String toString(){
        return nodes.toString();
    }
}
