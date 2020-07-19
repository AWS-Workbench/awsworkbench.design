package com.amazon.awsworkbench.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.amazon.awsworkbench.data.ComponentObject;

public class Analyser {
	
	
	
	private Map<String, SortedSet<String>> graphElems = new HashMap<String, SortedSet<String>>();
	
	
	
	
	public void addVariable(String varName) {
		if(!graphElems.containsKey(varName)) {
			graphElems.put(varName, new TreeSet<String>());
		}
	}
	
	public void addDependency(String dependency, String dependent) {
		if(graphElems.containsKey(dependent))
			graphElems.get(dependent).add(dependency);
	}

	public void checkCycles() {
		
		for(String s: graphElems.keySet()) {
			ArrayList<String> cycleTrace = new ArrayList<String>();
			boolean hasCycle = detectCycle(s,cycleTrace);
			if(hasCycle)
				System.out.println(cycleTrace);
		}
		
	}

	private boolean detectCycle(String s, ArrayList<String> cycleTrace) {
		// TODO Auto-generated method stub
		System.out.println(s + " " + graphElems.get(s));
		cycleTrace.add(s);
		for(String s1 : graphElems.get(s))
		{
			if(cycleTrace.contains(s1))
				return true;
			else 
				return detectCycle(s1, cycleTrace);
		}
		cycleTrace.remove(s);
		return false;
	}

}
