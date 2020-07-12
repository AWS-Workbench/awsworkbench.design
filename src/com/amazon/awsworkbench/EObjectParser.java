package com.amazon.awsworkbench;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EContentsEList;

public class EObjectParser {

	public void parse(EObject self) {

		try {
			EList<EStructuralFeature> allEStructFeats = self.eClass().getEAllStructuralFeatures();

			for (EStructuralFeature esf : allEStructFeats)
				if (self.eGet(esf) != null)
					System.out.println(esf.getName() + " " + self.eGet(esf).toString());
				else
					System.out.println(esf.getName() + " " +esf.getDefaultValueLiteral());

			System.out.println(self.getClass().getName() + "\n---------\n");

			EList<EObject> children = self.eContents();

			for (EObject e : children) {
				parse(e);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
