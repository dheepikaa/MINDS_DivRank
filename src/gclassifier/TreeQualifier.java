/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier;

import gclassifier.answer.SimpleTree;
import gclassifier.quality.GenePair;
import gclassifier.quality.GenePairQuality;
import gclassifier.sampler.PogNode;
import java.util.HashMap;
import java.util.Set;
import libsvm.SVMHelper;

/**
 *
 * @author kasturi
 */
public interface TreeQualifier {
    // given a new node to add/remove from the current subgraph in the random walk
    // return the quality of the corresponding decision tree
    public double updateClassifier(Node editedFeature, boolean isAdd);

    // generate any frequent pattern as an initial node for the random walk
    public PogNode genRandSeedSubgraph(int maxSize);

    // generate any frequent pattern
    // return only list of nodes and info density
    public PogNode genRandSeedSubgraphSimple(int maxSize);

    // sampler reject the subgraph
    // so we need to reverse the change to tree
    public void reverseChange();

    // return the entire node list of the graph
    public Set<Node> entireNodeList();

    // return the size of the sample data set
    public int sampleSetSize();

    // test if the current subgraph is connected
    public boolean useAllFeatures();

    // get the infomation gain if added a node to the tree
    public double getInfoGainAdded(Node newFeature);

    // get the estimated information gain that is lost if removing a node from the tree
    public double getInfoGainRemoved(Node feature);

    // get the baseline for calculating alpha
    public double getBaseline();

    // build a simple tree for random forest, given a list of nodes
    public SimpleTree buildSimpleTree(Set<Node> subgraphNodeList);

    public void statInfoGain();
    
    public Node getNodeByLabel(String label);
    
    public void buildTreeStatPair(Set<Node> subgraphNodeList, HashMap<GenePair, GenePairQuality> genePairMap);
    
    public boolean[][] getStoreFile();
    
    public SVMHelper getCurrentSVM();
    
    public void deleteEdge(int id1, int id2);
    
    // generate any frequent pattern as an initial node for the random walk    
    public Set<Node> randomSubGraph(Set<Node> entireNodeSet, int maxSize);
    
    // build an SVM for SVM ensembler, given a list of nodes
    public SVMHelper buildSVM(Set<Node> subgraphNodeList);

	public void reverseChangeDivrank();
}
