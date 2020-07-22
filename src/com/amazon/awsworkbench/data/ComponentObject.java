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
	private String resourceVariableName;
	private String identifier;
	private boolean isGenerated = false;
	private String generatedCode;
	private EObject parentEcoreObject;
	private String generatedClassName;

	private String parentConstruct = null;
	private String additionalCode;
	private List<String> dependentVariables = new ArrayList<String>();
	private boolean visited = false;

	private List<ComponentAttribute> nonCoreAttributes = new ArrayList<ComponentAttribute>();

	private Map<String, List<String>> dependencyMap = new HashMap<String, List<String>>();

	private List<String> parents = new ArrayList<String>();

	public final String VARNAME = "varName";
	public final String IDENTIFIER = "identifier";
	public final String GENERATED_CLASS_NAME = "generatedClassName";
	public final String ADDITIONAL_CODE = "additionalCode";
	public final String ASMAP = "AsMap";
	public final String ASREFERENCE = "AsReference";
	public final String ASLIST = "AsList";

	public static final String SPACE = " ";

	public static final String EQUALS = "=";

	public static final String CREATE = "create";
	
	public static final String BUILD = "build";
	
	public static final String BUILDER = "Builder";

	public static final String DOT = ".";

	public static final String OPENBRACKET = "(";

	public static final String CLOSEBRACKET = ")";

	public static final String QUOT = "\"";

	public static final String NEWLINE = "\n";

	public static final String STRING_CLASS = "java.lang.String";
	
	public static final String JAVALANG_PACKAGE = "java.lang";

	public static final String COMMA = ",";
	
	public static final String UNDERSCORE = "_";
	
	public static final String DOUBLEUNDERSCORE = "__";

	public static final String LESSTHAN = "<";

	public static final String GREATERTHAN = ">";
	
	public static final String TILDE = "~";
	
	public static final String HYPHEN = "-";
	
	public static final String COLON = ":";
	
	public static final String SEMICOLON = ";";

	public ComponentObject(EObject eCoreObject, List<String> parents, String parentName) throws Exception {

		parentEcoreObject = eCoreObject;
		if (parentName != null) {
			dependentVariables.add(parentName);
			this.parentConstruct = parentName;
		}
		this.parents.addAll(parents);

		EList<EStructuralFeature> allEStructuralFeatures = eCoreObject.eClass().getEAllStructuralFeatures();

		for (EStructuralFeature eStructuralFeature : allEStructuralFeatures) {
			if (eStructuralFeature.getName().equals(VARNAME)) {
				resourceVariableName = eCoreObject.eGet(eStructuralFeature).toString().trim().replace(' ', '_');
			} else if (eStructuralFeature.getName().equals(IDENTIFIER)) {
				identifier = eCoreObject.eGet(eStructuralFeature).toString().trim().replace(' ', '_');
			} else if (eStructuralFeature.getName().equals(GENERATED_CLASS_NAME)) {
				generatedClassName = eCoreObject.eGet(eStructuralFeature).toString();
			} else if (eStructuralFeature.getName().equals(ADDITIONAL_CODE)
					&& eCoreObject.eGet(eStructuralFeature) != null
					&& !eCoreObject.eGet(eStructuralFeature).toString().trim().isEmpty()) {
				additionalCode = eCoreObject.eGet(eStructuralFeature).toString();
			} else {

				if (!(eStructuralFeature instanceof EAttribute))
					continue;

				String featureName = eStructuralFeature.getName();

				if (eCoreObject.eGet(eStructuralFeature) != null
						&& !eCoreObject.eGet(eStructuralFeature).toString().trim().isEmpty()) {

					String value = eCoreObject.eGet(eStructuralFeature).toString().trim();

					if (featureName.endsWith(ASREFERENCE)) {

						addReferenceDependency(featureName, value);
					} else if (featureName.endsWith(ASLIST)) {

						addListDependency(featureName, value);
					} else if (featureName.endsWith(ASMAP)) {

						addMapDependency(featureName, value);
					}

					ComponentAttribute componentAttribute = new ComponentAttribute(eStructuralFeature, value);

					for (List<String> lists : dependencyMap.values()) {
						dependentVariables.addAll(lists);
					}

					nonCoreAttributes.add(componentAttribute);

				}

			}
		}

	}

	private void addReferenceDependency(String featureName, String featureValue) {

		String className = featureName
				.substring(featureName.indexOf(UNDERSCORE) + 1, featureName.lastIndexOf(UNDERSCORE))
				.replace(UNDERSCORE, DOT);

		String tempFeatureValue = featureValue.trim();

		if (isVariableExpression(tempFeatureValue)) // surrounded by ~
		{
			tempFeatureValue = featureValue.substring(1, featureValue.indexOf(DOT));

		}
		
		if (isStaticExpression(tempFeatureValue)) // surrounded by -
		{
			return;

		}

		if (dependencyMap.containsKey(className)) {
			dependencyMap.get(className).add(tempFeatureValue);
		} else {
			dependencyMap.put(className, new ArrayList<String>());
			dependencyMap.get(className).add(tempFeatureValue);
		}

	}

	private boolean isStaticExpression(String featureValue) {
		return (featureValue.trim().startsWith(HYPHEN) && featureValue.trim().endsWith(HYPHEN));
	}

	private boolean isVariableExpression(String featureValue) {
		
		return (featureValue.trim().startsWith(TILDE) && featureValue.trim().endsWith(TILDE));
	}

	

	private void addListDependency(String featureName, String featureValue) {

		String className = featureName.substring(featureName.indexOf(UNDERSCORE) + 1, featureName.lastIndexOf(UNDERSCORE))
				.replace(UNDERSCORE, DOT);
		if (className.startsWith(JAVALANG_PACKAGE))
			return;

		List<String> rawValues = new ArrayList<String>(Arrays.asList(featureValue.split(COMMA)));
		
		List<String> variableValues  = new ArrayList<String>();
		
		for(String rawValue : rawValues ) {
			if(isVariableExpression(rawValue)) {
				variableValues.add(rawValue.trim().substring(1, rawValue.indexOf(DOT)));
				continue;
			}
			else if(isStaticExpression(rawValue))
				continue;
			else
				variableValues.add(rawValue.trim());
			
				
		}

		if (dependencyMap.containsKey(className)) {
			dependencyMap.get(className).addAll(variableValues);
		} else {
			dependencyMap.put(className, new ArrayList<String>());
			dependencyMap.get(className).addAll(variableValues);
		}

	}

	private void addMapDependency(String featureName, String featureValue) throws Exception {

		String[] mapVals = featureName.substring(featureName.indexOf(UNDERSCORE) + 1, featureName.lastIndexOf(UNDERSCORE))
				.split(DOUBLEUNDERSCORE);

		if (mapVals.length > 2)
			EObjectParser.showError("Maps has more than 2 components");

		String className1 = mapVals[0].replace(UNDERSCORE, DOT);
		String className2 = mapVals[1].replace(UNDERSCORE, DOT);
		Map<String, String> featureMap = breakFeatureValueToMap(featureValue);

		if (!className1.startsWith(JAVALANG_PACKAGE)) {

			if (dependencyMap.containsKey(className1)) {
				dependencyMap.get(className1).addAll(featureMap.keySet());
			} else {
				dependencyMap.put(className1, new ArrayList<String>());
				dependencyMap.get(className1).addAll(featureMap.keySet());
			}

		}

		if (!className2.startsWith(JAVALANG_PACKAGE)) {

			if (dependencyMap.containsKey(className2)) {
				dependencyMap.get(className2).addAll(featureMap.values());
			} else {
				dependencyMap.put(className2, new ArrayList<String>());
				dependencyMap.get(className2).addAll(featureMap.values());
			}

		}

	}

	private Map<String, String> breakFeatureValueToMap(String featureValue) {

		HashMap<String, String> featureValues = new HashMap<String, String>();

		for (String nameValuePair : featureValue.split(COMMA)) {
			String[] nameAndValue = nameValuePair.split(COLON);
			featureValues.put(nameAndValue[0], nameAndValue[1]);
		}

		return featureValues;
	}

	public String getVarName() {

		return resourceVariableName;
	}

	public String getIdentifier() {

		return identifier;
	}

	public boolean isGenerated() {
		return isGenerated;
	}

	public String getGeneratedCode() {
		return generatedCode;
	}

	public EObject getParentObject() {
		return parentEcoreObject;
	}

	public String getGeneratedClassName() {
		return generatedClassName;
	}

	public String getBuilderClassName() {
		return generatedClassName + DOT + BUILDER;
	}

	public String getAdditionalCode() {
		return additionalCode;
	}

	public List<ComponentAttribute> getOtherAttributes() {
		return nonCoreAttributes;
	}

	public Map<String, List<String>> getDependencies() {
		return dependencyMap;
	}

	@Override
	public String toString() {

		String resultString = new String();

		resultString += getVarName();
		resultString += SPACE + getGeneratedClassName() + NEWLINE;
		resultString += SPACE + parents + NEWLINE;

		return resultString;

	}

	public void removeDependency(String className, String varName) {

		for (ComponentAttribute componentAttribute : nonCoreAttributes) {

			componentAttribute.removeDependency(className, varName);
			dependentVariables.remove(varName);
		}
	}

	public String generateCode(Map<String, ComponentObject> componentMap) {

		String code = new String();

		if (parentConstruct != null) {

			code += getGeneratedClassName() + SPACE + getVarName() + EQUALS + getBuilderClassName() + DOT + CREATE
					+ OPENBRACKET + parentConstruct + COMMA + QUOT + getIdentifier() + QUOT + CLOSEBRACKET + NEWLINE;

		} else {
			code += getGeneratedClassName() + SPACE + getVarName() + EQUALS + getBuilderClassName() + DOT + CREATE
					+ OPENBRACKET + CLOSEBRACKET + NEWLINE;
		}
		for (ComponentAttribute attribute : nonCoreAttributes) {

			if (!attribute.canGenerate(componentMap))
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

		code += DOT + BUILD + OPENBRACKET + CLOSEBRACKET + SEMICOLON + NEWLINE;

		this.generatedCode = code;
		this.isGenerated = true;

		return code;
	}

	private Pair<String, String> buildMap(ComponentAttribute attribute) {
		String mapName = getVarName() + UNDERSCORE + attribute.getName();

		MapAttribute mapAttribute = attribute.getMapAttribute();
		String declaration = "java.util.Map" + LESSTHAN + mapAttribute.getKeyClass() + COMMA
				+ mapAttribute.getValueClass() + GREATERTHAN + SPACE + mapName + EQUALS + " new " + "java.util.HashMap"
				+ LESSTHAN + mapAttribute.getKeyClass() + COMMA + mapAttribute.getValueClass() + GREATERTHAN + "();\n";

		declaration += NEWLINE;
		String key = new String();
		String value = new String();
		for (String s : mapAttribute.getValues().keySet()) {
			if (mapAttribute.getKeyClass().equals(STRING_CLASS))
				key = QUOT + s + QUOT;
			else
				key = s;

			value = mapAttribute.getValues().get(s);

			if (mapAttribute.getValueClass().equals(STRING_CLASS))
				value = QUOT + value + QUOT;
			declaration += mapName + DOT + "put" + OPENBRACKET + key + COMMA + value + CLOSEBRACKET + ";\n";
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
		return dependentVariables;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

}
