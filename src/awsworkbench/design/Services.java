package awsworkbench.design;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl;

/**
 * The services class used by VSM.
 */
public class Services {

	/**
	 * See
	 * http://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.sirius.doc%2Fdoc%2Findex.html&cp=24
	 * for documentation on how to write service methods.
	 */
	public Collection<String> getPropertyValueStringAsCollection(EObject self, EStructuralFeature feature) {
		
		
		if(self.eGet(feature) == null || self.eGet(feature).toString().isEmpty())
		{
			return new ArrayList<String>();
		}

		String[] arr = self.eGet(feature).toString().split(",");
		ArrayList<String> arrL = new ArrayList<String>();

		for (String s : arr) {
			arrL.add(s.trim());
		}

		return arrL;
	}
	
	public EObject removeValue(EObject self, EStructuralFeature feature, Object value) {
		
		
		Collection<String> selectedValues = (Collection<String>) value;
		if(self.eGet(feature) != null || !self.eGet(feature).toString().isEmpty())
		{
			List<String> existing = Arrays.asList(self.eGet(feature).toString().split(","));
			existing.removeAll(selectedValues);
			self.eSet(feature, String.join(",", existing));
		}
	
		
		return self;
	}

}
