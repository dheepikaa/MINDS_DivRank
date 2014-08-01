/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.sampler;

import gclassifier.Node;
import gclassifier.TreeQualifier;
import gclassifier.answer.OptimallyDiscriminativeSet;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author minhhx
 */
public class WeightedSamplerMHDA_Fast extends MetroSampler {

    public WeightedSamplerMHDA_Fast(TreeQualifier _qualifier) {
        qualifier = _qualifier;
        pogVisitCount = new HashMap<PogNode, Integer>();
        answerSet = new OptimallyDiscriminativeSet();
        useGamma = false; // use gamma for border prob, in new formula
        useFastVersion = true;
        limitEditMap = false;
        normBetaExp = 200;
    }

    public void printParam() {
        System.out.println(//"normBeta = " + normK + "\tnormGamma = " + normGamma +
                "\tEpsilon = " + epsilon// + "\tminGamma = " + minGamma
                + "\tnormBetaExp = " + normBetaExp);
    }

    @Override
    public void sample(int maxIter, int seedSize, double threshold) {
        theta = threshold;
        sample(maxIter, seedSize);
    }

    @Override
    public void sample(int maxIter, int seedSize) {
        // reset sampling parameter
        totalVisitCount = 0;
        pogVisitCount.clear();
        Set<Node> previousState = null;
        double averagePogSize = 0;
        long time = System.currentTimeMillis();
        System.out.println("Sampling MHDA Fast");
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
            PogNode q = p.chooseNeighborWeighted_Fast(qualifier, generator);
            if (q == null) {
                skipCounter++;
                continue;
            }
            double q2p = 0, p2q = 0;
            // if at null node --> just move up            
            if (p.size() == 0) {
                acceptProb = 1.0;
            } else {
                // new alpha for q
                q2p = q.getTransitProb(p);
                p2q = p.getTransitProb(q);
                
                // calc the acceptance prob
                if (p.infoDensity == q.infoDensity && p.size() > q.size() ||
                        p.infoDensity < q.infoDensity && p.size() < q.size()) {
                    acceptProb = Math.min(1.0, q.getPotential_Fast() / p.getPotential_Fast() / MetroSampler.epsilon / p2q * q2p);                    ;
                } else {
                    acceptProb = Math.min(1.0, q.getPotential_Fast() / p.getPotential_Fast() / p2q * q2p);                    
                }
            }

            // check if q is accepted as the next state
            // uniform(0,1) <= acceptProb
            double uniform = generator.nextDouble();
            if (uniform <= acceptProb) {  // move from p to q
                
                // delayed acceptance step
                if (previousState != null && previousState.equals(q)) { // if go back to previous state, then delay
                    // choose another neighbor uniform at random, avoid previous neighbor
                    PogNode q2 = p.chooseNeighborUniform_DA(qualifier, generator);
                    uniform = generator.nextDouble();
                    // calc the new acceptance probability
                    double p2q2 = p.getTransitProb(q2);
                    acceptProb = p2q2 * p2q2 / p2q /p2q;                    
                    if (uniform <= acceptProb){ //accept the new state & update previous state
                        q = q2;                        
                    }
                }
                
                previousState = p.nodeList();
                p = q;
                p.updateLastEditedNode();
                
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
