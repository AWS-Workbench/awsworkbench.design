package com.amazon.awsworkbench.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.javatuples.Pair;
import org.w3c.dom.CDATASection;

import com.amazon.awsworkbench.EObjectParser;

public class ComponentAttribute {

	private ComponentAttributeTypes type;

	private String value;

	private String name;

	private MapAttribute mapAttribute = new MapAttribute();

	private boolean canGenerate = true;

	private Pair<String, List<String>> dependencyList;

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
		// returnString += dependencyList.toString() + "\n";
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

			if (dependencyList != null && dependencyList.getValue0().equals(className)) {
				dependencyList.getValue1().addAll(values);
			} else {
				dependencyList = new Pair<String, List<String>>(className, new ArrayList<String>(values));
			}

		} else {
			String className = featureName.substring(featureName.indexOf('_') + 1, featureName.lastIndexOf('_'))
					.replace('_', '.');
			if (dependencyList != null && dependencyList.getValue0().equals(className)) {
				dependencyList.getValue1().add(featureValue);
			} else {
				dependencyList = new Pair<String, List<String>>(className, new ArrayList<String>());
				dependencyList.getValue1().add(featureValue);
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

	public Pair<String, List<String>> getAttributeValues() {
		return dependencyList;
	}

	public boolean isCanGenerate(Map<String, ComponentObject> componentMap) {

		List<String> toBeRemoved = new ArrayList<String>();

		if (type == ComponentAttributeTypes.REFERENCE || type == ComponentAttributeTypes.LIST) {

			for (String s : dependencyList.getValue1()) {
				if (componentMap.get(s) != null && !componentMap.get(s).isGenerated())
					toBeRemoved.add(s);
			}

		}

		if (type == ComponentAttributeTypes.MAP) {

			for (String s : mapAttribute.getValues().keySet()) {

				if (componentMap.get(s) != null && !componentMap.get(s).isGenerated())
					toBeRemoved.add(s);

			}
			for (String s : mapAttribute.getValues().values()) {

				if (componentMap.get(s) != null && !componentMap.get(s).isGenerated())
					toBeRemoved.add(s);

			}

		}

		for (String s : toBeRemoved)
			removeDependency(null, s);

		return canGenerate;
	}

	public void removeDependency(String className, String varName) {
		if (type == ComponentAttributeTypes.REFERENCE || type == ComponentAttributeTypes.LIST) {

			dependencyList.getValue1().remove(varName);
			if (dependencyList.getValue1().size() <= 0)
				canGenerate = false;
			else
				canGenerate = true;

		} else if (type == ComponentAttributeTypes.MAP) {

			mapAttribute.removeFromKey(varName);
			mapAttribute.removeFromValue(varName);

			if (mapAttribute.getValues().size() <= 0)
				canGenerate = false;
			else
				canGenerate = true;
		}

	}

	class MapAttribute {

		private String keyClass = new String();

		private String valueClass = new String();

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

		public void removeFromValue(String varName) {
			// TODO Auto-generated method stub
			List<String> tobeRemoved = new ArrayList<String>();

			for (String s : values.keySet()) {
				if (values.get(s).equals(varName))
					tobeRemoved.add(s);
			}
			for (String s : tobeRemoved)
				values.remove(s);

		}

		public void removeFromKey(String varName) {
			// TODO Auto-generated method stub
			values.remove(varName);
		}

		public MapAttribute() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public String toString() {
			String returnString = new String();

			returnString += keyClass + "\t\t" + valueClass + "\n";

			for (String s : values.keySet()) {

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
