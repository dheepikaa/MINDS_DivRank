/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gclassifier.sampler;

import gclassifier.Node;
import gclassifier.TreeQualifier;
import gclassifier.answer.OptimallyDiscriminativeSet;
import gclassifier.answer.Subgraph;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

/**
 * 
 * @author minhhx
 */
public class WeightedSamplerMH_Fast extends MetroSampler {

	public WeightedSamplerMH_Fast(TreeQualifier _qualifier) {
		qualifier = _qualifier;
		pogVisitCount = new HashMap<PogNode, Integer>();
		answerSet = new OptimallyDiscriminativeSet();
		useGamma = false; // use gamma for border prob, in new formula
		useFastVersion = true;
		limitEditMap = false;
		normBetaExp = 200;
	}

	public void printStatistics() {
	
		getDiversifiedSubgraphs();
		/*
		Collection<Subgraph> ans = answerSet.getAnswer();
		double sim = 0;
		ArrayList lis,lis2;// = new ArrayList<Float>();
		int c=0;
		ArrayList<Subgraph> toBeRemoved = new ArrayList<Subgraph>();
		for (Subgraph g : ans) {
			if (g.nodes.size() <= 2) {
				toBeRemoved.add(g);
				System.out.println("Found!!\n\n\n");
			}
		}
		for (Subgraph g : toBeRemoved) {
			ans.remove(g);
		}
		
		int[] sum = new int[10];
		
		for (Subgraph g1 : ans) {
			//System.out.println(g1.nodes);
			lis = new ArrayList<Float>();
			lis2 = new ArrayList<Integer>();
			for (Subgraph g2 : ans) {
				int count = 0;
				for (Node n1 : g1.nodes) {
					if (g2.nodes.contains(n1)) {
						count++;
					}
				}
				int s1 = g1.nodes.size();
				int s2 = g2.nodes.size();
				if(s1 > s2) {
					s1 = s2;
				}
				float t = (float)(count+0.0) / (s1 );
				lis.add(t);
				lis2.add(count);
				sim += t;
			}
			
			Collections.sort(lis);
			Collections.sort(lis2);
			Float thres = 0f;
			int[] toPlot = new int[10];
			
			for (int i=0; i<10; i++)
				toPlot[i] = 0;
			
			for (int i=0; i<lis.size(); i++) {
				if ((Float)lis.get(i) - (thres+0.1) < 0) {
					toPlot[(int)(thres*10)]++;
				} else {
					thres += 0.1;
					toPlot[(int)(thres*10)]+= toPlot[(int)(thres*10)-1];
					toPlot[(int)(thres*10)]++;
				}
			}
			while(thres*10 < 9) {
				thres += 0.1;
				toPlot[(int)(thres*10)]+= toPlot[(int)(thres*10)-1];
			}
			for (int i=0; i<10; i++)
				sum[i] += toPlot[i];
			
//			System.out.println(lis);
//			System.out.println(lis2);
//			for (int i=0; i<10; i++)
//				System.out.println(toPlot[i]);
			
		}
		
		System.out.println("Average");
		for (int i=0; i<10; i++)
			System.out.println(sum[i]/ans.size());
		
		for (Subgraph g : ans) {
			ArrayList temp = new ArrayList<Integer>();
			for (Node node : g.nodes) {
				temp.add(node.id);
			}
			Collections.sort(temp);
			System.out.println(temp);
		}
		System.out.println("Similarity score:" + sim);
		*/
	}

	private void getDiversifiedSubgraphs() {
		// TODO Auto-generated method stub
		Collection<Subgraph> ans = answerSet.getAnswer();
		HashMap< Subgraph, ArrayList<Subgraph>> graphMap = new HashMap<Subgraph, ArrayList<Subgraph>>();
		for (Subgraph g1 : ans) {
			graphMap.put(g1, new ArrayList<Subgraph>());
			//System.out.println(g1.nodes);
			for (Subgraph g2 : ans) {
				
				int count = 0;
				for (Node n1 : g1.nodes) {
					if (g2.nodes.contains(n1)) {
						count++;
					}
				}
				int s1 = g1.nodes.size();
				int s2 = g2.nodes.size();
				if(s1 > s2) {
					s1 = s2;
				}
				float t = (float)(count+0.0) / (s1 );
				if (t >= 0.8) {
					graphMap.get(g1).add(g2);
				}
			}
		}
		ArrayList<Subgraph> diversifiedSet = new ArrayList<Subgraph>();
		while(true) {
			Subgraph g = getMaxRep(graphMap);
			if (g == null) {
				System.out.println("Done!");
				break;
			}
			diversifiedSet.add(g);
			removeRepresentedGraphs(graphMap.get(g), graphMap);
		}
		answerSet.clear();
		for (int i=0; i<diversifiedSet.size(); i++) {
			answerSet.addSubgraph(diversifiedSet.get(i).nodes, diversifiedSet.get(i).score);
			//answerSet.getAnswer().add(diversifiedSet.get(i));
			
			ArrayList temp = new ArrayList<Integer>();
			for (Node node : diversifiedSet.get(i).nodes) {
				temp.add(node.id);
			}
			Collections.sort(temp);
			System.out.println(temp);
		}
		
	}

	private void removeRepresentedGraphs(ArrayList<Subgraph> arrayList, HashMap<Subgraph, ArrayList<Subgraph>> graphMap) {
		// TODO Auto-generated method stub
		
		for (int i=0; i<arrayList.size(); i++) {
			
			graphMap.remove(arrayList.get(i));
		}
		for (Subgraph g : graphMap.keySet()) {
			for (int i=0; i<arrayList.size(); i++) {
				if(graphMap.get(g).contains(arrayList.get(i))) {
					graphMap.get(g).remove(arrayList.get(i));
				}
							
			}
		}
	}

	private Subgraph getMaxRep(HashMap<Subgraph, ArrayList<Subgraph>> graphMap) {
		// TODO Auto-generated method stub
		Subgraph maxGraph = null;
		int maxCount = 0;
		
		for (Subgraph g : graphMap.keySet()) {
			int count = graphMap.get(g).size();
			if ( count > maxCount) {
				maxCount = count;
				maxGraph = g;
			}
		}
		return maxGraph;
	}

	public void printParam() {
		System.out.println(// "normBeta = " + normK + "\tnormGamma = " +
							// normGamma +
				"\tEpsilon = " + epsilon// + "\tminGamma = " + minGamma
						+ "\tnormBetaExp = " + normBetaExp);
	}

	// sampling without stat
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
			PogNode q = p.chooseNeighborWeighted_Fast(qualifier, generator);
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
				
				if (iter % 50 == 0) {
					System.out.println(iter
							+ "\t\t"
							+ answerSet.size()
							+ "\t\t"
							+ skipCounter
							+ "\t\t"
							+ new DecimalFormat("00.000").format(averagePogSize
									/ iter) + "\t" + p.nodeList().size()
							+ "\t\t"
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
	public void sample(int maxIter, int seedSize) {
		// reset sampling parameter
		totalVisitCount = 0;
		pogVisitCount.clear();
		double averagePogSize = 0;
		long time = System.currentTimeMillis();
		System.out.println("Sampling Fast Version");
		printParam();
		System.out
				.println("#Accepted\t#UniqueAccepted\t#Rejected\tAvgSize\tcurPotential\tTime");
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
					;
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
				if (iter % 50 == 0) {
					System.out.println(iter
							+ "\t\t"
							+ 0
							+ "\t\t"
							+ skipCounter
							+ "\t\t"
							+ new DecimalFormat("00.000").format(averagePogSize
									/ iter) + "\t" + p.nodeList().size()
							+ "\t\t"
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
		// System.out.println("Iter: " + iter + " POG size explored : " +
		// pogVisitCount.size() + " He has skipped: " + skipCounter);
	}
}
