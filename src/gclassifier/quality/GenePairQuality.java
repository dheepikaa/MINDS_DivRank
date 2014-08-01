/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.quality;

/**
 *
 * @author minhhx
 */
public class GenePairQuality {
    public double sumAcc;
    public int noTrees;
    public double accuracy(){
        return sumAcc/noTrees;
    }
}
