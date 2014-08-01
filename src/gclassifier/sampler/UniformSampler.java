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
public class UniformSampler extends MetroSampler {

    public UniformSampler(TreeQualifier _qualifier) {
        qualifier = _qualifier;
        pogVisitCount = new HashMap<PogNode, Integer>();
        answerSet = new OptimallyDiscriminativeSet();
    }

    @Override
    public void sample(int maxIter, int seedSize) {
        // reset sampling parameter
        totalVisitCount = 0;
        pogVisitCount.clear();

        // generate any frequent pattern
        PogNode p = qualifier.genRandSeedSubgraph(seedSize);

        int iter = 0;
        double acceptProb = 0.0;
        int skipCounter = 0;

        while (iter < maxIter) {
            // pick a node q to jump to
            double beta_p = calcDeletionProb(p);   // the prob that a vertex is removed from the subgraph            
            PogNode q = p.chooseNeighborWeighted(qualifier, generator, beta_p);
            if (q == null) {
                skipCounter++;
                continue;
            }

            // p is null node --> move up
            if (p.size() == 0) {
                acceptProb = 1.0;
            } else {
                // new alpha for q
                double beta_q = calcDeletionProb(q);

                double degreeP = 0.0, degreeQ = 0.0;
                if (p.size() < q.size()) {
                    degreeP = p.degreeUp(qualifier) / (1 - beta_p);
                    degreeQ = q.degreeDown(qualifier) / beta_q;
                } else {
                    degreeP = p.degreeDown(qualifier) / beta_p;
                    degreeQ = q.degreeUp(qualifier) / (1 - beta_q);
                }

                if (p.size() > q.size() && p.infoDensity == q.infoDensity) {
                    acceptProb = 1.0;
                } else {
                    acceptProb = Math.min(degreeP * q.infoDensity / degreeQ / p.infoDensity, 1.0);
                }
            }

            // check if q is accepted as the next state
            // uniform(0,1) <= acceptProb
            double uniform = generator.nextDouble();
            if (uniform <= acceptProb) {  // move from p to q
                p = q;
                iter++;

                // update the visit count of p
                Integer curCount = pogVisitCount.get(q);
                if (curCount != null) {
                    curCount++;
                } else {
                    curCount = 1;
                }
                pogVisitCount.put(q, curCount);
            } else {
                // skip q
                qualifier.reverseChange();
                skipCounter++;
            }
            iter++;
            if (iter % 1000 == 0) {
                System.out.println("Iter: " + iter + " POG size explored : " + pogVisitCount.size()
                        + " He has skipped: " + skipCounter
                        + " Current Node Size: " + p.size());
            }
        }
        totalVisitCount = iter;
        System.out.println("Iter: " + iter + " POG size explored : " + pogVisitCount.size() + " He has skipped: " + skipCounter);
    }
}
