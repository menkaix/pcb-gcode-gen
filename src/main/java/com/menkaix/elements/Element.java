package com.menkaix.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.pcbgcode.utilities.UnknownElementException;
import com.menkaix.project.Behaviour;
import com.menkaix.project.BitHead;
import com.menkaix.project.GcodeBehaviour;
import com.menkaix.project.GcodeProject;

public class Element implements Serializable {

	private static final long serialVersionUID = 194916028736735923L;

	private String name;
	private String subType;

	private WeakHashMap<String, Object> properties = new WeakHashMap<String, Object>();

	private transient List<Behaviour> behaviours = new ArrayList<Behaviour>();
	
	

	protected void checkMandatoryProperties(String... propertyNames) throws MissingPropertyException {
		
		ArrayList<String> missing = new ArrayList<String>() ;
		
		for (String string : propertyNames) {
			
			if(!properties.containsKey(string)) missing.add(string) ;
			
		}
		
		if(missing.size()>0) {
			String message = "missing property " ;
			for (String string : missing) {
				message += " -"+string ;
			}
			throw new MissingPropertyException(message);
		}
		
	}

	protected SimplePoint pointFromMap(Object mapIn) throws MissingPropertyException {

		Map<String, Double> map = (Map<String, Double>) mapIn;

		if (!map.containsKey("x"))
			throw new MissingPropertyException("missing property X");
		if (!map.containsKey("y"))
			throw new MissingPropertyException("missing property Y");
		// if(!map.containsKey("x")) throw new MissingPropertyException("missing
		// property X") ;

		SimplePoint ans = new SimplePoint();
		ans.setX(map.get("x"));
		ans.setY(map.get("y"));

		if (map.containsKey("z")) {
			ans.setZ(map.get("z"));
		}

		return ans;

	}
	
	public String previewGcode() {
		String ans = "----- GCode preveiw for "+getElementName();
		int i = 0 ;
		System.out.println(getBehaviours().size()+ " behaviours total");
		for (Behaviour behaviour : getBehaviours()) {
			System.out.println("#"+i++);
			if(behaviour instanceof GcodeBehaviour) {
				ans += ((GcodeBehaviour) behaviour).getGcode(new GcodeProject("test", BitHead.LASER))+"\n";
			}
		}
		
		 ans += "----- end GCode preveiw for "+getElementName();
		
		return ans ;
	}

	public void reloadBehaviour() throws MissingPropertyException, UnknownElementException {

		throw new UnknownElementException();
	}

	public Element() {
		setElementName(this.getClass().getSimpleName());
		setSubType(this.getClass().getSimpleName());
	}

	public Object getProperty(String key) {
		return properties.get(key);
	}

	public void setProperty(String k, Object value) {
		properties.put(k, value);
	}

	public List<Behaviour> getBehaviours() {

		return this.behaviours;
	}

	public String getElementName() {

		return this.name;
	}

	public void setElementName(String name) {
		this.name = name;

	}

	public WeakHashMap<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(WeakHashMap<String, Object> properties) {
		this.properties = properties;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

}
