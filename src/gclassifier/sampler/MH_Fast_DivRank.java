/**
 * 
 */
package gclassifier.sampler;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import gclassifier.FixedSizePriorityQueue;
import gclassifier.Node;
import gclassifier.SubGraphFeature;
import gclassifier.TreeQualifier;
import gclassifier.answer.OptimallyDiscriminativeSet;

/**
 * @author Dheepikaa N
 *
 */
public class MH_Fast_DivRank extends MetroSampler {

	/**
	 * 
	 */
	public MH_Fast_DivRank(TreeQualifier _qualifier) {
		// TODO Auto-generated constructor stub
		qualifier = _qualifier;
		pogVisitCount = new HashMap<PogNode, Integer>();
		answerSet = new OptimallyDiscriminativeSet();
		useGamma = false; // use gamma for border prob, in new formula
		useFastVersion = false;
		useFastVersionDivRank = true;
		limitEditMap = false;
		normBetaExp = 200;
		nodePotential = new HashMap<ArrayList<Integer>, Double>();
		visit_count = new HashMap<ArrayList<Integer>, Integer>();
		featureQueue = new FixedSizePriorityQueue();
	}

	public void printParam() {
		System.out.println(// "normBeta = " + normK + "\tnormGamma = " +
							// normGamma +
				"\tEpsilon = " + epsilon// + "\tminGamma = " + minGamma
						+ "\tnormBetaExp = " + normBetaExp);
	}

	@Override
	public void sample(int maxIter, int seedSize, double threshold) {
		theta = threshold;
		// reset sampling parameter
		totalVisitCount = 0;
		pogVisitCount.clear();
		answerSet.clear();
		double averagePogSize = 0;
		long time = System.currentTimeMillis();
		System.out.println("Sampling Fast Version No stat");
		printParam();
		System.out
				.println("#Accepted\tAnswerSize\t#Rejected\tAvgSize\tcurPotential\tSize_subgraph\tTime");
		
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
			PogNode q = p.chooseNeighborWeighted_Fast_DivRank(qualifier, generator);
			if (q == null) {
				skipCounter++;
				continue;
			}
			// if at null node --> just move up
			if (p.size() == 0) {
				acceptProb = 1.0;
			} else {
				// new alpha for q
				double q2p = q.getTransitProb(p);
				double p2q = p.getTransitProb(q);

				// calc the acceptance prob
				if (p.infoDensity == q.infoDensity && p.size() > q.size()
						|| p.infoDensity < q.infoDensity && p.size() < q.size()) {
					acceptProb = Math.min(1.0, q.getPotential_Fast()
							/ p.getPotential_Fast() / MetroSampler.epsilon
							/ p2q * q2p);
				} else {
					acceptProb = Math.min(1.0, q.getPotential_Fast()
							/ p.getPotential_Fast() / p2q * q2p);
				}
			}

			// check if q is accepted as the next state
			// uniform(0,1) <= acceptProb
			double uniform = generator.nextDouble();
			if (uniform <= acceptProb) { // move from p to q
				p = q;

				// add to answerset if potential > threshold
				if (q.infoDensity > theta)
					answerSet.addSubgraph(q.nodeList(), q.infoDensity);
				iter++;
				averagePogSize += p.size();
				if (iter % 5 == 0) {
					System.out.println(iter
							+ "\t\t"
							+ answerSet.size()
							+ "\t\t"
							+ skipCounter
							+ "\t\t"
							+ new DecimalFormat("00.000").format(averagePogSize
									/ iter) 
							+ "\t"
							+ new DecimalFormat("0.000").format(p.infoDensity)
							+ "\t\t" + p.size() + "\t"
							+ (System.currentTimeMillis() - time));
				}
			} else {
				// skip the subgraph
				qualifier.reverseChange();
				skipCounter++;
			}
		}
		totalVisitCount = iter;
		answerSet.potentialStat();
	
	} 
	
	@Override
	public void printStatistics() {
		int size = MetroSampler.featureQueue.maxSize;
		
		for(int i=0; i<size; i++) {
			if(!MetroSampler.featureQueue.hasNext()) {
				break;
			}
			SubGraphFeature temp = MetroSampler.featureQueue.getNext();
			System.out.print(temp.featurePotential+" ");
			for(Integer n : temp.nodeList) {
				System.out.print(n+" ");
			}
			System.out.println();
		}
	}
	/* (non-Javadoc)
	 * @see gclassifier.sampler.MetroSampler#sample(int, int)
	 */
	@Override
	public void sample(int maxIter, int seedSize) {
		// TODO Auto-generated method stub

	}

}
