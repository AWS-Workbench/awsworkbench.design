package com.amazon.awsworkbench;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EContentsEList;
import org.eclipse.ui.statushandlers.StatusManager;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.amazon.awsworkbench.data.ComponentObject;
import com.amazon.awsworkbench.dependency.Analyser;

import awsworkbench.design.Activator;

public class EObjectParser {

	private String[] mandatoryFields = { "varName", "identifier" };

	private Map<String, ComponentObject> componentMap = new HashMap<String, ComponentObject>();

	private Map<String, SortedSet<String>> uniqueValues = new HashMap<String, SortedSet<String>>();

	private Map<String, List<String>> variables = new HashMap<String, List<String>>();

	private Set<String> imports = new TreeSet<String>();
	private StringBuilder codeOutput = new StringBuilder();

	private Analyser analyser = new Analyser();

	public void generateCode(EObject self) throws Exception {
		
		

		System.out.println("Hello");

		componentMap = new HashMap<String, ComponentObject>();
		uniqueValues = new HashMap<String, SortedSet<String>>();
		variables = new HashMap<String, List<String>>();
		analyser = new Analyser();
		
		codeOutput = new StringBuilder();
		parse(self , new ArrayList<String>());

		buildDependencyGraph();
		analyser.checkCycles();
		
		printComponents();

		generateImports();

		printCode();

		generateAppAndStackClass();
	}

	private void printComponents() {
		for (ComponentObject c : componentMap.values()) {

			System.out.println(c);
		}
		
	}

	private void printCode() {

		for (ComponentObject c : componentMap.values()) {

			System.out.println(c.generateCode(componentMap));
		}

	}

	private void buildDependencyGraph() throws Exception {

		Map<String, ComponentObject> graphAdditions = new HashMap<String, ComponentObject>();

		for (ComponentObject cObject : componentMap.values()) {
			if (!graphAdditions.containsKey(cObject.getVarName())) {
				graphAdditions.put(cObject.getVarName(), cObject);
				analyser.addVariable(cObject.getVarName());
			}
		}

		for (ComponentObject cObject : graphAdditions.values()) {
			Map<String, List<String>> dependencies = cObject.getDependencies();

			for (String key : dependencies.keySet()) {
				List<String> varList = dependencies.get(key);

				for (String vars : varList) {
					if (graphAdditions.containsKey(vars)) {
						ComponentObject dObject = graphAdditions.get(vars);
						if (key.equals(dObject.getGeneratedClassName())) {
							analyser.addDependency(dObject.getVarName(), cObject.getVarName());
						} else {
							System.out.println("Mismatch : " + vars + " does not belong to class : " + key);
							cObject.removeDependency(key, vars);

						}

					} else {
						System.out.println("Variable not defined: " + vars + " for class : " + key);
						cObject.removeDependency(key, vars);
					}
				}

			}
		}

	}

	private void generateAppAndStackClass() {
		// TODO Auto-generated method stub

	}

	private void printImports() {
		for (String s : imports)
			System.out.print(s);

	}

	private void generateImports() {

		imports.add("import java.util.*;\n");
		for (ComponentObject cObject : componentMap.values()) {

			String starImport = cObject.getGeneratedClassName().substring(0,
					cObject.getGeneratedClassName().lastIndexOf("."));
			starImport = starImport + ".*";
			imports.add("import " + cObject.getGeneratedClassName() + ";\n");
			imports.add("import " + cObject.getBuilderClassName() + ";\n");
			imports.add("import " + starImport + ";\n");
		}

	}

	public void parse(EObject obj, List<String> parents) throws Exception {

		checkMandatoryFields(obj);

		ComponentObject cObject = new ComponentObject(obj,parents);
		componentMap.put(cObject.getVarName(), cObject);
		if (variables.containsKey(cObject.getGeneratedClassName()))
			variables.get(cObject.getGeneratedClassName()).add(cObject.getVarName());
		else {
			variables.put(cObject.getGeneratedClassName(), new ArrayList<String>());
			variables.get(cObject.getGeneratedClassName()).add(cObject.getVarName());
		}
		
		
		parents.add(cObject.getVarName());
		EList<EObject> children = obj.eContents();
		for (EObject e : children)
			parse(e,parents);
		parents.remove(cObject.getVarName());

	}

	private void checkMandatoryFields(EObject self) throws Exception {

		// TODO Auto-generated method stub
		EList<EStructuralFeature> allEStructFeats = self.eClass().getEAllStructuralFeatures();

		for (EStructuralFeature esf : allEStructFeats) {
			if (isMandatory(esf)) {

				if ((self.eGet(esf) == null || self.eGet(esf).toString().trim().isEmpty()))
					showError("Mandatory fields (Var Name or Identifier) are empty for some components");

				String keyName = esf.getName();
				String value = self.eGet(esf).toString().trim();

				if (uniqueValues.containsKey(keyName)) {
					if (uniqueValues.get(keyName).contains(value))
						showError("Duplicate value: " + value + " found for " + keyName);
					else
						uniqueValues.get(keyName).add(value);

				} else {

					uniqueValues.put(keyName, new TreeSet<String>());
					uniqueValues.get(keyName).add(value);

				}

			}
		}

	}

	public static void showError(String errorMessage) throws Exception {
		Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, errorMessage);
		StatusManager.getManager().handle(status, StatusManager.BLOCK);
		throw new Exception(errorMessage);

	}

	private boolean isMandatory(EStructuralFeature esf) {
		String fieldName = esf.getName().trim();
		for (String mandatoryField : mandatoryFields) {
			if (fieldName.equals(mandatoryField))
				return true;

		}
		return false;
	}

}
