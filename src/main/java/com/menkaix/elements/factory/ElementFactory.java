package com.menkaix.elements.factory;

import com.menkaix.elements.ArcPath;
import com.menkaix.elements.Circle;
import com.menkaix.elements.Element;
import com.menkaix.elements.PolyLineElement;
import com.menkaix.elements.Rectangle;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.pcbgcode.utilities.UnknownElementException;

public class ElementFactory {

	public static Element create(Element base) throws MissingPropertyException, UnknownElementException {

		if (base.getSubType().equals(ArcPath.class.getSimpleName())) {
			ArcPath ans = new ArcPath();
			ans.setProperties(base.getProperties());
			ans.reloadBehaviour();
			return ans;
		} else if (base.getSubType().equals(Circle.class.getSimpleName())) {
			Circle ans = new Circle();
			ans.setProperties(base.getProperties());
			ans.reloadBehaviour();
			return ans;
		} else if (base.getSubType().equals(PolyLineElement.class.getSimpleName())) {
			PolyLineElement ans = new PolyLineElement();
			ans.setProperties(base.getProperties());
			ans.reloadBehaviour();
			return ans;
		} else if (base.getSubType().equals(Rectangle.class.getSimpleName())) {
			Rectangle ans = new Rectangle();
			ans.setProperties(base.getProperties());
			ans.reloadBehaviour();
			return ans;
		}

		throw new UnknownElementException();
	}

}
