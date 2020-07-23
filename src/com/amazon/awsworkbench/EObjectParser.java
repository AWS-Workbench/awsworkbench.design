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

	private Map<String, ComponentObject> componentObjectMap = new HashMap<String, ComponentObject>();

	private Map<String, SortedSet<String>> uniqueMandatoryFieldValues = new HashMap<String, SortedSet<String>>();

	private Map<String, List<String>> classNameToVariablesMap = new HashMap<String, List<String>>();

	private Set<String> importStatements = new TreeSet<String>();
	private StringBuilder codeOutput = new StringBuilder();

	private ComponentObject appObject;

	private Analyser graphAnalyser = new Analyser();

	public void generateCode(EObject self) throws Exception {


		componentObjectMap = new HashMap<String, ComponentObject>();
		uniqueMandatoryFieldValues = new HashMap<String, SortedSet<String>>();
		classNameToVariablesMap = new HashMap<String, List<String>>();
		graphAnalyser = new Analyser();

		codeOutput = new StringBuilder();
		parse(self, new ArrayList<String>(), true,null);

		buildDependencyGraph();
		graphAnalyser.checkCycles();

		//printComponents();

		generateImports();

		
		graphAnalyser.topologicalSort();
		
		
		printCode();
		
	//	printImports();

	//	generateApp();

	}

	private void generateApp() {
		
		String code = appObject.generateCode(componentObjectMap);

	}

	private void printComponents() {
		for (ComponentObject c : componentObjectMap.values()) {

			System.out.println(c);
		}

	}

	private void printCode() {

		for (ComponentObject c : graphAnalyser.getQueue()) {

			System.out.println(c.generateCode(componentObjectMap));
		}

	}

	private void buildDependencyGraph() throws Exception {

		Map<String, ComponentObject> graphAdditions = new HashMap<String, ComponentObject>();

		for (ComponentObject dependentObject : componentObjectMap.values()) {
			if (!graphAdditions.containsKey(dependentObject.getVarName())) {
				graphAdditions.put(dependentObject.getVarName(), dependentObject);
				graphAnalyser.addVariable(dependentObject.getVarName(), dependentObject);
			}
		}

		for (ComponentObject dependentObject : graphAdditions.values()) {
			Map<String, List<String>> dependencies = dependentObject.getDependencies();

			for (String key : dependencies.keySet()) {
				List<String> varList = dependencies.get(key);

				for (String vars : varList) {
					if (graphAdditions.containsKey(vars)) {
						ComponentObject neededObject = graphAdditions.get(vars);
						if (key.equals(neededObject.getGeneratedClassName())) {
							graphAnalyser.addDependency(neededObject.getVarName(), dependentObject.getVarName());
						} else {
							System.out.println("Mismatch : " + vars + " does not belong to class : " + key);
							//cObject.removeDependency(key, vars);

						}

					} else {
						System.out.println("Variable not defined: " + vars + " for class : " + key);
						dependentObject.removeDependency(key, vars);
					}
				}

			}
		}

	}

	private void generateAppAndStackClass() {
		// TODO Auto-generated method stub

	}

	private void printImports() {
		for (String statement : importStatements)
			System.out.print(statement);

	}

	private void generateImports() {

		importStatements.add("import java.util.*;\n");
		for (ComponentObject currentObject : componentObjectMap.values()) {

			String starImport = currentObject.getGeneratedClassName().substring(0,
					currentObject.getGeneratedClassName().lastIndexOf("."));
			starImport = starImport + ".*";
			importStatements.add("import " + currentObject.getGeneratedClassName() + ";\n");
			importStatements.add("import " + currentObject.getBuilderClassName() + ";\n");
			importStatements.add("import " + starImport + ";\n");
		}

	}

	public void parse(EObject currentEcoreObject, List<String> parents, boolean isApp, String parentVarname) throws Exception {

		checkMandatoryFields(currentEcoreObject);

		ComponentObject componentObject = new ComponentObject(currentEcoreObject, parents, parentVarname);
		if (isApp) {
			appObject = componentObject;

		}
		componentObjectMap.put(componentObject.getVarName(), componentObject);
		if (classNameToVariablesMap.containsKey(componentObject.getGeneratedClassName()))
			classNameToVariablesMap.get(componentObject.getGeneratedClassName()).add(componentObject.getVarName());
		else {
			classNameToVariablesMap.put(componentObject.getGeneratedClassName(), new ArrayList<String>());
			classNameToVariablesMap.get(componentObject.getGeneratedClassName()).add(componentObject.getVarName());
		}

		parents.add(componentObject.getVarName());
		EList<EObject> children = currentEcoreObject.eContents();
		for (EObject e : children)
			parse(e, parents, false,componentObject.getVarName());
		parents.remove(componentObject.getVarName());

	}

	private void checkMandatoryFields(EObject self) throws Exception {

		// TODO Auto-generated method stub
		EList<EStructuralFeature> allEStructFeats = self.eClass().getEAllStructuralFeatures();

		for (EStructuralFeature eStructuralFeature : allEStructFeats) {
			if (isMandatory(eStructuralFeature)) {

				if ((self.eGet(eStructuralFeature) == null || self.eGet(eStructuralFeature).toString().trim().isEmpty()))
					showError("Mandatory fields (Var Name or Identifier) are empty for some components");

				String keyName = eStructuralFeature.getName();
				String value = self.eGet(eStructuralFeature).toString().trim();

				if (uniqueMandatoryFieldValues.containsKey(keyName)) {
					if (uniqueMandatoryFieldValues.get(keyName).contains(value))
						showError("Duplicate value: " + value + " found for " + keyName);
					else
						uniqueMandatoryFieldValues.get(keyName).add(value);

				} else {

					uniqueMandatoryFieldValues.put(keyName, new TreeSet<String>());
					uniqueMandatoryFieldValues.get(keyName).add(value);

				}

			}
		}

	}

	public static void showError(String errorMessage) throws Exception {
		Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, errorMessage);
		StatusManager.getManager().handle(status, StatusManager.BLOCK);
		throw new Exception(errorMessage);

	}

	private boolean isMandatory(EStructuralFeature eStructuralFeature) {
		String fieldName = eStructuralFeature.getName().trim();
		for (String mandatoryField : mandatoryFields) {
			if (fieldName.equals(mandatoryField))
				return true;

		}
		return false;
	}

}
