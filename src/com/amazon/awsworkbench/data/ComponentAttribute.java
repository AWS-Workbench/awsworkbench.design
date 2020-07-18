package com.amazon.awsworkbench.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EStructuralFeature;

public class ComponentAttribute {

	private ComponentAttributeTypes type;

	private String value;

	private String name;

	private Map<String, List<String>> attributeVals = new HashMap<String, List<String>>();

	public ComponentAttribute(EStructuralFeature feature, String value) {

		extractName(feature);
		String featureName = feature.getName();

		if (feature.getEType() instanceof EEnum)

			type = ComponentAttributeTypes.ENUM;
		else if (feature.getEType() instanceof EDataType) {

			String instanceClassName = feature.getEType().getInstanceClassName();

			if (featureName.endsWith("AsReference"))
				type = ComponentAttributeTypes.REFERENCE;
			else if (featureName.endsWith("AsList"))
				type = ComponentAttributeTypes.LIST;
			else if (featureName.endsWith("AsMap"))
				type = ComponentAttributeTypes.MAP;
			else if (instanceClassName.contains("Boolean"))
				type = ComponentAttributeTypes.BOOLEAN;
			else if (instanceClassName.contains("int"))
				type = ComponentAttributeTypes.INTEGER;
			else if (instanceClassName.contains("String"))
				type = ComponentAttributeTypes.STRING;	

		}
		extractAttributeClassAndValue(feature, value);

	}

	private void extractAttributeClassAndValue(EStructuralFeature feature, String featureValue) {

		String featureName = feature.getName();

		if (type == ComponentAttributeTypes.MAP) {
			
		}
		else if (type == ComponentAttributeTypes.LIST) {
			String className = featureName.substring(featureName.indexOf('_') + 1, featureName.lastIndexOf('_'))
					.replace('_', '.');
			

			List<String> values = new ArrayList<String>(Arrays.asList(featureValue.split(",")));

			if (attributeVals.containsKey(className)) {
				attributeVals.get(className).addAll(values);
			} else {
				attributeVals.put(className, new ArrayList<String>());
				attributeVals.get(className).addAll(values);
			}
			
		}
		else
		{
			String className = featureName.substring(featureName.indexOf('_') + 1, featureName.lastIndexOf('_'))
					.replace('_', '.');
			if (attributeVals.containsKey(className)) {
				attributeVals.get(className).add(featureValue);
			} else {
				attributeVals.put(className, new ArrayList<String>());
				attributeVals.get(className).add(featureValue);
			}
		}

	}

	private void extractName(EStructuralFeature feature) {
		String featureName = feature.getName();

		if (featureName.indexOf("With") != -1)
			name = featureName.substring(0, featureName.indexOf("With"));
		else if (featureName.indexOf('_') != -1)
			name = featureName.substring(0, featureName.indexOf('_'));
		else
			name = featureName;

	}

	public ComponentAttributeTypes getType() {
		return type;
	}

	public void setType(ComponentAttributeTypes type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
