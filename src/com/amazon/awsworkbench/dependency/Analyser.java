package com.amazon.awsworkbench.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import com.amazon.awsworkbench.data.ComponentObject;

public class Analyser {
	
	
	
	private Map<String, SortedSet<String>> graphElems = new HashMap<String, SortedSet<String>>();
	private Map<String, ComponentObject> graphObjects = new HashMap<String, ComponentObject>();
	
	
	
	
	public void addVariable(String varName, ComponentObject cObject) {
		if(!graphElems.containsKey(varName)) {
			graphElems.put(varName, new TreeSet<String>());
		}
		if(!graphObjects.containsKey(varName)) {
			graphObjects.put(varName, cObject);
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
	
	
	
	
	public void topologicalSort() {
		
		Stack<ComponentObject> stack = new Stack<ComponentObject>();
		
		for(ComponentObject c : graphObjects.values()) {
			
			if(!c.isVisited()) {
				topologicalSortUtil (c,stack);
			}
		}
		
		while(!stack.empty()) {
			ComponentObject c  = stack.pop();
			System.out.println(c.getVarName());
		}
		
	}

	private void topologicalSortUtil(ComponentObject c, Stack<ComponentObject> stack) {
		
		c.setVisited(true);
		for(String dependent : c.getDependentVars()) {
			
			ComponentObject c1 = graphObjects.get(dependent);
			if(c1 == null ) {
				System.out.println("null:"+ dependent);
				continue;
			}
			if(!c1.isVisited()) {
				topologicalSortUtil(c1, stack);
			}
		}
		stack.push(c);
	}

}
