package com.amazon.awsworkbench.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.JavaCompiler;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.javatuples.Pair;

import com.amazon.awsworkbench.EObjectParser;
import com.amazon.awsworkbench.data.ComponentAttribute.MapAttribute;

public class ComponentObject {
	private String varName;
	private String identifier;
	private boolean isGenerated = false;
	private String generatedCode;
	private EObject parentObject;
	private String generatedClassName;
	private String additionalCode;
	private List<String> dependentVars = new ArrayList<String>();
	private boolean visited = false;

	private List<ComponentAttribute> otherAttributes = new ArrayList<ComponentAttribute>();

	private Map<String, List<String>> dependencies = new HashMap<String, List<String>>();

	private List<String> parents = new ArrayList<String>();

	public final String VARNAME = "varName";
	public final String IDENTIFIER = "identifier";
	public final String GENERATED_CLASS_NAME = "generatedClassName";

	public final String ADDITIONAL_CODE = "additionalCode";

	public static final String SPACE = " ";

	public static final String EQUALS = " = ";

	public static final String CREATE = ".create()";

	public static final String DOT = ".";

	public static final String OPENBRACKET = "( ";

	public static final String CLOSEBRACKET = " )";

	public static final String QUOT = "\"";

	public static final String NEWLINE = " \n";

	public static final String STRING_CLASS = "java.lang.String";

	public static final String COMMA = " , ";
	public static final String UNDERSCORE = "_";

	public static final String LESSTHAN = "<";

	public static final String GREATERTHAN = ">";

	

	public ComponentObject(EObject self, List<String> parents, String parentName) throws Exception {

		parentObject = self;
		if (parentName != null)
			dependentVars.add(parentName);
		this.parents.addAll(parents);
		System.out.println("\n\n");
		System.out.println(self.getClass().getName());

		EList<EStructuralFeature> allEStructFeats = self.eClass().getEAllStructuralFeatures();

		for (EStructuralFeature esf : allEStructFeats) {
			if (esf.getName().equals(VARNAME)) {
				varName = self.eGet(esf).toString();
			} else if (esf.getName().equals(IDENTIFIER)) {
				identifier = self.eGet(esf).toString();
			} else if (esf.getName().equals(GENERATED_CLASS_NAME)) {
				generatedClassName = self.eGet(esf).toString();
			} else if (esf.getName().equals(ADDITIONAL_CODE) && self.eGet(esf) != null
					&& !self.eGet(esf).toString().trim().isEmpty()) {
				additionalCode = self.eGet(esf).toString();
			} else {

				if (!(esf instanceof EAttribute))
					continue;

				System.out.println(esf.getName());
				System.out.println("Isenum: " + (esf.getEType().getInstanceClassName()));
				System.out.println();

				String featureName = esf.getName();

				if (self.eGet(esf) != null && !self.eGet(esf).toString().trim().isEmpty()) {
//					System.out.println(esf.getName() + " " + esf.toString());
//					System.out.println(self.eGet(esf).toString().trim());

					String value = self.eGet(esf).toString().trim();

					if (featureName.endsWith("AsReference")) {

						addReferenceDependency(featureName, value);
					} else if (featureName.endsWith("AsList")) {

						addListDependency(featureName, value);
					} else if (featureName.endsWith("AsMap")) {

						addMapDependency(featureName, value);
					}

					ComponentAttribute cAttribute = new ComponentAttribute(esf, value);
					System.out.println(cAttribute.toString());

					for (List<String> lists : dependencies.values()) {
						dependentVars.addAll(lists);
					}

					otherAttributes.add(cAttribute);

				}

			}
		}

	}

	private void addReferenceDependency(String featureName, String featureValue) {

		String className = featureName.substring(featureName.indexOf('_') + 1, featureName.lastIndexOf('_'))
				.replace('_', '.');

		if (dependencies.containsKey(className)) {
			dependencies.get(className).add(featureValue);
		} else {
			dependencies.put(className, new ArrayList<String>());
			dependencies.get(className).add(featureValue);
		}

	}

	private void addListDependency(String featureName, String featureValue) {

		String className = featureName.substring(featureName.indexOf('_') + 1, featureName.lastIndexOf('_'))
				.replace('_', '.');
		if (className.startsWith("java.lang"))
			return;

		List<String> values = new ArrayList<String>(Arrays.asList(featureValue.split(",")));

		if (dependencies.containsKey(className)) {
			dependencies.get(className).addAll(values);
		} else {
			dependencies.put(className, new ArrayList<String>());
			dependencies.get(className).addAll(values);
		}

	}

	private void addMapDependency(String featureName, String featureValue) throws Exception {

		String[] mapVals = featureName.substring(featureName.indexOf('_') + 1, featureName.lastIndexOf('_'))
				.split("__");

		if (mapVals.length > 2)
			EObjectParser.showError("Maps has more than 2 components");

		String className1 = mapVals[0].replace('_', '.');
		String className2 = mapVals[1].replace('_', '.');
		Map<String, String> featureMap = breakFeatureValueToMap(featureValue);

		if (!className1.startsWith("java.lang")) {

			if (dependencies.containsKey(className1)) {
				dependencies.get(className1).addAll(featureMap.keySet());
			} else {
				dependencies.put(className1, new ArrayList<String>());
				dependencies.get(className1).addAll(featureMap.keySet());
			}

		}

		if (!className2.startsWith("java.lang")) {

			if (dependencies.containsKey(className2)) {
				dependencies.get(className2).addAll(featureMap.values());
			} else {
				dependencies.put(className2, new ArrayList<String>());
				dependencies.get(className2).addAll(featureMap.values());
			}

		}

	}

	private Map<String, String> breakFeatureValueToMap(String featureValue) {

		HashMap<String, String> featureValues = new HashMap<String, String>();

		for (String s : featureValue.split(",")) {
			String[] s1 = s.split(":");
			featureValues.put(s1[0], s1[1]);
		}

		return featureValues;
	}

	public String getVarName() {
		// TODO Auto-generated method stub
		return varName;
	}

	public String getIdentifier() {
		// TODO Auto-generated method stub
		return identifier;
	}

	public boolean isGenerated() {
		return isGenerated;
	}

	public String getGeneratedCode() {
		return generatedCode;
	}

	public EObject getParentObject() {
		return parentObject;
	}

	public String getGeneratedClassName() {
		return generatedClassName;
	}

	public String getBuilderClassName() {
		return generatedClassName + ".Builder";
	}

	public String getAdditionalCode() {
		return additionalCode;
	}

	public List<ComponentAttribute> getOtherAttributes() {
		return otherAttributes;
	}

	public Map<String, List<String>> getDependencies() {
		return dependencies;
	}

	@Override
	public String toString() {

		String resultString = new String();

		resultString += getVarName();
		resultString += " " + getGeneratedClassName() + "\n";
		resultString += " " + parents + "\n";

		return resultString;

	}

	public void removeDependency(String className, String varName) {

		for (ComponentAttribute cAttribute : otherAttributes) {

			cAttribute.removeDependency(className, varName);
			dependentVars.remove(varName);
		}
	}

	public String generateCode(Map<String, ComponentObject> componentMap) {

		

		String code = new String();

		code += getGeneratedClassName() + SPACE + getVarName() + EQUALS + getBuilderClassName() + CREATE + NEWLINE;

		for (ComponentAttribute attribute : otherAttributes) {

			if (!attribute.isCanGenerate(componentMap))
				continue;
			if (!(attribute.getType() == ComponentAttributeTypes.LIST
					|| attribute.getType() == ComponentAttributeTypes.MAP)) {
				String value = getSingleValue(attribute.getAttributeValues());
				if (attribute.getType() == ComponentAttributeTypes.BOOLEAN
						|| attribute.getType() == ComponentAttributeTypes.INTEGER) {
					code += DOT + attribute.getName() + OPENBRACKET + value + CLOSEBRACKET + NEWLINE;

				} else if (attribute.getType() == ComponentAttributeTypes.REFERENCE
						&& componentMap.containsKey(value)) {
					code += DOT + attribute.getName() + OPENBRACKET + value + CLOSEBRACKET + NEWLINE;
				} else if (attribute.getType() == ComponentAttributeTypes.STRING) {
					code += DOT + attribute.getName() + OPENBRACKET + QUOT + value + QUOT + CLOSEBRACKET + NEWLINE;
				}

			} else if (attribute.getType() == ComponentAttributeTypes.LIST) {

				String listCode = buildList(attribute.getAttributeValues());

				code += DOT + attribute.getName() + OPENBRACKET + listCode + CLOSEBRACKET + NEWLINE;
			}

			else if (attribute.getType() == ComponentAttributeTypes.MAP) {

				Pair<String, String> codePair = buildMap(attribute);
				code = codePair.getValue1() + code;
				
				code += DOT + attribute.getName() + OPENBRACKET + codePair.getValue0() + CLOSEBRACKET + NEWLINE;
			}

		}

		code += ".build();\n";
		
		this.generatedCode  = code;
		this.isGenerated = true;
		
		

		return code;
	}

	private Pair<String, String> buildMap(ComponentAttribute attribute) {
		String mapName = getVarName() + UNDERSCORE + attribute.getName();

		MapAttribute mapAttribute = attribute.getMapAttribute();
		String declaration = "java.util.Map" + LESSTHAN + mapAttribute.getKeyClass() + COMMA
				+ mapAttribute.getValueClass() + GREATERTHAN + SPACE + mapName + EQUALS + " new " + "java.util.HashMap"
				+ LESSTHAN + mapAttribute.getKeyClass() + COMMA + mapAttribute.getValueClass() + GREATERTHAN + "();\n";
		
		declaration += NEWLINE ;
		String key = new String();
		String value = new String();
		for(String s: mapAttribute.getValues().keySet())
		{
			if(mapAttribute.getKeyClass().equals(STRING_CLASS))
				 key = QUOT +s + QUOT;
			else
				key = s;
			
			value = mapAttribute.getValues().get(s);
			
			if(mapAttribute.getValueClass().equals(STRING_CLASS))
				 value = QUOT +value + QUOT;
			declaration += mapName+DOT+"put" +OPENBRACKET+key+COMMA+value+CLOSEBRACKET+";\n";
		}
		declaration += NEWLINE + NEWLINE;
		return new Pair<String, String>(mapName, declaration);

	}

	private String buildList(Pair<String, List<String>> pair) {

		String listCode = "Arrays.asList( ";
		boolean start = true;
		for (String s1 : pair.getValue1()) {

			if (!start)
				listCode += COMMA;

			start = false;
			if (pair.getValue0().contains(STRING_CLASS)) {
				listCode += QUOT + s1 + QUOT;
			} else {
				listCode += s1;

			}

		}
		listCode += " )";
		return listCode;
	}

	private String getSingleValue(Pair<String, List<String>> pair) {
		return pair.getValue1().get(0);
	}

	public List<String> getDependentVars() {
		return dependentVars;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

}
