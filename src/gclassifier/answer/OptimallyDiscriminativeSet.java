package gclassifier.answer;

import gclassifier.Node;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

public class OptimallyDiscriminativeSet {

    private HashMap<Integer, Subgraph> answers;
    private HashMap<Integer, HashSet<Integer>> index;
    int idcount;

    public OptimallyDiscriminativeSet() {
        answers = new HashMap();
        index = new HashMap();
        idcount = 0;
    }
    
    public void clear(){
        answers.clear();
        index.clear();
        idcount = 0;
    }
    
    public int size(){
        return answers.size();
    }

    public PriorityQueue<Subgraph> sortAnswer(){
        PriorityQueue<Subgraph> pQueue = new PriorityQueue<Subgraph>();
        pQueue.addAll(answers.values());
        return pQueue;
    }

    public Collection<Subgraph> getAnswer(){
        return (Collection<Subgraph>) answers.values();
    }

    public void addSubgraph(Set<Node> subgraph, double score) {
        
    	if (optimal(subgraph, score)) {
            
        	Set<Integer> cands = getSuperGraphs(subgraph, score);
            for (int id : cands) {
                Subgraph g = answers.get(id);
                for (Node n : g.nodes) {
                    index.get(n.id).remove(id);
                }
                answers.remove(id);

            }
           
            Subgraph g = new Subgraph(subgraph, score, idcount++);

            for (Node n : subgraph) {
                if (!index.containsKey(n.id)) {
                    index.put(n.id, new HashSet<Integer>());
                }
                index.get(n.id).add(g.id);
            }
            answers.put(g.id, g);
        }

    }

    public void print() {
        for (int id : answers.keySet()) {
            answers.get(id).print();
        }
    }

    public void potentialStat(){
        int[] noSize = new int[100];
        double[] potentialPerSize = new double[100];
        double minDensity = 2.0, maxDensity = -1.0;
        double density = 0;
        Arrays.fill(noSize, 0);
        Arrays.fill(potentialPerSize, 0);
        for (int id : answers.keySet()) {
            Subgraph g = answers.get(id);
            int size = g.nodes.size();
            if (size >= 100)
                size = 99;
            noSize[size]++;
            potentialPerSize[size] += g.score;
            if (g.size()>0){
                density = g.score/g.size();
                if (minDensity > density){
                    minDensity = density;
                }
                if (maxDensity < density){
                    maxDensity = density;
                }
            }
        }

        String strSize = "";
        String strAvgPotentialPerSize = "";
        for (int size = 0; size < noSize.length; size++){
            if (noSize[size]==0)
                continue;
            strSize += "," + Integer.toString(size);
            strAvgPotentialPerSize += "," + Double.toString((double)potentialPerSize[size]/noSize[size]);
        }
        System.err.println("Avg Potential per size:\n" + strSize + "\n" + strAvgPotentialPerSize);
        System.err.println("\nMaxDensity:" + maxDensity + "\tMinDensity:" + minDensity + "\tDensitySpan:" + (maxDensity - minDensity) + "\n");
    }

    public void infoDensityStat(double threshold) {
        double minDensity = 2.0;
        double maxDensity = -1.0;
        double avgDensity = 0.0;
        double avgScore = 0.0;
        double minScore = 2.0;
        double maxScore = -1.0;

//        OptimallyDiscriminativeSet _answerSet = new OptimallyDiscriminativeSet();
//
//        for (Map.Entry<PogNode, Integer> entry : pogVisitCount.entrySet()) {
//            PogNode key = entry.getKey();
//            if (key.size() == 0 || key.infoDensity < threshold) {
//                continue;
//            }
//
//            _answerSet.addSubgraph(key.nodeList(), key.infoDensity);
//        }

        int count = 0;
        for (Subgraph s : getAnswer()) {
            double density = s.score / s.size();
            avgScore += s.score;
            count++;
            avgDensity += density;
            if (minDensity > density) {
                minDensity = density;
            }
            if (maxDensity < density) {
                maxDensity = density;
            }
            if (maxScore < s.score) {
                maxScore = s.score;
            }
            if (minScore > s.score) {
                minScore = s.score;
            }
            if (s.score == 1.0 && s.size() == 1) {
                System.err.print("-------------------OMG");
                s.print();
            }
            if (density == 1.0 && s.size() == 1) {
                System.err.print("-------------------Hello Motor");
                s.print();
            }
        }

        avgDensity /= count;
        avgScore /= count;

        System.err.println("\tminDensity: " + minDensity + "\tmaxDensity: " + maxDensity
                + "\tDensitySpan: " + (maxDensity - minDensity)
                + "\tavgD: " + avgDensity
                + "\tminP: " + minScore
                + "\tmaxP: " + maxScore
                + "\tavgP: " + avgScore
                + "\tsize: " + count);
//        System.err.println("minDensity: " + minDensity + " maxDensity: " + maxDensity
//                + " avgDensity: " + avgDensity + " avgScore: " + avgScore
//                + " answerSet size: " + count);
    }
    
    private boolean optimal(Set<Node> subgraph, double score) {
        if (answers.size() == 0) {
            return true;
        }
        HashMap<Integer, Integer> cands = new HashMap();
        for (Node n : subgraph) {
            HashSet<Integer> ids = index.get(n.id);
            if (ids == null) {
                continue;
            }
            for (Integer id : ids) {
                Subgraph g = answers.get(id);
                if (g.size() <= subgraph.size() && g.score >= score) {
                    if (cands.containsKey(g.id)) {
                        cands.put(g.id, cands.get(g.id) + 1);
                    } else {
                        cands.put(g.id, 1);
                    }
                }
            }
        }
        //System.out.println(cands.toString());
        for (Integer id : cands.keySet()) {
            Subgraph g = answers.get(id);
            if (g.size() == cands.get(g.id) && g.score >= score) {
                return false;
            }
        }
        return true;
    }

    private Set<Integer> getSuperGraphs(Set<Node> subgraph, double score) {
        HashSet<Integer> cands = new HashSet();
        if (answers.size() == 0) {
            return cands;
        }

        for (Node n : subgraph) {
            HashSet<Integer> ids = index.get(n.id);
            if (ids == null) {
                return new HashSet<Integer>();
            }
            if (cands.size() == 0) {
                for (int id : ids) {
                    Subgraph g = answers.get(id);
                    if (g.size() >= subgraph.size() && g.score <= score) {
                        cands.add(id);
                    }
                }
            } else {
                HashSet<Integer> _cands = new HashSet(cands);
                for (int id : _cands) {
                    if (!ids.contains(id)) {
                        cands.remove(id);
                    }
                }
            }
            if (cands.size() == 0) {
                break;
            }

        }
        return cands;
    }
    
    public Set<Node> findMostVisitedNodes(int noNodes){
        HashMap<Node, Integer> countNode = new HashMap<Node, Integer>(); // map Node to # visits
        for (Subgraph g : answers.values()){
            for (Node n : g.nodes){
                if (countNode.containsKey(n)){
                    countNode.put(n, countNode.get(n)+1);
                } else {
                    countNode.put(n, 1);
                }
            }
        }
        
        // find the highest visited nodes
        Set<Node> bestNodes = new HashSet<Node>();
        PriorityQueue<NodeRanker> ranker = new PriorityQueue<NodeRanker>();
        for (Node n : countNode.keySet()){
            ranker.add(new NodeRanker(n, countNode.get(n)));
        }
        int i = 0;
        while (!ranker.isEmpty() && i < noNodes){
            bestNodes.add(ranker.poll().n);
            i++;
        }
        
        return bestNodes;
    }

    public static void main(String[] args) {
        OptimallyDiscriminativeSet ans = new OptimallyDiscriminativeSet();
        HashSet<Node> sg1 = new HashSet();
        HashSet<Node> sg2 = new HashSet();
        HashSet<Node> sg3 = new HashSet();
        HashSet<Node> sg4 = new HashSet();
        Node n1 = new Node(1, "1");
        Node n2 = new Node(2, "2");
        Node n3 = new Node(3, "3");
        Node n4 = new Node(4, "4");
        Node n5 = new Node(5, "5");
        sg1.add(n1);
        sg1.add(n2);
        sg1.add(n3);
        sg2.add(n1);
        sg2.add(n2);
        sg2.add(n3);
        sg2.add(n4);
        sg3.add(n1);
        sg3.add(n2);
        sg3.add(n3);
        sg3.add(n5);
        sg4.add(n1);
        sg4.add(n2);
        ans.addSubgraph(sg2, 3);
        ans.addSubgraph(sg3, 4);
        ans.addSubgraph(sg1, 3);
        ans.addSubgraph(sg4, 2);
        ans.addSubgraph(sg2, 4);

        ans.print();


    }
}
