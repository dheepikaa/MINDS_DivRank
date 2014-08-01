/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gclassifier.answer;

/**
 *
 * @author minhhx
 */
public class SimpleTreeNode {
    public String feature;
    public SimpleTreeNode trueChild;
    public SimpleTreeNode falseChild;
    public boolean classLabel;

    public SimpleTreeNode(String _feature){
        feature = _feature;
    }

    public SimpleTreeNode(boolean _classLabel){
        feature = null;
        classLabel = _classLabel;
    }

    public void setChild(SimpleTreeNode _trueChild, SimpleTreeNode _falseChild){
        trueChild = _trueChild;
        falseChild = _falseChild;
    }

    public SimpleTreeNode trueChild(){
        return trueChild;
    }

    public SimpleTreeNode falseChild(){
        return falseChild;
    }

    public boolean classLabel(){
        return classLabel;
    }

    public String feature(){
        return feature;
    }
}
