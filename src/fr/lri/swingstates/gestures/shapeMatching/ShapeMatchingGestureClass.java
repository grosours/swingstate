package fr.lri.swingstates.gestures.shapeMatching;

import java.awt.geom.Point2D;
import java.util.Vector;

import fr.lri.swingstates.gestures.Gesture;
import fr.lri.swingstates.gestures.GestureClass;
import fr.lri.swingstates.gestures.dollar1.Dollar1Utils;

public class ShapeMatchingGestureClass extends GestureClass {
	
	private ShapeMatchingClassifier classifier;
	private Vector<Point2D> resampledGesture = new Vector<Point2D>();

	ShapeMatchingGestureClass(ShapeMatchingClassifier classifier) {
		super();
		this.classifier = classifier;
	}
	
	ShapeMatchingGestureClass(String n, ShapeMatchingClassifier classifier) {
		super(n);
		this.classifier = classifier;
	}
	
	/**
	 * <p>
	 * Adds an example of this class of gestures.
	 * Here, since it is a simple matching shape classifier
	 * that only stores one template per class. This gesture
	 * replaces the current template for this class.
	 * </p>
	 * 
	 * <p>
	 * Each time a gesture is added, a vector of points
	 * corresponding to this gesture as resampled, rotated and scaled is
	 * computed and stored in <code>resampledGestures</code>.
	 * </p>
	 * @param gesture
	 *            The gesture to add to this gesture class.
	 */
	public void addExample(Gesture gesture) {
		gestures.clear();
		super.addExample(gesture);
		Vector<Point2D> newPoints = new Vector<Point2D>();
		Dollar1Utils.resample(gesture.getPoints(), classifier.getNbPoints(), newPoints);
		Dollar1Utils.scaleToSquare(newPoints, classifier.getSizeScaleToSquare(), newPoints);
		Dollar1Utils.translateToOrigin(newPoints, newPoints);
		resampledGesture = newPoints;
	}
	

}
