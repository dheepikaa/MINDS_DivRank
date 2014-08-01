/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.answer;

import gclassifier.TreeQualifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import libsvm.SVMHelper;
import libsvm.svm_problem;

/**
 *
 * @author mhoang
 */
public class SVMEnsembler {
    Set<SVMHelper> ensembler;
    public SVMEnsembler(Collection<Subgraph> subgraphSet, TreeQualifier qualifier){
        ensembler = new HashSet<SVMHelper>();        
        for (Subgraph sub : subgraphSet){
            SVMHelper anSVM = qualifier.buildSVM(sub.nodes);
            ensembler.add(anSVM);
        }        
    }
    
    // return the accuracy
    public double classify(svm_problem _prob){    
        int[] countClass = new int[_prob.l];
        double[] result = null;
        for (SVMHelper aSVM : ensembler){
            result = aSVM.predict(_prob);
            for (int i = 0; i < result.length; i++){
                if (result[i] == 1){
                    countClass[i] += 1;
                }
            }
        }
        
        // return the majority vote for each item in _prob
        double decision = 1;
        double acc = 0.0;
        int decisionPlane = ensembler.size()/2;
        if (decisionPlane*2 < ensembler.size()){
            decisionPlane += 1;
        }
        for (int i = 0; i < _prob.l; i++){
            decision = countClass[i] >= decisionPlane ? 1:-1;
            if (decision == _prob.y[i]){
                acc += 1;
            }            
        }
        return acc/_prob.l;
    }
}
