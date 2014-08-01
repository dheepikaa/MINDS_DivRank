/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.sampler;

import gclassifier.Node;
import gclassifier.TreeQualifier;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author minhhx
 */
public class RandomSampler extends MetroSampler {

    public RandomSampler(TreeQualifier _qualifier) {
        qualifier = _qualifier;
        pogVisitCount = new HashMap<PogNode, Integer>();
    }

    @Override
    public void sample(int maxIter, int seedSize) {
        // reset sampling parameter
        totalVisitCount = 0;
        pogVisitCount.clear();

        System.out.println("Random Choosing...");

        long time = System.currentTimeMillis();
        int iter = 0;

        while (iter < maxIter) {
            // pick a random size
            int size = generator.nextInt(seedSize) + 1;
            // generate any frequent pattern
            PogNode p = qualifier.genRandSeedSubgraphSimple(size);

            Integer curCount = pogVisitCount.get(p);
            if (curCount != null) {
                curCount++;
            } else {
                curCount = 1;
            }
            pogVisitCount.put(p, curCount);

            iter++;
            if (iter % 1000 == 0) {
                System.out.println("Iter: " + iter + " POG size explored : " + pogVisitCount.size()
                        + " Current Node Size: " + p.size()
                        + " Time: " + (System.currentTimeMillis() - time));
            }
        }
        totalVisitCount = iter;
        System.out.println("Iter: " + iter + " POG size explored : " + pogVisitCount.size());
    }
}
