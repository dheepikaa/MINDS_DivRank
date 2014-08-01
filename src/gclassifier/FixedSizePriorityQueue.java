package gclassifier;

import java.util.PriorityQueue;

public class FixedSizePriorityQueue {
	
	public int maxSize = 50;
	
	PriorityQueue<SubGraphFeature> pq = new PriorityQueue<SubGraphFeature>();
	
	public boolean add(SubGraphFeature node) {
		
		pq.remove(node);
		if(pq.size() > maxSize) {
			SubGraphFeature last = pq.poll();
			if(last.featurePotential > node.featurePotential) {
				node = last;
			}
		}
		return pq.add(node);
	}
	
	public SubGraphFeature getNext() {
		return pq.poll();
	}
	
	public boolean hasNext() {
		
		if(pq.isEmpty())
			return false;
		return true;
	}

}
