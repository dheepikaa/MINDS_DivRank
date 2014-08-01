package gclassifier;

import java.util.ArrayList;
import java.util.Iterator;

public class SubGraphFeature implements Comparable<SubGraphFeature>{

	public ArrayList<Integer> nodeList;
	public double featurePotential;

	public SubGraphFeature(ArrayList<Integer> nodeListTemp, double potential) {
		nodeList = nodeListTemp;
		featurePotential = potential;
	}

	@Override
	public int compareTo(SubGraphFeature s2) {
		// TODO Auto-generated method stub
		if(featurePotential > s2.featurePotential)
			return 1;
		else
			return -1;
	}
	
	@Override
	public boolean equals(Object o2) {
		
		if(o2 == null || o2.getClass() != getClass()) {
			return false;
		} else {
			SubGraphFeature s2 = (SubGraphFeature) o2;
			Iterator<Integer> i1 = nodeList.iterator();
			Iterator<Integer> i2 = s2.nodeList.iterator();
			
			while(i1.hasNext() && i2.hasNext()) {
				int re = i1.next().intValue(), re2 = i2.next().intValue();
				if(re != re2) {
					return false;
				}
			}
			
			if(i1.hasNext() || i2.hasNext()) 
				return false;
			
		}
		
		return true;
	}
}
