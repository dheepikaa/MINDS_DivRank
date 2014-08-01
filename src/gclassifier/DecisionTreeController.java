package gclassifier;

import gclassifier.answer.OptimallyDiscriminativeSet;
import gclassifier.answer.SimpleTree;
import gclassifier.answer.Subgraph;
import gclassifier.quality.GenePair;
import gclassifier.quality.GenePairQuality;
import gclassifier.sampler.MetroSampler;
import gclassifier.sampler.PogNode;
import java.util.*;
import java.io.*;
import java.io.FileReader;
import java.util.HashMap;
import libsvm.SVMHelper;

public class DecisionTreeController implements TreeQualifier {
    //to store the whole file in this array, this is what will be used henceforth

    boolean[][] storeFile;
    Set<Integer> sampleIndex;
    public Graph ppiGraph;
    private Tree backupTree;
    private Tree currentTree;
    private SVMHelper backupSVM;
    private SVMHelper currentSVM;
    Random generator;
    int POGsize;
    private double deltaBaseline;
    long time;
    int[] noSize;
    double[] potentialPerSize;
    int[] noPotential;
    OptimallyDiscriminativeSet answerSet;
    HashMap<String, Integer> protidMap;

    public DecisionTreeController(String fileProExp, String fileSampleLabel, String filePPI, boolean isPPIPruned, int maxNoProtein) {
        sampleIndex = new HashSet();
        if (maxNoProtein == 0) {
            maxNoProtein = Integer.MAX_VALUE;
        }
        protidMap = loadSamples(fileProExp, fileSampleLabel);
        generator = new Random();
//        protidMap = loadSamples(fileProExp, fileSampleLabel, maxNoProtein);

        if (isPPIPruned) {
            loadPrunedGraph(filePPI, protidMap, maxNoProtein);
//            loadPrunedGraph(filePPI, protidMap);
        } else {
            loadFullGraph(filePPI, protidMap);
        }
        
        // for Nazli
        // exteng the store_File
        boolean[][] storeFile_tmp = new boolean[storeFile.length][protidMap.size() + 1];

        for (int i = 0; i < storeFile.length; i++) {
            //initializing all protein expression values to false
            Arrays.fill(storeFile_tmp[i], false);
            for (int j = 0; j < storeFile[i].length; j++){
                storeFile_tmp[i][j] = storeFile[i][j];
            }
        }
        storeFile = storeFile_tmp;
        // end for Nazli
        
    }

    // random labels
    public DecisionTreeController(String fileProExp, String filePPI) {
        generator = new Random();
        sampleIndex = new HashSet();

        protidMap = loadSamplesRandLabel(fileProExp);

        loadPrunedGraph(filePPI, protidMap);
        generator = new Random();
        
        // for Nazli
        // exteng the store_File
        boolean[][] storeFile_tmp = new boolean[storeFile.length][protidMap.size() + 1];

        for (int i = 0; i < storeFile.length; i++) {
            //initializing all protein expression values to false
            Arrays.fill(storeFile_tmp[i], false);
            for (int j = 0; j < storeFile[i].length; j++){
                storeFile_tmp[i][j] = storeFile[i][j];
            }
        }
        storeFile = storeFile_tmp;
        // end for Nazli
    }

    // random expression levels
    public DecisionTreeController(String fileProExp, String fileSampleLabel, String filePPI) {
        generator = new Random();
        sampleIndex = new HashSet();

        protidMap = loadSamplesRandExpLevel(fileProExp, fileSampleLabel);

        loadPrunedGraph(filePPI, protidMap);
        generator = new Random();
        
        // for Nazli
        // exteng the store_File
        boolean[][] storeFile_tmp = new boolean[storeFile.length][protidMap.size() + 1];

        for (int i = 0; i < storeFile.length; i++) {
            //initializing all protein expression values to false
            Arrays.fill(storeFile_tmp[i], false);
            for (int j = 0; j < storeFile[i].length; j++){
                storeFile_tmp[i][j] = storeFile[i][j];
            }
        }
        storeFile = storeFile_tmp;
        // end for Nazli
    }

    //random edges
    public DecisionTreeController(String fileProExp, String fileSampleLabel, String filePPI, double random) {
        // TODO Auto-generated constructor stub
        sampleIndex = new HashSet();
        generator = new Random();
        protidMap = loadSamplesRandExpLevel(fileProExp, fileSampleLabel);

        loadRandomGraph(filePPI, protidMap, random);
        // for Nazli
        // exteng the store_File
        boolean[][] storeFile_tmp = new boolean[storeFile.length][protidMap.size() + 1];

        for (int i = 0; i < storeFile.length; i++) {
            //initializing all protein expression values to false
            Arrays.fill(storeFile_tmp[i], false);
            for (int j = 0; j < storeFile[i].length; j++){
                storeFile_tmp[i][j] = storeFile[i][j];
            }
        }
        storeFile = storeFile_tmp;
        // end for Nazli
    }

    // random samples
    public DecisionTreeController(String filePPI, int noSamples, PogNode seed, int seedSize) {
        // map protein name to an index
        protidMap = new HashMap();
        generator = new Random();
        sampleIndex = new HashSet();
        //building the PPI network
        ppiGraph = new Graph();
        int count = 1;
        int maxNoNode = 61;
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePPI));

            while (br.ready()) {
                String X = br.readLine();
                String[] Y = X.split("\t");
                String A = Y[0].trim();
                String B = Y[1].trim();
                if (!protidMap.containsKey(A) && protidMap.size() < maxNoNode) {
                    protidMap.put(A, count++);
                }
                if (!protidMap.containsKey(B) && protidMap.size() < maxNoNode) {
                    protidMap.put(B, count++);
                }

                ppiGraph.addEdge(A, B, protidMap);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("No of nodes: " + ppiGraph.nodes.size()
                + " No of edges: " + ppiGraph.noEdges());

        // pick two arbitrarity connected nodes
        Set<Node> seedGraph = null;

        while (true) {
            seedGraph = randomSubGraph(ppiGraph.nodes, seedSize);
            if (seedGraph.size() == seedSize) {
                break;
            }
        }

        ArrayList<Node> nodeArray = new ArrayList<Node>(seedGraph);

        //------ create the sample List
        storeFile = new boolean[noSamples][protidMap.size() + 1];

        // 1/4 negative samples n1 = false
        count = noSamples / 2;
        int i = 0;
        for (i = 0; i < count; i++) {
            storeFile[i][0] = true;
            for (int j = 0; j < 5; j++) {
                storeFile[i][generator.nextInt(storeFile[i].length - 1) + 1] = true;
            }
            for (Node n : nodeArray) {
                storeFile[i][n.id] = true;
            }
        }

        // 1/4 negative samples n1 = true, n2 = false
        count = noSamples;
        for (; i < count; i++) {
            storeFile[i][0] = false;
            for (int j = 0; j < 5; j++) {
                storeFile[i][generator.nextInt(storeFile[i].length - 1) + 1] = true;
            }
            for (int j = 0; j < nodeArray.size() / 2; j++) {
                storeFile[i][nodeArray.get(generator.nextInt(nodeArray.size())).id] = true;
            }
        }

        /*
         // 1/2 positive samples n1 = true, n2 = true
         count = noSamples/2;
         int i = 0;
         for (i = 0; i < count ; i++){
         storeFile[i][0] = true;
         for (int j = 1; j < storeFile[i].length; j++){
         storeFile[i][j] = generator.nextBoolean();
         }
         storeFile[i][n1.id] = true;
         storeFile[i][n2.id] = true;            
         }
        
         // 1/6 negative samples n1 = false, n2 = false
         count += noSamples/6;
         for (; i < count ; i++){
         storeFile[i][0] = false;
         for (int j = 1; j < storeFile[i].length; j++){
         storeFile[i][j] = generator.nextBoolean();
         }
         storeFile[i][n1.id] = false;
         storeFile[i][n2.id] = false;
         }
         // 1/6 negative samples n1 = false, n2 = true
         count += noSamples/6;
         for (; i < count ; i++){
         storeFile[i][0] = false;
         for (int j = 1; j < storeFile[i].length; j++){
         storeFile[i][j] = generator.nextBoolean();
         }
         storeFile[i][n1.id] = false;
         storeFile[i][n2.id] = true;
         }
         // 1/6 negative samples n1 = true, n2 = false
         count = noSamples;
         for (; i < count ; i++){
         storeFile[i][0] = false;
         for (int j = 1; j < storeFile[i].length; j++){
         storeFile[i][j] = generator.nextBoolean();
         }
         storeFile[i][n1.id] = true;
         storeFile[i][n2.id] = false;
         }
         * 
         */

        // create sample index
        for (i = 0; i < storeFile.length; i++) {
            sampleIndex.add(i);
        }

        // create seedGraph
        seed.updatePogNode(seedGraph, calcInfoDensity(seedGraph));
    }

    // this constructor is used for cross validation
    public DecisionTreeController(Set<Integer> _sampleIndex,
            Graph _ppiGraph, boolean[][] _storeFile) {
        sampleIndex = _sampleIndex;
        generator = new Random();
        ppiGraph = _ppiGraph;
        storeFile = _storeFile;
    }

    // calculate the density of a graph from scratch
    private double calcInfoDensity(Set<Node> subgraphNodeList) {
        Tree tree = new Tree(storeFile, sampleIndex, subgraphNodeList);
        return (double) tree.noCorect() / sampleSetSize();
    }

    // enum all the subgraph
    // chosen indicates the list of chosen nodes
    // candidates is the candidate list, which consists of all the neighbors of currently chosen nodes
    // excludedList is the list of nodes that cannot be chosen
    private void enumSubgraph(Set<Node> candidates, Set<Node> chosen, Set<Node> excludedList, double threshold, int maxSize) {
        if (chosen.size() > maxSize) {
            return;
        }
        // if candidates is empty, print the result
        if (candidates.isEmpty()) {
//            if (chosen.size() >= 4) {
            double infoDensity = calcInfoDensity(chosen);
            if (infoDensity >= threshold) {
//                    int size = chosen.size();
//                    noSize[size]++;
//                    potentialPerSize[size] += infoDensity;
//                    noPotential[(int) (infoDensity * 100)]++;
                answerSet.addSubgraph(chosen, infoDensity);

                POGsize++;
                if (POGsize % 1000 == 0) {
                    System.out.println(POGsize + " : " + chosen.size() + " " + infoDensity + " Time: " + (System.currentTimeMillis() - time));
                }
            }
//            }
            return;
        }

        // pick a candidate from the candidate list
        Iterator<Node> iter = candidates.iterator();
        Node chosenNode = iter.next();

        // store the original candidate set
        Set<Node> org_candidates = new HashSet<Node>(candidates);

        // if chosen this candidate
        chosen.add(chosenNode);
        excludedList.add(chosenNode);
        candidates.addAll(chosenNode.neighbors);
        candidates.removeAll(excludedList);
        enumSubgraph(candidates, chosen, excludedList, threshold, maxSize);

        // restore the sets
        candidates = org_candidates;
        candidates.remove(chosenNode);
        chosen.remove(chosenNode);

        // else, candidate not chosen
        enumSubgraph(candidates, chosen, excludedList, threshold, maxSize);
        excludedList.remove(chosenNode);
    }

    private void newEnumSubgraph(Set<Node> excludedList, Set<Node> allCandidates, double threshold, int maxSize) {
        if (allCandidates.isEmpty()) {
            System.out.println("Graph is empty!");
            return;
        }
        // pick a candidate from the node list
        Iterator<Node> iter = allCandidates.iterator();
        Node chosenNode = iter.next();

        Set<Node> candidates = new HashSet<Node>();
        Set<Node> chosenList = new HashSet<Node>();

        chosenList.add(chosenNode);
        excludedList.add(chosenNode);
        candidates.addAll(chosenNode.neighbors);
        candidates.removeAll(excludedList);

        enumSubgraph(candidates, chosenList, excludedList, threshold, maxSize);

        candidates.clear();
        allCandidates.remove(chosenNode);
        newEnumSubgraph(excludedList, allCandidates, threshold, maxSize);
    }

    public void startEnumSubgraph(double threshold, int maxSize) {
        System.out.println("\nEnumerate all subgraph:\n");
        POGsize = 0;
        answerSet = new OptimallyDiscriminativeSet();

        Set<Node> excludedList = new HashSet<Node>();
        Set<Node> allCandidates = new HashSet<Node>(ppiGraph.nodes);
        time = System.currentTimeMillis();
        newEnumSubgraph(excludedList, allCandidates, threshold, maxSize);
        System.out.println("POG size: " + POGsize);
        System.out.println("Time: " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        answerSet.potentialStat();
        System.out.println("Time: " + (System.currentTimeMillis() - time));
    }

    public void startEnumSubgraph_Old(double threshold, int maxSize) {
        System.out.println("\nEnumerate all subgraph:\n");
        POGsize = 0;
        noSize = new int[100];
        noPotential = new int[101];
        potentialPerSize = new double[100];

        Set<Node> excludedList = new HashSet<Node>();
        Set<Node> allCandidates = new HashSet<Node>(ppiGraph.nodes);
        time = System.currentTimeMillis();
        newEnumSubgraph(excludedList, allCandidates, threshold, maxSize);
        System.out.println("POG size: " + POGsize);
        System.out.println("Time: " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        // print statistic info
        String strSizeHistogram = "";
        String strSize = "";
        String strPotentialPerSizeAvg = "";
        String strPotentialHistogram = "";
        String strPotential = "";

        for (int i = 0; i < noSize.length; i++) {
            if (noSize[i] == 0) {
                continue;
            }
            strSize += "," + Integer.toString(i);
            strSizeHistogram += "," + Integer.toString(noSize[i]);
            strPotentialPerSizeAvg += "," + Double.toString(potentialPerSize[i] / noSize[i]);
        }

        for (int i = 0; i < noPotential.length; i++) {
            if (noPotential[i] == 0) {
                continue;
            }
            strPotential += "," + Double.toString((double) i / 100.0);
            strPotentialHistogram += "," + Integer.toString(noPotential[i]);
        }

        System.err.println("Avg Potential persize:\n" + strSize + "\n" + strPotentialPerSizeAvg
                + "\nSize Histogram:\n" + strSizeHistogram);
        System.err.println("Potential Histogram:\n" + strPotential + "\n" + strPotentialHistogram);

        System.out.println("Time: " + (System.currentTimeMillis() - time));
    }

    public void statInfoGain() {
        System.err.println("Stat on infomation gain");
        PriorityQueue<Subgraph> pQueue = new PriorityQueue<Subgraph>();
        for (Node n : ppiGraph.nodes) {
            Set<Node> nodelist = new HashSet<Node>();
            nodelist.add(n);
            double infoGain = calcInfoDensity(nodelist);
            Subgraph s = new Subgraph(nodelist, infoGain, -1);
            pQueue.add(s);
        }

        String strInfoGain = "";
        while (!pQueue.isEmpty()) {
            Subgraph s = pQueue.poll();
            strInfoGain += Double.toString(s.score) + ",";
            s.print();
        }
        System.err.println("Done stat");
        System.err.println(strInfoGain);
    }

    public Set<Integer> getHighPotentialNodes(double threshold) {
        Set<Integer> highPotentialNodes = new HashSet<Integer>();
        for (Node n : ppiGraph.nodes) {
            Set<Node> nodelist = new HashSet<Node>();
            nodelist.add(n);
            double infoGain = calcInfoDensity(nodelist);
            if (infoGain > threshold) {
                highPotentialNodes.add(n.id);
            }
        }
        return highPotentialNodes;
    }

    public Set<Node> randomSubGraph(Set<Node> entireNodeSet, int maxSize) {
        Set<Node> nodeList = new HashSet<Node>();   // list of node of random subgraph
        Set<Node> additionalCandidates = new HashSet<Node>(); // list of candidate for nodes in subgraph

        // change entireNodeList to array
        ArrayList<Node> candidateList = new ArrayList<Node>(entireNodeSet);
        if (candidateList.isEmpty()) {
            System.out.println("There are no nodes");
            return null;
        }

        // choose randomly a node from candidate list and add to the subgraph
        Node chosenNode = picknRemoveRandomNode(candidateList);

//        // fix a start node for breast dataset
//        Node chosenNode = getNodeByLabel("1051");
//        candidateList.remove(chosenNode);
//        // end fix start node

        nodeList.add(chosenNode);

        // update candidate set
        additionalCandidates.addAll(chosenNode.neighbors);
        additionalCandidates.remove(chosenNode);

        candidateList = new ArrayList<Node>(additionalCandidates);

        while (nodeList.size() < maxSize && !candidateList.isEmpty()) {
            // choose randomly a node from candidate list and add to the subgraph
            chosenNode = picknRemoveRandomNode(candidateList);
            if (chosenNode == null) {
                return nodeList;
            }
            nodeList.add(chosenNode);

            // update candidate set
            additionalCandidates.clear();
            additionalCandidates.addAll(chosenNode.neighbors);
            additionalCandidates.removeAll(nodeList);
            additionalCandidates.removeAll(candidateList);
            candidateList.addAll(additionalCandidates);
        }
        return nodeList;
    }

    // given a node list
    // pick a random node from it
    private Node picknRemoveRandomNode(ArrayList<Node> nodeList) {
        if (nodeList.isEmpty()) {
            return null;
        }
        return nodeList.remove(generator.nextInt(nodeList.size()));
    }

    // given a node list
    // remove all nodes that do not appear in the sample list
    private Set<Node> pruneNodeList(Set<Node> nodeList) {
        Set<Node> prunedList = new HashSet<Node>();
        for (Node n : nodeList) {
            if (n.id < storeFile[0].length) {
                prunedList.add(n);
            }
        }
        return prunedList;
    }

    public double updateClassifier(Node editedFeature, boolean isAdd) {
        if (MetroSampler.useSVM) {
            backupSVM = new SVMHelper(currentSVM);
            Set<Node> newNodeList = new HashSet<Node>();
            newNodeList.addAll(currentSVM.getNodeList());
            if (isAdd) {
                newNodeList.add(editedFeature);
                currentSVM = new SVMHelper(storeFile, newNodeList);
            } else {
                newNodeList.remove(editedFeature);
                currentSVM = new SVMHelper(storeFile, newNodeList);
            }
            return currentSVM.getPotential();
        } else { // use NCDT
            backupTree = new Tree(currentTree);
            if (isAdd) {
                currentTree.addFeature(editedFeature, sampleIndex);
            } else {
                currentTree.removeFeature(editedFeature, sampleIndex);
            }

            return (double) currentTree.noCorect() / sampleSetSize();
            //        return currentTree.noCorect();
        }

    }

    public PogNode genRandSeedSubgraph(int maxSize) {
        Set<Node> subgraphNodeList = randomSubGraph(ppiGraph.nodes, maxSize);
        if (subgraphNodeList == null) {
            return null;
        }

        PogNode _return = null;
        if (MetroSampler.useSVM) {
            currentSVM = new SVMHelper(storeFile, subgraphNodeList);
            _return = new PogNode(subgraphNodeList,
                    currentSVM.getPotential(),
                    this);
        } else {
            currentTree = new Tree(storeFile, sampleIndex, subgraphNodeList);
            _return = new PogNode(subgraphNodeList,
                    (double) currentTree.noCorect() / sampleSetSize(),
                    //                currentTree.noCorect(),
                    this);

            if (!currentTree.useAllFeatures()) {
                System.err.println("Britney Spears told me that you seed is bad!");
            }
        }
        return _return;
    }

    public PogNode genRandSeedSubgraphSimple(int maxSize) {
        Set<Node> subgraphNodeList = randomSubGraph(ppiGraph.nodes, maxSize);

        // prune to make sure all features are in the sample list
        Set<Node> prunedSubgraphNodeList = pruneNodeList(subgraphNodeList);

        currentTree = new Tree(storeFile, sampleIndex, prunedSubgraphNodeList);
        PogNode _return = new PogNode(subgraphNodeList, (double) currentTree.noCorect() / sampleSetSize());

        return _return;
    }

    public void reverseChange() {
        if (MetroSampler.useSVM) {
            currentSVM = backupSVM;
        } else {
            currentTree = backupTree;
        }
    }

    public Set<Node> entireNodeList() {
        return ppiGraph.nodes;
    }

    public boolean useAllFeatures() {
        if (MetroSampler.useSVM) {
            return true;
        } else {
            return currentTree.useAllFeatures();
        }
    }

    public int noCorrect() {
        return currentTree.noCorect();
    }

    public double getInfoGainAdded(Node newFeature) {
        double _return = currentTree.calcTantativeInfoGain(newFeature, storeFile);
        return _return;
    }

    public double getInfoGainRemoved(Node feature) {
        return currentTree.getInfoGainRemoved(feature);
    }

    public int sampleSetSize() {
        return sampleIndex.size();
    }

    public double getBaseline() {
        return deltaBaseline;
    }

    public SimpleTree buildSimpleTree(Set<Node> subgraphNodeList) {
        Tree tree = new Tree(storeFile, sampleIndex, subgraphNodeList);
        return new SimpleTree(tree);
    }

    // load data from files
    private HashMap<String, Integer> loadSamples(String fileProExp, String fileSampleLabel) {
        Boolean b1 = true;
        int linenum = 0; //to keep track of the number of rows in the file (which is equal to the number of samples)

        // map protein name to an index
        HashMap<String, Integer> _protidMap = new HashMap();

        //to store class value true or false for every sampl
        HashMap<String, Boolean> SampleBinary = new HashMap();

        //this reads the entire file nto an arraylist of arraylists, this is used just once in th beginning
        ArrayList<ArrayList<String>> sampleInfoStore = new ArrayList<ArrayList<String>>();

//        if (maxNoProtein <= 0){
//            maxNoProtein = Integer.MAX_VALUE;
//        }

        try {
            BufferedReader br1 = new BufferedReader(new FileReader(fileProExp));
            int count = 1;
            while (br1.ready()) {
                String[] X = br1.readLine().split("\t");
                ArrayList<String> sampleInfoLine = new ArrayList();

                for (int i = 0; i < X.length - 1; i++) {
//                for (int i = 0; i < maxNoProtein; i++) {
                    sampleInfoLine.add(X[i].trim());
//                    if (!protidMap.containsKey(X[i + 1].trim()) && protidMap.size() < maxNoProtein) {
                    if (!_protidMap.containsKey(X[i + 1].trim())) {
                        _protidMap.put(X[i + 1].trim(), count++);
                    }
                }
                sampleInfoLine.add(X[X.length - 1]);  //adding the last protein
                linenum++;
                sampleInfoStore.add(sampleInfoLine);
            }
        } catch (Exception e) {
            System.out.println("Exeption: " + e);
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileSampleLabel));
            while (br.ready()) {
                String X = br.readLine();
                String[] Y = X.split("\t");
                if (Y[1].trim().equals("0")) {
                    b1 = false;
                } else if (Y[1].trim().equals("1")) {
                    b1 = true;
                }
                SampleBinary.put(Y[0].trim(), b1);

            }
        } catch (Exception e) {
            System.out.println(e);
        }


        storeFile = new boolean[linenum][_protidMap.size() + 1];


        for (int i = 0; i < linenum; i++) {
            //initializing all protein expression values to false
            Arrays.fill(storeFile[i], false);
        }

        int countTrue = 0;
        //filling up storeFile:
        for (int i = 0; i < linenum; i++) {
            String Samp = sampleInfoStore.get(i).get(0); //getting sample name
            storeFile[i][0] = SampleBinary.get(Samp);  //the class labels
            if (storeFile[i][0]) {
                countTrue++;
            }

            for (int j = 0; j < sampleInfoStore.get(i).size() - 1; j++) {
                String prot = sampleInfoStore.get(i).get(j + 1);  //getting the 1 vlue proteins for this sample
                // System.out.println(String.valueOf(protid.get(prot)));
                Integer k = _protidMap.get(prot);  //getting the index of this protein
                if (k != null) {
                    storeFile[i][k] = true;
                }
            }
        }
        System.out.println("Num Proteins " + _protidMap.size()
                +". Num events: " + storeFile.length);

        for (int i = 0; i < sampleInfoStore.size(); i++) {
            sampleIndex.add(i);
        }

        if (countTrue >= storeFile.length - countTrue) {
            deltaBaseline = (double) (countTrue) / storeFile.length;
        } else {
            deltaBaseline = 1.0 - (double) countTrue / storeFile.length;
        }

        return _protidMap;
    }

    // load data from files
    private HashMap<String, Integer> loadSamplesRandLabel(String fileProExp) {
        Boolean b1 = true;
        int linenum = 0; //to keep track of the number of rows in the file (which is equal to the number of samples)

        // map protein name to an index
        HashMap<String, Integer> _protidMap = new HashMap();

        //this reads the entire file nto an arraylist of arraylists, this is used just once in th beginning
        ArrayList<ArrayList<String>> sampleInfoStore = new ArrayList<ArrayList<String>>();

//        if (maxNoProtein <= 0){
//            maxNoProtein = Integer.MAX_VALUE;
//        }

        try {
            BufferedReader br1 = new BufferedReader(new FileReader(fileProExp));
            int count = 1;
            while (br1.ready()) {
                String[] X = br1.readLine().split("\t");
                ArrayList<String> sampleInfoLine = new ArrayList();

                for (int i = 0; i < X.length - 1; i++) {
//                for (int i = 0; i < maxNoProtein; i++) {
                    sampleInfoLine.add(X[i].trim());
//                    if (!protidMap.containsKey(X[i + 1].trim()) && protidMap.size() < maxNoProtein) {
                    if (!_protidMap.containsKey(X[i + 1].trim())) {
                        _protidMap.put(X[i + 1].trim(), count++);
                    }
                }
                sampleInfoLine.add(X[X.length - 1]);  //adding the last protein
                linenum++;
                sampleInfoStore.add(sampleInfoLine);
            }
        } catch (Exception e) {
            System.out.println("Exeption: " + e);
        }

        // randomly choose half of the samples as positive and
        // half as negative samples
        int countTrue = sampleInfoStore.size() / 2;
        Set<Integer> positiveSamples = new HashSet<Integer>();
        while (positiveSamples.size() < countTrue) {
            int k = generator.nextInt(sampleInfoStore.size());
            positiveSamples.add(k);
        }

        storeFile = new boolean[linenum][_protidMap.size() + 1];


        for (int i = 0; i < linenum; i++) {
            //initializing all protein expression values to false
            Arrays.fill(storeFile[i], false);
        }

        //filling up storeFile:
        for (int i = 0; i < linenum; i++) {
            if (positiveSamples.contains(i)) {
                storeFile[i][0] = true;  //the class labels
            }
            for (int j = 0; j < sampleInfoStore.get(i).size() - 1; j++) {
                String prot = sampleInfoStore.get(i).get(j + 1);  //getting the 1 vlue proteins for this sample
                // System.out.println(String.valueOf(protid.get(prot)));
                Integer k = _protidMap.get(prot);  //getting the index of this protein
                if (k != null) {
                    storeFile[i][k] = true;
                }
            }
        }
        System.out.println("Num Proteins " + _protidMap.size()
                +". Num events: " + storeFile.length);

        for (int i = 0; i < sampleInfoStore.size(); i++) {
            sampleIndex.add(i);
        }

        if (countTrue >= storeFile.length - countTrue) {
            deltaBaseline = (double) (countTrue) / storeFile.length;
        } else {
            deltaBaseline = 1.0 - (double) countTrue / storeFile.length;
        }

        return _protidMap;
    }

    // load data from files
    private HashMap<String, Integer> loadSamplesRandExpLevel(String fileProExp, String fileSampleLabel) {
        Boolean b1 = true;
        int linenum = 0; //to keep track of the number of rows in the file (which is equal to the number of samples)

        // map protein name to an index
        HashMap<String, Integer> _protidMap = new HashMap();

        //to store class value true or false for every sampl
        HashMap<String, Boolean> SampleBinary = new HashMap();

        //this reads the entire file nto an arraylist of arraylists, this is used just once in th beginning
        ArrayList<ArrayList<String>> sampleInfoStore = new ArrayList<ArrayList<String>>();

        try {
            BufferedReader br1 = new BufferedReader(new FileReader(fileProExp));
            int count = 1;
            while (br1.ready()) {
                String[] X = br1.readLine().split("\t");
                ArrayList<String> sampleInfoLine = new ArrayList();

                for (int i = 0; i < X.length - 1; i++) {
                    sampleInfoLine.add(X[i].trim());
                    if (!_protidMap.containsKey(X[i + 1].trim())) {
                        _protidMap.put(X[i + 1].trim(), count++);
                    }
                }
                sampleInfoLine.add(X[X.length - 1]);  //adding the last protein
                linenum++;
                sampleInfoStore.add(sampleInfoLine);
            }
        } catch (Exception e) {
            System.out.println("Exeption: " + e);
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileSampleLabel));
            while (br.ready()) {
                String X = br.readLine();
                String[] Y = X.split("\t");
                if (Y[1].trim().equals("0")) {
                    b1 = false;
                } else if (Y[1].trim().equals("1")) {
                    b1 = true;
                }
                SampleBinary.put(Y[0].trim(), b1);

            }
        } catch (Exception e) {
            System.out.println(e);
        }


        storeFile = new boolean[linenum][_protidMap.size() + 1];


        for (int i = 0; i < linenum; i++) {
            //initializing all protein expression values to false
            Arrays.fill(storeFile[i], false);
        }

        int countTrue = 0;
        //filling up storeFile:
        for (int i = 0; i < linenum; i++) {
            String Samp = sampleInfoStore.get(i).get(0); //getting sample name
            storeFile[i][0] = SampleBinary.get(Samp);  //the class labels
            if (storeFile[i][0]) {
                countTrue++;
            }

            // randomly pick the same no of overexpressed proteins for each sample
            // as in the original dataset
            Set<Integer> positiveSamples = new HashSet<Integer>();
            while (positiveSamples.size() < sampleInfoStore.get(i).size()) {
                int k = generator.nextInt(storeFile[i].length);
                positiveSamples.add(k);
            }

            for (Integer j : positiveSamples) {
                storeFile[i][j] = true;
            }
        }
        System.out.println("Num Proteins " + _protidMap.size()
                +". Num events: " + storeFile.length);

        for (int i = 0; i < sampleInfoStore.size(); i++) {
            sampleIndex.add(i);
        }

        if (countTrue >= storeFile.length - countTrue) {
            deltaBaseline = (double) (countTrue) / storeFile.length;
        } else {
            deltaBaseline = 1.0 - (double) countTrue / storeFile.length;
        }

        return _protidMap;
    }

    // load data from files
    private HashMap<String, Integer> loadSamples(String fileProExp, String fileSampleLabel, int maxNoProtein) {
        Boolean b1 = true;
        int linenum = 0; //to keep track of the number of rows in the file (which is equal to the number of samples)

        // map protein name to an index
        HashMap<String, Integer> _protidMap = new HashMap();

        //to store class value true or false for every sampl
        HashMap<String, Boolean> SampleBinary = new HashMap();

        //this reads the entire file nto an arraylist of arraylists, this is used just once in th beginning
        ArrayList<ArrayList<String>> sampleInfoStore = new ArrayList<ArrayList<String>>();

        if (maxNoProtein <= 0) {
            maxNoProtein = Integer.MAX_VALUE;
        }

        try {
            BufferedReader br1 = new BufferedReader(new FileReader(fileProExp));
            int count = 1;
            while (br1.ready()) {
                String[] X = br1.readLine().split("\t");
                ArrayList<String> sampleInfoLine = new ArrayList();

                for (int i = 0; i < X.length - 1; i++) {
//                for (int i = 0; i < maxNoProtein; i++) {
                    sampleInfoLine.add(X[i].trim());
                    if (!_protidMap.containsKey(X[i + 1].trim()) && _protidMap.size() < maxNoProtein) {
//                    if (!_protidMap.containsKey(X[i + 1].trim())) {
                        _protidMap.put(X[i + 1].trim(), count++);
                    }
                }
                sampleInfoLine.add(X[X.length - 1]);  //adding the last protein
                linenum++;
                sampleInfoStore.add(sampleInfoLine);
            }
        } catch (Exception e) {
            System.out.println("Exeption: " + e);
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileSampleLabel));
            while (br.ready()) {
                String X = br.readLine();
                String[] Y = X.split("\t");
                if (Y[1].trim().equals("0")) {
                    b1 = false;
                } else if (Y[1].trim().equals("1")) {
                    b1 = true;
                }
                SampleBinary.put(Y[0].trim(), b1);

            }
        } catch (Exception e) {
            System.out.println(e);
        }


        storeFile = new boolean[linenum][_protidMap.size() + 1];


        for (int i = 0; i < linenum; i++) {
            //initializing all protein expression values to false
            Arrays.fill(storeFile[i], false);
        }

        int countTrue = 0;
        //filling up storeFile:
        for (int i = 0; i < linenum; i++) {
            String Samp = sampleInfoStore.get(i).get(0); //getting sample name
            storeFile[i][0] = SampleBinary.get(Samp);  //the class labels
            if (storeFile[i][0]) {
                countTrue++;
            }

            for (int j = 0; j < sampleInfoStore.get(i).size() - 1; j++) {
                String prot = sampleInfoStore.get(i).get(j + 1);  //getting the 1 vlue proteins for this sample
                // System.out.println(String.valueOf(_protid.get(prot)));
                Integer k = _protidMap.get(prot);  //getting the index of this protein
                if (k != null) {
                    storeFile[i][k] = true;
                }
            }
        }
        System.out.println("Num Proteins " + _protidMap.size()
                +". Num events: " + storeFile.length);

        for (int i = 0; i < sampleInfoStore.size(); i++) {
            sampleIndex.add(i);
        }

        if (countTrue >= storeFile.length - countTrue) {
            deltaBaseline = (double) (countTrue) / storeFile.length;
        } else {
            deltaBaseline = 1.0 - (double) countTrue / storeFile.length;
        }

        return _protidMap;
    }

    public void loadPrunedGraph(String filePPI, HashMap<String, Integer> _protidMap, int maxNoProtein) {
        //building the PPI network
        ppiGraph = new Graph();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePPI));

            while (br.ready()) {
                String X = br.readLine();
                String[] Y = X.split("\t");

                String A = Y[0].trim();
                String B = Y[1].trim();

//                ppiGraph.addEdge(A, B, _protidMap);
//                ppiGraph.addEdgeMaxSize(A, B, _protidMap, maxNoProtein);
                
                // for Nazli
                ppiGraph.addArbitraryEdge(A, B, _protidMap);
                // end for Nazli

                //System.out.println("One run of graph building completed");
            }

        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Pruned Graphs. Num nodes: " + ppiGraph.nodes.size()
                + ". Num edges: " + ppiGraph.noEdges());
    }

    public void loadPrunedGraph(String filePPI, HashMap<String, Integer> _protidMap) {
        //building the PPI network
        ppiGraph = new Graph();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePPI));

            while (br.ready()) {
                String X = br.readLine();
                String[] Y = X.split("\t");

                String A = Y[0].trim();
                String B = Y[1].trim();

//                ppiGraph.addEdge(A, B, _protidMap);
                // for Nazli
                ppiGraph.addArbitraryEdge(A, B, _protidMap);
                // end for Nazli

                //System.out.println("One run of graph building completed");
            }

        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Pruned Graphs. Num nodes: " + ppiGraph.nodes.size()
                + ". Num edges: " + ppiGraph.noEdges());
    }

    //load graph with edge noise
    private void loadRandomGraph(String filePPI, HashMap<String, Integer> _protidMap, double noise) {
        //building the PPI network
        ppiGraph = new Graph();
        Random random = new Random();
        HashSet<String> nodes = new HashSet<String>();
        int numEdges = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePPI));

            while (br.ready()) {
                String X = br.readLine();
                String[] Y = X.split("\t");

                String A = Y[0].trim();
                String B = Y[1].trim();
                if (!nodes.contains(A)) {
                    nodes.add(A);
                }
                if (!nodes.contains(B)) {
                    nodes.add(B);
                }
                if (random.nextDouble() > noise) {
                    //ppiGraph.addEdge(A, B, _protidMap);
                    // for Nazli
                    ppiGraph.addArbitraryEdge(A, B, protidMap);
                    // end for Nazli
                } else {
                    numEdges++;
                }
                // 
                //System.out.println("One run of graph building completed");
            }
            String[] a = new String[1];
            String[] nArray = nodes.toArray(a);
            for (int i = 0; i < numEdges; i++) {
                String A = nArray[random.nextInt(nArray.length)];
                String B = nArray[random.nextInt(nArray.length)];

                //ppiGraph.addEdge(A, B, _protidMap);
                // for Nazli
                ppiGraph.addArbitraryEdge(A, B, protidMap);
                // end for Nazli
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Rand Graphs. Num nodes: " + ppiGraph.nodes.size()
                + ". Num edges: " + ppiGraph.noEdges());

    }

    public void loadFullGraph(String filePPI, HashMap<String, Integer> _protidMap) {
        //building the PPI network
        ppiGraph = new Graph();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePPI));

            while (br.ready()) {
                String X = br.readLine();
                String[] Y = X.split("\t");

                String A = Y[0].trim();
                String B = Y[1].trim();

//                ppiGraph.addArbitraryEdge(A, B, _protidMap);
                ppiGraph.addEdge(A, B, _protidMap);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Full Graph. Num nodes: " + ppiGraph.nodes.size()
                + ". Num edges: " + ppiGraph.noEdges());
    }

    // test purpose
    public Tree testBuildDecisionTree() {
        HashSet ids = new HashSet();
        //ids.add(1);
        ids.add(2);
        ids.add(4);
        ids.add(3);
        Set<Node> subgraph = findNode(ppiGraph.nodes, ids);

        Tree decTree = new Tree(storeFile, sampleIndex, subgraph);
        decTree.PreOrderTraversal();

        Node newFeature = findNode(ppiGraph.nodes, 1);
        subgraph.add(newFeature);

        decTree.addFeature(newFeature, sampleIndex);
        decTree.PreOrderTraversal();

        subgraph.remove(newFeature);
        decTree.removeFeature(newFeature, sampleIndex);
        decTree.PreOrderTraversal();

        // t.computeQuality();
        System.out.println("Correct no of the subgraph is " + decTree.noCorect());

        return decTree;
    }

    // test purpose
    private Set<Node> findNode(Set<Node> nodeList, Set<Integer> ids) {
        Set<Node> subgraph = new HashSet<Node>();
        for (Node n : nodeList) {
            if (ids.contains(n.id)) {
                subgraph.add(n);
            }
        }
        return subgraph;
    }

    // test purpose
    private Node findNode(Set<Node> nodeList, int id) {
        for (Node n : nodeList) {
            if (id == n.id) {
                return n;
            }
        }
        return null;
    }

    // given the original label in the data set file
    // return the Node object
    public Node getNodeByLabel(String label) {
        Integer id = protidMap.get(label);
        if (id == null) {
            return null;
        }
        return ppiGraph.getNodebyID(id.intValue());
    }

    public void buildTreeStatPair(Set<Node> subgraphNodeList, HashMap<GenePair, GenePairQuality> genePairMap) {
        Tree tree = new Tree(storeFile, sampleIndex, subgraphNodeList);
        tree.statPair(genePairMap);
    }

    public static void convertFormat(String fileProExp, String fileSampleLabel, Set<Integer> removedNodes, int type) {
        Boolean b1 = true;
        int linenum = 0; //to keep track of the number of rows in the file (which is equal to the number of samples)

        // map protein name to an index
        HashMap<String, Integer> _protidMap = new HashMap();

        //to store class value true or false for every sampl
        HashMap<String, Boolean> SampleBinary = new HashMap();

        //this reads the entire file nto an arraylist of arraylists, this is used just once in th beginning
        ArrayList<ArrayList<String>> sampleInfoStore = new ArrayList<ArrayList<String>>();

//        if (maxNoProtein <= 0){
//            maxNoProtein = Integer.MAX_VALUE;
//        }

        try {
            BufferedReader br1 = new BufferedReader(new FileReader(fileProExp));
            int count = 1;
            while (br1.ready()) {
                String[] X = br1.readLine().split("\t");
                ArrayList<String> sampleInfoLine = new ArrayList();

                for (int i = 0; i < X.length - 1; i++) {
//                for (int i = 0; i < maxNoProtein; i++) {
                    sampleInfoLine.add(X[i].trim());
//                    if (!protidMap.containsKey(X[i + 1].trim()) && protidMap.size() < maxNoProtein) {
                    if (!_protidMap.containsKey(X[i + 1].trim())) {
                        _protidMap.put(X[i + 1].trim(), count++);
                    }
                }
                sampleInfoLine.add(X[X.length - 1]);  //adding the last protein
                linenum++;
                sampleInfoStore.add(sampleInfoLine);
            }
        } catch (Exception e) {
            System.out.println("Exeption: " + e);
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileSampleLabel));
            while (br.ready()) {
                String X = br.readLine();
                String[] Y = X.split("\t");
                if (Y[1].trim().equals("0")) {
                    b1 = false;
                } else if (Y[1].trim().equals("1")) {
                    b1 = true;
                }
                SampleBinary.put(Y[0].trim(), b1);

            }
        } catch (Exception e) {
            System.out.println(e);
        }

        if (type == 1) { // svm
            boolean[] feas = new boolean[_protidMap.size()];
            //filling up storeFile:
            for (int i = 0; i < linenum; i++) {
                Arrays.fill(feas, false);
                String Samp = sampleInfoStore.get(i).get(0); //getting sample name

                if (SampleBinary.get(Samp)) {
                    System.out.print("+1 ");
                } else {
                    System.out.print("-1 ");
                }

                for (int j = 0; j < sampleInfoStore.get(i).size() - 1; j++) {
                    String prot = sampleInfoStore.get(i).get(j + 1);  //getting the 1 vlue proteins for this sample

//                System.out.print(prot + ":1");

                    Integer k = _protidMap.get(prot) - 1;  //getting the index of this protein
                    if (k != null) {
                        feas[k] = true;
                    }
                }
                for (int j = 0; j < feas.length; j++) {
                    if (removedNodes == null || !removedNodes.contains(j)) {
                        if (feas[j]) {
                            System.out.print((j + 1) + ":1 ");
                        } else {
                            System.out.print((j + 1) + ":0 ");
                        }
                    }
                }
                System.out.println();
            }
        } else { // csv
            for (int i = 0; i < _protidMap.size() - removedNodes.size(); i++) {
                System.out.print((i + 1) + ",");
            }
            System.out.println("Class");
            boolean[] feas = new boolean[_protidMap.size()];
            //filling up storeFile:
            for (int i = 0; i < linenum; i++) {
                Arrays.fill(feas, false);
                for (int j = 0; j < sampleInfoStore.get(i).size() - 1; j++) {
                    String prot = sampleInfoStore.get(i).get(j + 1);  //getting the 1 vlue proteins for this sample

                    Integer k = _protidMap.get(prot);  //getting the index of this protein
                    if (k != null) {
                        feas[k - 1] = true;
                    }
                }
                for (int j = 0; j < feas.length; j++) {
                    if (removedNodes == null || !removedNodes.contains(j)) {
                        if (feas[j]) {
                            System.out.print("1,");
                        } else {
                            System.out.print("0,");
                        }
                    }
                }

                String Samp = sampleInfoStore.get(i).get(0); //getting sample name

                if (SampleBinary.get(Samp)) {
                    System.out.println("yes");
                } else {
                    System.out.println("no");
                }
            }

        }
    }

    public boolean[][] getStoreFile() {
        return storeFile;
    }

    public SVMHelper getCurrentSVM() {
        return currentSVM;
    }

    public void deleteEdge(int id1, int id2) {
        ppiGraph.deleteEdge(id1, id2);
    }

    public SVMHelper buildSVM(Set<Node> subgraphNodeList) {
        return new SVMHelper(storeFile, subgraphNodeList);
    }

	@Override
	public void reverseChangeDivrank() {
		// TODO Auto-generated method stub
		
	}
}
