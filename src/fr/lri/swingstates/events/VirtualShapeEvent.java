/*  
 *   Authors: Caroline Appert (caroline.appert@lri.fr)
 *   Copyright (c) Universite Paris-Sud XI, 2007. All Rights Reserved
 *   Licensed under the GNU LGPL. For full terms see the file COPYING.
*/
package fr.lri.swingstates.events;

import java.awt.geom.Point2D;

import fr.lri.swingstates.canvas.CShape;

/**
 * A virtual event originated on a <code>CShape</code>.
 * 
 * @author Caroline Appert
 *
 */
public class VirtualShapeEvent extends VirtualPositionEvent {

	private boolean hasAlreadyPicked = false;
	CShape cshape;
	int modifier;
	
	public boolean hasAlreadyPicked() {
		return hasAlreadyPicked;
	}

	public void setShape(CShape shape) {
		this.cshape = shape;
		hasAlreadyPicked = true;
	}

	/**
	 * Builds a <code>VirtualShapeEvent</code>.
	 * @param n The name of the event.
	 * @param shape The <code>CShape</code>.
	 * @param pt The point on which this event occured.
	 */
	public VirtualShapeEvent(String n, CShape shape, Point2D pt) {
		super(n, pt);
		cshape = shape;
		hasAlreadyPicked = true;
	}
	
	/**
	 * Builds a <code>VirtualShapeEvent</code>.
	 * @param shape The <code>CShape</code>.
	 * @param pt The point on which this event occured.
	 */
	public VirtualShapeEvent(CShape shape, Point2D pt) {
		super("", pt);
		cshape = shape;
	}
	
	/**
	 * @return The <code>CShape</code> on which this event occured.
	 */
	public CShape getShape() {
		return cshape;
	}

	/**
	 * @return The point on which this event occured.
	 */
	public Point2D getPoint() {
		return point;
	}

	/**
	 * @return The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT (constants of class <code>BasicInputStateMachine</code>)
	 */
	public int getModifier() {
		return modifier;
	}

}
