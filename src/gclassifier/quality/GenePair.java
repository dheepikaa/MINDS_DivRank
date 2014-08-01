/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.quality;

/**
 *
 * @author minhhx
 */
public class GenePair {
    // id of two genes
    public int geneA; // root of tree
    public int geneB; // direct child of geneA
    
    public boolean geneAExpression;
    public boolean geneBExpression;
    
    public GenePair(int _geneA, int _geneB, boolean _geneAExp, boolean _geneBExp){
        geneA = _geneA;
        geneB = _geneB;
        geneAExpression = _geneAExp;
        geneBExpression = _geneBExp;
    }
    
    @Override
    public boolean equals(Object o){
        GenePair pair = (GenePair) o;
        if (pair.geneA == geneA && pair.geneB == geneB &&
                pair.geneAExpression == geneAExpression &&
                pair.geneBExpression == geneBExpression){
            return true;
        } else {
            return false;
        }        
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + this.geneA;
        hash = 89 * hash + this.geneB;
        hash = 89 * hash + (this.geneAExpression ? 1 : 0);
        hash = 89 * hash + (this.geneBExpression ? 1 : 0);
        return hash;
    }
}
