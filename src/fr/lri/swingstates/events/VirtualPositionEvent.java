/*  
 *   Authors: Caroline Appert (caroline.appert@lri.fr)
 *   Copyright (c) Universite Paris-Sud XI, 2007. All Rights Reserved
 *   Licensed under the GNU LGPL. For full terms see the file COPYING.
*/
package fr.lri.swingstates.events;

import java.awt.geom.Point2D;

/**
 * A virtual event originated on a given position.
 * 
 * @author Caroline Appert
 *
 */
public class VirtualPositionEvent extends VirtualEvent {

	Point2D point;
	
	/**
	 * Builds a <code>VirtualPositionEvent</code>.
	 * @param n The name of the event.
	 * @param pt The point on which this event occured.
	 */
	public VirtualPositionEvent(String n, Point2D pt) {
		super(n);
		point = pt;
	}
	
	/**
	 * Builds a <code>VirtualPositionEvent</code>.
	 * @param pt The point on which this event occured.
	 */
	public VirtualPositionEvent(Point2D pt) {
		super("");
		point = pt;
	}

	/**
	 * @return The point on which this event occured.
	 */
	public Point2D getPoint() {
		return point;
	}
	
}
