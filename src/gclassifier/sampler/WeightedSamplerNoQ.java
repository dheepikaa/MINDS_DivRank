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
public class WeightedSamplerNoQ extends MetroSampler {

    public WeightedSamplerNoQ(TreeQualifier _qualifier) {
        qualifier = _qualifier;
        pogVisitCount = new HashMap<PogNode, Integer>();
        answerSet = new OptimallyDiscriminativeSet();
        limitEditMap = true;
    }

    @Override
    public void sample(int maxIter, int seedSize) {
        // reset sampling parameter
        totalVisitCount = 0;
        pogVisitCount.clear();
        long time = System.currentTimeMillis();
        System.out.println("Sampling: log scale + no qij");

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
                if (p.infoDensity == q.infoDensity && p.size() > q.size()) {
                    acceptProb = 1.0;
                } else {
                    acceptProb = Math.min(1.0, q.infoDensity / p.infoDensity);
                }
            }

            // check if q is accepted as the next state
            // uniform(0,1) <= acceptProb
            double uniform = generator.nextDouble();
            if (uniform <= acceptProb) {  // move from p to q
                p = q;
//                System.out.println("Accept " + acceptProb + " " + uniform + " "+ q.size() + " " + q.infoDensity);

                // update the visit count of q
                Integer curCount = pogVisitCount.get(q);
                if (curCount != null) {
                    curCount++;
                } else {
                    curCount = 1;
                }
                pogVisitCount.put(q, curCount);

                iter++;
                if (iter % 1000 == 0) {
                    System.out.println("Iter: " + iter + " POG size explored : " + pogVisitCount.size()
                            + " He has skipped: " + skipCounter
                            + " Current Node Size: " + p.size()
                            + " Time: " + (System.currentTimeMillis() - time));
                }

            } else {
                // skip the subgraph
                qualifier.reverseChange();
                skipCounter++;
//                System.out.println("Skipped " + acceptProb + " " + uniform + " "+ q.size() + " " + q.infoDensity);
            }
        }
        totalVisitCount = iter;
        System.out.println("Iter: " + iter + " POG size explored : " + pogVisitCount.size() + " He has skipped: " + skipCounter);
    }
}
