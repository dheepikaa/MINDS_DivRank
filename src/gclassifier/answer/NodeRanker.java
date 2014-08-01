/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.answer;

import gclassifier.Node;

/**
 *
 * @author mhoang
 */
public class NodeRanker implements Comparable{
    Node n;
    int count;
    public NodeRanker(Node _n, int _count){
        n = _n;
        count = _count;
    }
    public int compareTo(Object t) {
        NodeRanker r = (NodeRanker)t;
        if (count > r.count) {
            return -1;
        } else if (count < r.count) {
            return 1;
        } else {
            return 0;
        }
    }
    
}
