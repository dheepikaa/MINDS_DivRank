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
public class WeightedSamplerMH extends MetroSampler {

    public WeightedSamplerMH(TreeQualifier _qualifier) {
        qualifier = _qualifier;
        pogVisitCount = new HashMap<PogNode, Integer>();
        answerSet = new OptimallyDiscriminativeSet();
        limitEditMap = true;
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
        double averagePogSize = 0;
        long time = System.currentTimeMillis();
        System.out.println("Sampling: log scale + qij");
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
            double beta_p = calcDeletionProb(p);
            PogNode q = p.chooseNeighborWeighted(qualifier, generator, beta_p);
            if (q == null) {
                skipCounter++;
                continue;
            }

            // if at null node --> just move up
            if (p.size() == 0) {
                acceptProb = 1.0;
            } else {
                // new alpha for q
                double beta_q = calcDeletionProb(q);

                double q2p = q.getTransitProb(p);
                double p2q = p.getTransitProb(q);
                if (q.size() == 0){
                    q2p = 1.0;
                }
                if (p.size() < q.size()) {
                    p2q *= (1 - beta_p);
                    q2p *= beta_q;
                } else {
                    p2q *= beta_p;
                    q2p *= (1 - beta_q);
                }
                if (p.infoDensity == q.infoDensity && p.size() > q.size()) {
                    acceptProb = 1.0;
                } else {
                    acceptProb = Math.min(1.0, q.infoDensity / p.infoDensity / p2q * q2p);
                }
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
                            + "\t" + averagePogSize/iter      
                            + "\t" + p.infoDensity
                            + "\t" + (System.currentTimeMillis() - time)                            
                            );
                }
            } else {
                // skip the subgraph
                qualifier.reverseChange();
                skipCounter++;
            }
        }
        totalVisitCount = iter;
        System.out.println("Iter: " + iter + " POG size explored : " + pogVisitCount.size() + " He has skipped: " + skipCounter);
    }
}
