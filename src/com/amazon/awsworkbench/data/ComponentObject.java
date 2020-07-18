package com.amazon.awsworkbench.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

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

	private List<ComponentAttribute> otherAttributes = new ArrayList<ComponentAttribute>();

	public ComponentObject(EObject self) {

		parentObject = self;
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

				if (self.eGet(esf) != null && !self.eGet(esf).toString().trim().isEmpty())
					otherAttributes.add(new ComponentAttribute(esf.getEType().getInstanceClassName(),
							self.eGet(esf).toString().trim(), esf.getName()));

			}
		}

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

}
