package com.amazon.awsworkbench.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.smartcardio.ATR;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.amazon.awsworkbench.EObjectParser;


public class ComponentAttribute {

	private ComponentAttributeTypes type;

	private String value;

	private String name;

	private MapAttribute mapAttribute = new MapAttribute();

	private Map<String, List<String>> attributeVals = new HashMap<String, List<String>>();

	public ComponentAttribute(EStructuralFeature feature, String value) throws Exception {

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
	
	@Override
	public String toString() {
		
		String returnString = new String();
		
		returnString += name + "\n";
		returnString += type + "\n";
		returnString += attributeVals.toString() + "\n";
		returnString += mapAttribute.toString() + "\n";
		
		return returnString;
		
	}

	private void extractAttributeClassAndValue(EStructuralFeature feature, String featureValue) throws Exception {

		String featureName = feature.getName();

		if (type == ComponentAttributeTypes.MAP) {

			mapAttribute = new MapAttribute(feature, featureName, featureValue);

		} else if (type == ComponentAttributeTypes.LIST) {
			String className = featureName.substring(featureName.indexOf('_') + 1, featureName.lastIndexOf('_'))
					.replace('_', '.');

			List<String> values = new ArrayList<String>(Arrays.asList(featureValue.split(",")));

			if (attributeVals.containsKey(className)) {
				attributeVals.get(className).addAll(values);
			} else {
				attributeVals.put(className, new ArrayList<String>());
				attributeVals.get(className).addAll(values);
			}

		} else {
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

	public String getName() {
		return name;
	}

	public MapAttribute getMapAttribute() {
		return mapAttribute;
	}

	public Map<String, List<String>> getAttributeValues() {
		return attributeVals;
	}

	class MapAttribute {

		private String keyClass = new String();

		private String valueClass  = new String();

		private Map<String, String> values = new HashMap<String, String>();

		public MapAttribute(EStructuralFeature feature, String featureName, String featureValue) throws Exception {
			// TODO Auto-generated constructor stub

			String[] mapVals = featureName.substring(featureName.indexOf('_') + 1, featureName.lastIndexOf('_'))
					.split("__");

			if (mapVals.length > 2)
				EObjectParser.showError("Maps has more than 2 components");

			keyClass = mapVals[0].replace('_', '.');
			valueClass = mapVals[1].replace('_', '.');
			breakFeatureValueToMap(featureValue);

		}

		public MapAttribute() {
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public String toString()
		{
			String returnString = new String();
			
			returnString += keyClass +  "\t\t" + valueClass + "\n";
			
			for(String s : values.keySet()) {
				
				returnString += s + "\t\t" + values.get(s) + "\n";
			}
			
			return returnString;
			
			
		
		}
		

		private void breakFeatureValueToMap(String featureValue) {

			for (String s : featureValue.split(",")) {
				String[] s1 = s.split(":");
				values.put(s1[0], s1[1]);
			}

		}

		public String getKeyClass() {
			return keyClass;
		}

		public String getValueClass() {
			return valueClass;
		}

		public Map<String, String> getValues() {
			return values;
		}

	}

}
