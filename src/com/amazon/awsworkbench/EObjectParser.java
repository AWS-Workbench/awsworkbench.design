package com.amazon.awsworkbench;

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
import org.jgrapht.traverse.TopologicalOrderIterator;

import com.amazon.awsworkbench.data.ComponentObject;

import awsworkbench.design.Activator;

public class EObjectParser {

	private String[] mandatoryFields = { "varName", "identifier" };

	private Map<String, ComponentObject> componentMap = new HashMap<String, ComponentObject>();

	private Map<String, SortedSet<String>> uniqueValues = new HashMap<String, SortedSet<String>>();

	private Set<String> imports = new TreeSet<String>();
	private StringBuilder codeOutput = new StringBuilder();

	public void generateCode(EObject self) throws Exception {

		componentMap = new HashMap<String, ComponentObject>();
		uniqueValues = new HashMap<String, SortedSet<String>>();
		codeOutput = new StringBuilder();
		parse(self);
		generateImports();
		
		generateAppAndStackClass();
	}

	private void generateAppAndStackClass() {
		// TODO Auto-generated method stub
		
	}

	private void printImports() {
		for(String s: imports)
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

	public void parse(EObject obj) throws Exception {

		checkMandatoryFields(obj);

		ComponentObject cObject = new ComponentObject(obj);
		componentMap.put(cObject.getVarName(), cObject);
		EList<EObject> children = obj.eContents();
		for (EObject e : children)
			parse(e);

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
