/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package libsvm;

import gclassifier.Node;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author mhoang
 * used for two purpose: 1) build the SVM for a given subgraph, and 2) get the (SVM model) weights of a given list of nodes
 */
public class SVMHelper {
    private Set<Node> nodeList;
    private svm_model model;
    private double potential;
    public SVMHelper(boolean[][] _storeFile, Set<Node> _subgraphNodeList){
        svm_problem prob;
        nodeList = new HashSet<Node>();
        nodeList.addAll(_subgraphNodeList);
        prob = new svm_problem();
        prob.l = _storeFile.length;
        int noFea = _subgraphNodeList.size();
        prob.x = new svm_node[prob.l][noFea];
        prob.y = new double[prob.l];
        for (int i = 0; i <prob.l; i++){
            svm_node[] nodeList = prob.x[i];
            int j = 0;
            for (Node n : _subgraphNodeList){
                svm_node aNode = new svm_node();
                aNode.index = n.id;
                aNode.value = _storeFile[i][n.id]?1:0;
                nodeList[j++] = aNode;
            }
            
            prob.y[i] = _storeFile[i][0]?1:-1;
        }
        
        // set param
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.degree = 3;
        param.gamma = 0;	// 1/num_features
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 100;
        param.C = 1;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 0;
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];

        model = svm.svm_train(prob, param);
        potential = calcAccuracy(prob);
    }
    
    
    public static svm_problem createSVMProblem(boolean[][] _storeFile){
        svm_problem prob;
        prob = new svm_problem();
        prob.l = _storeFile.length;
        int noFea = _storeFile[0].length - 1;
        prob.x = new svm_node[prob.l][noFea];
        prob.y = new double[prob.l];
        for (int i = 0; i <prob.l; i++){
            svm_node[] nodeList = prob.x[i];            
            for (int j = 0; j < noFea; j++){
                svm_node aNode = new svm_node();
                aNode.index = j+1;
                aNode.value = _storeFile[i][j+1]?1:0;
                nodeList[j] = aNode;
            }
            
            prob.y[i] = _storeFile[i][0]?1:-1;
        }        
        return prob;
    }
    
    public SVMHelper(SVMHelper aHelper){
        model = aHelper.model;
        nodeList = aHelper.nodeList;
    }    
    
    public Set<Node> getNodeList(){
        return nodeList;
    }    
    
    public double getPotential(){
        return potential;
    }
    
    // calc accuracy given a set of samples
    public double calcAccuracy(svm_problem _prob){
        int noCorrect = 0;
        double prediction;
        for (int i = 0; i < _prob.l; i++) {
            prediction = svm.svm_predict(model, _prob.x[i]);
            if (prediction == _prob.y[i]){
                noCorrect++;
            }
        }
        return (double)noCorrect/_prob.l;
    }
    
    public double[] predict(svm_problem _prob){
        double[] result = new double[_prob.l];
        for (int i = 0; i < _prob.l; i++) {
            result[i]=svm.svm_predict(model, _prob.x[i]);          
        }
        return result;
    }
    
    // calculate the weight for a given set of nodes
    // return nodeid -> weights
    public HashMap<Integer, Double> getWeights(Set<Node> nodeSet){
        Set<Integer> nodeIndexSet = new HashSet<Integer>();
        for (Node n : nodeSet){
            nodeIndexSet.add(n.id);
        }
        HashMap<Integer, Double> weights = new HashMap<Integer, Double>();  
        int nodeIndex;
        for (int i = 0; i < model.SV.length; i++){
            for (int j = 0; j < model.SV[i].length; j++){
                nodeIndex = model.SV[i][j].index;
                if (nodeIndexSet.contains(nodeIndex)){
                    if (weights.containsKey(nodeIndex)){
                        weights.put(nodeIndex, weights.get(nodeIndex) + model.SV[i][j].value * model.sv_coef[0][i]); 
                    } else {
                        weights.put(nodeIndex, model.SV[i][j].value * model.sv_coef[0][i]);
                    }                    
                }
                               
            }
        }
        for (Integer index : weights.keySet()){
            weights.put(index, Math.abs(weights.get(index)));
        }
        return weights;        
    }
}
