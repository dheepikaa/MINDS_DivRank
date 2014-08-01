/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.quality;

/**
 *
 * @author minhhx
 */
public class GenePairRankerEntry implements Comparable{
    public int geneA;
    public int geneB;
    public double quality;
    public GenePairRankerEntry(int _geneA, int _geneB, double _quality){
        geneA = _geneA;
        geneB = _geneB;
        quality = _quality;
    }

    public int compareTo(Object t) {
        GenePairRankerEntry item = (GenePairRankerEntry) t;
        if (quality < item.quality) {
            return 1;
        } else if (quality > item.quality) {
            return -1;
        } else {
            return 0;
        }
    }
}
