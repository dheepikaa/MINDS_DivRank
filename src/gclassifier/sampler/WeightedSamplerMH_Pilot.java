/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.sampler;

import gclassifier.Node;
import gclassifier.Tree;
import gclassifier.TreeQualifier;
import gclassifier.answer.OptimallyDiscriminativeSet;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import org.omg.CORBA.INTF_REPOS;

/**
 *
 * @author minhhx
 */
public class WeightedSamplerMH_Pilot extends MetroSampler {

    public double inProb; // prob to move toward the border
    public static double minPotentialRatio;// for deciding when to stop pilot sampling thresholdPotential/maxPotential
    public static double minDeltaPotentialRatio;// for deciding when to stop pilot sampling deltaPotential/maxPotential
    public WeightedSamplerMH_Pilot(TreeQualifier _qualifier) {
        qualifier = _qualifier;
        pogVisitCount = new HashMap<PogNode, Integer>();
        answerSet = new OptimallyDiscriminativeSet();
        useGamma = true; // use gamma for border prob, in new formula
        limitEditMap = false;
        inProb = 0.99;
        minPotentialRatio = 0.95;
        minDeltaPotentialRatio = 0.0005;
    }

    public void printParam() {
        System.out.println("inProb = " + inProb + "\tminPotentialRatio = " + minPotentialRatio
                + "\tminDeltaPotentialRatio" + minDeltaPotentialRatio);
    }

    // sample a number of graphs noGraphsPerSize for each size, average the potential
    // stop when the average potential of a size >= 0.99
    public void pilot(int noGraphsPerSize) {
        System.out.println("Pilot sampling of potential per size (" +noGraphsPerSize + "each)...");
        System.out.println("Size\tAvgPotential\tTime");

        long time = System.currentTimeMillis();
        int size = 1;
        double potential = 0;
        while (true) {
            for (int i = 0; i < noGraphsPerSize; i++){
                PogNode p = qualifier.genRandSeedSubgraphSimple(size);
            }
            potential = Tree.getAvgPotential(size);
            System.out.println(size + "\t" + potential + "\t" + (System.currentTimeMillis() - time));
            
            // stop if average potential is too small
//            if (potential - Tree.getAvgPotential(size-1) < minDeltaPotentialRatio){
//                break;
//        }
            if (potential > 1.0 - minDeltaPotentialRatio){
                break;
            }
            size++;            
        }
        System.out.println("MaxPotential = " + Tree.maxPotential + "\tMinPotential = " + Tree.minPotential);
    }   
       
    @Override
    public void sample(int maxIter, int seedSize, double threshold) {
        theta = threshold;
        sample(maxIter, seedSize);
    }

    @Override
    public void sample(int maxIter, int seedSize) {
        // pilot sampling to gather stat about potential
        pilot(100);
        
        // reset sampling parameter
        totalVisitCount = 0;
        pogVisitCount.clear();
        double averagePogSize = 0;
        long time = System.currentTimeMillis();
        System.out.println("Sampling: New Formula");
        printParam();
        System.out.println("#Accepted\t#UniqueAccepted\t#Rejected\tAvgSize\tcurPotential\tTime");
        // generate any frequent pattern
        PogNode p = qualifier.genRandSeedSubgraph(seedSize);
        if (p == null) {
            return;
        }

        int iter = 0;
        double acceptProb = 0.0;
        int skipCounter = 0;

        while (iter < maxIter) {
            // pick the next node
            PogNode q = p.chooseNeighborPilot(qualifier, generator, inProb);
            if (q == null) {
                skipCounter++;
                continue;
            }
            // if at null node --> just move up
            if (p.size() == 0) {
                acceptProb = 1.0;
            } else {
                double q2p = q.getTransitProb_Pilot(p, inProb);
                double p2q = p.getTransitProb_Pilot(q, inProb);
                if (q.size() == 0) {
                    q2p = 1.0;
                }

                // calc the acceptance prob
                acceptProb = Math.min(1.0, q.getPotential_Pilot() / p.getPotential_Pilot() / p2q * q2p);
            }

            // check if q is accepted as the next state
            // uniform(0,1) <= acceptProb
            double uniform = generator.nextDouble();
            if (uniform <= acceptProb) {  // move from p to q
                p = q;

                // update the visit count of q
                Integer curCount = pogVisitCount.get(q);
                if (curCount != null) {
                    curCount++;
                } else {
                    curCount = 1;
                }
                pogVisitCount.put(q, curCount);

                iter++;
                averagePogSize += p.size();
                if (iter % 500 == 0) {
                    System.out.println(iter
                            + "\t" + pogVisitCount.size()
                            + "\t" + skipCounter
                            + "\t" + averagePogSize / iter
                            + "\t" + p.infoDensity
                            + "\t" + (System.currentTimeMillis() - time));
                }
            } else {
                // skip the subgraph
                qualifier.reverseChange();
                skipCounter++;
            }
        }
        totalVisitCount = iter;
//        System.out.println("Iter: " + iter + " POG size explored : " + pogVisitCount.size() + " He has skipped: " + skipCounter);
    }
}
