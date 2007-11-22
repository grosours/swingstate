/*  
 *   Authors: Caroline Appert (caroline.appert@lri.fr)
 *   Copyright (c) Universite Paris-Sud XI, 2007. All Rights Reserved
 *   Licensed under the GNU LGPL. For full terms see the file COPYING.
*/
package fr.lri.swingstates.gestures.dollar1;

/*******************************************************************************
 * The algorithm and original C# code are from:
 * http://faculty.washington.edu/wobbrock/proj/dollar/ The algorithm is
 * described in: Wobbrock, J.O., Wilson, A.D., and Li, Y. 2007. Gestures without
 * libraries, toolkits or training: a $1 recognizer for user interface
 * prototypes. In proc.UIST'07.
 ******************************************************************************/

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Vector;

import fr.lri.swingstates.gestures.Gesture;
import fr.lri.swingstates.gestures.GestureClass;

/**
 * A class of gestures for $1 recognizer.
 * 
 * @author Caroline Appert
 * 
 */
public class Dollar1GestureClass extends GestureClass {

	private Dollar1Classifier classifier;
	private Vector<Vector<Point2D>> resampledGestures = new Vector<Vector<Point2D>>();

	Dollar1GestureClass(Dollar1Classifier classifier) {
		super();
		this.classifier = classifier;
	}

	Dollar1GestureClass(String n, Dollar1Classifier classifier) {
		super(n);
		this.classifier = classifier;
	}

	/**
	 * {@inheritDoc} Each time a gesture is added, a vector of points
	 * corresponding to this gesture as resampled, rotated and scaled is
	 * computed and stored in <code>resampledGestures</code>.
	 */
	public void addExample(Gesture gesture) {
		super.addExample(gesture);
		Vector<Point2D> newPoints = new Vector<Point2D>();
		Dollar1Utils.resample(gesture.getPoints(), classifier.getNbPoints(), newPoints);
		Dollar1Utils.rotateToZero(newPoints, newPoints);
		Dollar1Utils.scaleToSquare(newPoints, classifier.getSizeScaleToSquare(), newPoints);
		Dollar1Utils.translateToOrigin(newPoints, newPoints);
		resampledGestures.add(newPoints);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean removeExample(Gesture gesture) {
		if (!gestures.contains(gesture))
			return false;
		int index = gestures.indexOf(gesture);
		if (index != -1)
			resampledGestures.remove(index);
		return super.removeExample(gesture);
	}

	/**
	 * @return The vector of gesture examples as resampled, rotated and scaled.
	 * @see Dollar1GestureClass#addExample(Gesture)
	 */
	public Vector<Vector<Point2D>> getResampledGestures() {
		return resampledGestures;
	}

	/**
	 * @return The average vector of this class. A point#i in this vector is the
	 *         gravity center of points#i of all examples.
	 */
	public Vector<Point2D> getAverage() {
		int nbPoints = classifier.getNbPoints();
		Vector<Point2D> average = new Vector<Point2D>(nbPoints);
		double sumX, sumY;
		for (int i = 0; i < nbPoints; i++) {
			sumX = 0;
			sumY = 0;
			for (Iterator<Vector<Point2D>> iterator = resampledGestures.iterator(); iterator.hasNext();) {
				Point2D pt = iterator.next().get(i);
				sumX += pt.getX();
				sumY += pt.getY();
			}
			average.add(new Point2D.Double(sumX / resampledGestures.size(), sumY / resampledGestures.size()));
		}
		return average;
	}

}
