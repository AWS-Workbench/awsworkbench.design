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
	public  static final String UNDERSCORE = "_";
	
	public  static final String LESSTHAN = "<";
	
	public  static final String GREATERTHAN = ">";

	private List<ComponentAttribute> otherAttributes = new ArrayList<ComponentAttribute>();

	private Map<String, List<String>> dependencies = new HashMap<String, List<String>>();
	
	private List<String> parents = new ArrayList<String>();

	public ComponentObject(EObject self, List<String> parents) throws Exception {

		parentObject = self;
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

	public String generateCode(Map<String, ComponentObject> componentMap) {
		String code = new String();

		code += getGeneratedClassName() + SPACE + getVarName() + EQUALS + getBuilderClassName() + CREATE + NEWLINE;

		for (ComponentAttribute attribute : otherAttributes) {
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

				String[] listCode = buildMap(attribute);

				code += listCode[1];
			}

		}

		code += ".build();\n";

		return code;
	}

	private String[] buildMap(ComponentAttribute attribute) {
		
		String[] returnCode = new String[2];
		
		String mapName = getVarName()+ UNDERSCORE + attribute.getName();
		
		MapAttribute mapAttribute = attribute.getMapAttribute(); 
		String declaration = "java.util.Map" + LESSTHAN + mapAttribute.getKeyClass() + COMMA + mapAttribute.getValueClass()+GREATERTHAN
				+ SPACE + mapName + EQUALS + " new " + "java.util.Map" + LESSTHAN + mapAttribute.getKeyClass() + COMMA +
				mapAttribute.getValueClass()+GREATERTHAN + "();\n";
		
		
		returnCode[0] = mapName;
		returnCode[1]= declaration;
		
		
		
		return returnCode;
		
	}

	private String buildList(Map<String, List<String>> attributeValues) {
		// TODO Auto-generated method stub
		
		
		String listCode = "Arrays.asList( ";

		for (String s : attributeValues.keySet()) {

			List<String> list = attributeValues.get(s);
			boolean start = true;
			for (String s1 : list) {

				if (!start)
					listCode += COMMA;

				start = false;
				if (s.contains(STRING_CLASS)) {
					listCode += QUOT + s1 + QUOT;
				} else {
					listCode += s1;

				}

			}

		}

		listCode += " )";

		return listCode;
	}

	private String getSingleValue(Map<String, List<String>> attributeValues) {

		for (String s : attributeValues.keySet()) {
			List<String> list = attributeValues.get(s);
			return list.get(0);
		}

		return null;

	}

}
