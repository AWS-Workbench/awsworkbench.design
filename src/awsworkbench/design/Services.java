package awsworkbench.design;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import java.util.List;


import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;


import com.amazon.aws.workbench.model.awsworkbench.AppBuilder_core;
import com.amazon.aws.workbench.model.awsworkbench.ServiceResources;
import com.amazon.awsworkbench.EObjectParser;

/**
 * The services class used by VSM.
 */
public class Services {

	/**
	 * See
	 * http://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.sirius.doc%2Fdoc%2Findex.html&cp=24
	 * for documentation on how to write service methods.
	 */

	public final String VARNAME = "varName";
	public final String GENERATED_CLASS_NAME = "generatedClassName";

	public Collection<String> getPropertyValueStringAsCollection(EObject self, EStructuralFeature feature) {

		if (self.eGet(feature) == null || self.eGet(feature).toString().isEmpty()) {
			return new ArrayList<String>();
		}

		String[] arr = self.eGet(feature).toString().split(",");
		ArrayList<String> arrL = new ArrayList<String>();

		for (String s : arr) {
			arrL.add(s.trim());
		}

		return arrL;
	}

	public Collection<String> getDependsOnAsStringCollection(ServiceResources self) {

		ArrayList<String> values = new ArrayList<String>();

		for (ServiceResources s : self.getDependsON()) {
			String className = new String();
			String varName = new String();
			EList<EStructuralFeature> allEStructuralFeatures = s.eClass().getEAllStructuralFeatures();
			for (EStructuralFeature eStructuralFeature : allEStructuralFeatures) {

				if (eStructuralFeature.getName().equals(VARNAME)) {
					varName = s.eGet(eStructuralFeature).toString().trim().replace(' ', '_');

				} else if (eStructuralFeature.getName().equals(GENERATED_CLASS_NAME)) {
					className = s.eGet(eStructuralFeature).toString();

				}
			}
			values.add(className + " " + varName);
		}
		return values;

	}

	public String getLabel(EObject self, EStructuralFeature feature) {

		String label = new String();

		String featureName = feature.getName();

		if (featureName.indexOf("With") != -1)
			label = featureName.substring(0, featureName.indexOf("With"));
		else if (featureName.indexOf('_') != -1)
			label = featureName.substring(0, featureName.indexOf('_'));
		else
			label = featureName;

		return label;
	}

	public String getDescription(EObject self, EStructuralFeature feature) {

		String label = new String();

		String featureName = feature.getName();

		if (featureName.indexOf('_') != -1) {
			label = featureName.substring(featureName.indexOf('_') + 1, featureName.lastIndexOf('_'));
			label = label.replace("__", " , ");
			label = label.replace('_', '.');
			label = label.replace("java.lang.", "");
			label = label.replace("java.util.List.", "List of ");
			label = label.replace("java.util.Map.", "Map of ");
		}

		return label;
	}

	@SuppressWarnings("unchecked")
	public EObject removeValue(EObject self, EStructuralFeature feature, Object value) {

		System.out.println(value.getClass().getName() + " " + feature.getName());
		Collection<String> selectedValues = (Collection<String>) value;
		
		
		

		if (feature.getName().equalsIgnoreCase("dependsON")) {
			
			if(self instanceof ServiceResources) {
				List<String> varNames = new ArrayList<String>();
				for(String s : selectedValues) {
					varNames.add(s.split(" ")[1]);
				}
				
				ServiceResources s = (ServiceResources)self;
				EList<ServiceResources> dependsOnList = s.getDependsON();
				List<ServiceResources> removeList = new ArrayList<ServiceResources>();
				for(ServiceResources dependsElem: dependsOnList) {
					
					String dependsVarName =  getVarName(dependsElem);
					
					if(varNames.contains(dependsVarName))
						removeList.add(dependsElem);	
				}
				dependsOnList.removeAll(removeList);
				s.eSet(feature, dependsOnList);
				return s;
				
			
			}

		} else {

			if (self.eGet(feature) != null && !self.eGet(feature).toString().isEmpty()) {
				List<String> existing = new ArrayList<String>(Arrays.asList(self.eGet(feature).toString().split(",")));
				existing.removeAll(selectedValues);
				self.eSet(feature, String.join(",", existing));
			}
		}

		return self;
	}

	private String getVarName(ServiceResources dependsElem) {
		EList<EStructuralFeature> allEStructuralFeatures = dependsElem.eClass().getEAllStructuralFeatures();
		for (EStructuralFeature eStructuralFeature : allEStructuralFeatures) {

			if (eStructuralFeature.getName().equals(VARNAME)) {
				return dependsElem.eGet(eStructuralFeature).toString().trim().replace(' ', '_');

			} 
		}
		return null;
		
	}

	public EObject addValue(EObject self, EStructuralFeature feature, String newValue) {

		System.out.println(newValue);
		if (feature.getName().endsWith("AsMap") && (newValue.indexOf(':') == -1 || newValue.indexOf(",") != -1)) {

			return self;
		}

		if (self.eGet(feature) != null && !self.eGet(feature).toString().isEmpty()) {
			List<String> existing = new ArrayList<String>(Arrays.asList(self.eGet(feature).toString().split(",")));
			existing.add(newValue.trim());
			self.eSet(feature, String.join(",", existing));
		} else
			self.eSet(feature, newValue.trim());

		return self;
	}

	public Collection<String> generateCode(AppBuilder_core self, String className) {

		EObjectParser parser = new EObjectParser();

		try {
			parser.generateCode(self, className);
		} catch (Exception e) {
			
			e.printStackTrace();
		}

		
		return null;
	}

	public Collection<ServiceResources> getDependsOn(ServiceResources s) {

		return s.getDependsON();
	}

	public void addDependsOn(ServiceResources source, ServiceResources target) {
		source.getDependsON().add(target);
	}

	
}
