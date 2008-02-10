/*
 * Authors: Caroline Appert (caroline.appert@lri.fr) Copyright (c) Universite
 * Paris-Sud XI, 2007. All Rights Reserved Licensed under the GNU LGPL. For full
 * terms see the file COPYING.
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
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Vector;

import fr.lri.swingstates.canvas.CPolyLine;
import fr.lri.swingstates.gestures.AbstractClassifier;
import fr.lri.swingstates.gestures.Gesture;
import fr.lri.swingstates.gestures.GestureClass;
import fr.lri.swingstates.gestures.Score;

/**
 * A classifier that implements $1 algorithm to classify gestures.
 * 
 * @author Caroline Appert
 * 
 */
public class Dollar1Classifier extends AbstractClassifier {

	private double theta = Math.PI / 4;
	private double deltaTheta = Math.PI / 90;

	private double currentDistance = -1;
	private double maximumDistance = 30;
	private double sizeScaleToSquare = 100;

	private int nbPoints = 64;

	/**
	 * {@inheritDoc}
	 */
	public String classify(Gesture g) {
		double minScore = Double.MAX_VALUE;
		double currentScore;
		GestureClass recognized = null;

		Vector<Point2D> inputPointsResampled = new Vector<Point2D>();
		Dollar1Utils.resample(g.getPoints(), nbPoints, inputPointsResampled);
		Dollar1Utils.rotateToZero(inputPointsResampled, inputPointsResampled);
		Dollar1Utils.scaleToSquare(inputPointsResampled, sizeScaleToSquare, inputPointsResampled);
		Dollar1Utils.translateToOrigin(inputPointsResampled, inputPointsResampled);

		for (Iterator<GestureClass> classesIterator = classes.iterator(); classesIterator.hasNext();) {
			Dollar1GestureClass nextClass = (Dollar1GestureClass) classesIterator.next();
			for (Iterator<Vector<Point2D>> gesturesIterator = nextClass.getResampledGestures().iterator(); gesturesIterator.hasNext();) {
				Vector<Point2D> gesturePoints = gesturesIterator.next();
				currentScore = Dollar1Utils.distanceAtBestAngle(inputPointsResampled, gesturePoints, -theta, theta, deltaTheta);
				if (currentScore < minScore) {
					minScore = currentScore;
					recognized = nextClass;
				}
			}
		}
		currentDistance = minScore;
		if (currentDistance > maximumDistance)
			return null;
		return recognized.getName();
	}

	/**
	 * {@inheritDoc}
	 */
	public CPolyLine getRepresentative(String className) {
		int i = 0;
		Dollar1GestureClass gestureClass = null;
		for (; i < classes.size(); i++) {
			gestureClass = (Dollar1GestureClass) classes.get(i);
			if (className.compareTo(gestureClass.getName()) == 0)
				break;
		}

		Vector<Point2D> average = gestureClass.getAverage();
		CPolyLine representative = null;
		Vector<Point2D> next = null;
		double minValue = Double.MAX_VALUE;
		for (Iterator<Vector<Point2D>> gesturesIterator = gestureClass.getResampledGestures().iterator(); gesturesIterator.hasNext();) {
			next = gesturesIterator.next();
			double value = Dollar1Utils.distanceAtBestAngle(next, average, -theta, theta, deltaTheta);
			if (value < minValue) {
				minValue = value;
				representative = Dollar1Utils.asPolyLine(next);
			}
		}

		if (representative != null)
			representative.setFilled(false);
		return representative;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeClass(String className) {
		if (classes.size() == 0)
			System.err.println("no class " + className + " in the classifier");
		GestureClass gc = findClass(className);
		int i = classes.indexOf(gc);
		classes.remove(i);
	}

	/**
	 * Builds a new classifier by loading its definition in a file.
	 * 
	 * @param file
	 *            The name of the file containing the definition of the
	 *            classifier.
	 * @return The newly created classifier.
	 */
	public static Dollar1Classifier newClassifier(String file) {
		return newClassifier(new File(file));
	}

	/**
	 * Builds a new classifier by loading its definition in a file.
	 * 
	 * @param filename
	 *            The name of the file containing the definition of the
	 *            classifier.
	 * @return The newly created classifier.
	 */
	public static Dollar1Classifier newClassifier(File filename) {
		Dollar1Classifier c = new Dollar1Classifier();
		try {
			c.read(new DataInputStream(new FileInputStream(filename)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return c;
	}

	/**
	 * Builds a new classifier by loading its definition in a url.
	 * 
	 * @param url
	 *            The url containing the definition of the classifier.
	 * @return The newly created classifier.
	 */
	public static Dollar1Classifier newClassifier(URL url) {
		Dollar1Classifier c = new Dollar1Classifier();
		try {
			URLConnection urlc = null;
			urlc = url.openConnection();
			c.read(new DataInputStream(urlc.getInputStream()));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addClass(String className) {
		if (findClass(className) != null)
			return;
		Dollar1GestureClass gcr = new Dollar1GestureClass(className, this);
		classes.add(gcr);
	}

	/**
	 * @return The number of points used for resampling a gesture during $1
	 *         recognition process.
	 */
	public int getNbPoints() {
		return nbPoints;
	}

	/**
	 * @return The size of the bounding box side used for rescaling a gesture
	 *         during $1 recognition process.
	 */
	public double getSizeScaleToSquare() {
		return sizeScaleToSquare;
	}

	protected Object read(DataInputStream in) throws IOException {
		int nClasses = in.readInt();
		for (int i = 0; i < nClasses; i++) {
			Dollar1GestureClass c = new Dollar1GestureClass(this);
			c.read(in);
			classes.add(c);
		}
		return this;
	}

	/**
	 * @return The maximum score threshold for recognition.
	 */
	public double getMaximumDistance() {
		return maximumDistance;
	}

	/**
	 * Sets a minimum score threshold for recognition. If the distance is
	 * greater than this maximum distance, the gesture is not recognized (i.e.
	 * method <code>classify</code> returns null.
	 * 
	 * @param maximumDistance
	 *            The minimum score threshold for recognition.
	 */
	public void setMaximumDistance(double maximumDistance) {
		this.maximumDistance = maximumDistance;
	}

	/**
	 * @return The distance of the last recognized gesture.
	 */
	public double getCurrentDistance() {
		return currentDistance;
	}

	/**
	 * {@inheritDoc}
	 */
	public Vector<Score> sortedClasses(Gesture g) {
		Vector<Score> sortedClasses = new Vector<Score>();

		Vector<Point2D> inputPointsResampled = new Vector<Point2D>();
		Dollar1Utils.resample(g.getPoints(), nbPoints, inputPointsResampled);
		Dollar1Utils.rotateToZero(inputPointsResampled, inputPointsResampled);
		Dollar1Utils.scaleToSquare(inputPointsResampled, sizeScaleToSquare, inputPointsResampled);
		Dollar1Utils.translateToOrigin(inputPointsResampled, inputPointsResampled);

		double score;
		double minClassScore = 0;
		for (int nc = 0; nc < classes.size(); nc++) {
			minClassScore = Integer.MAX_VALUE;
			for (Iterator<Vector<Point2D>> gesturesIterator = ((Dollar1GestureClass) classes.get(nc)).getResampledGestures().iterator(); gesturesIterator.hasNext();) {
				Vector<Point2D> gesturePoints = gesturesIterator.next();
				score = Dollar1Utils.distanceAtBestAngle(inputPointsResampled, gesturePoints, -theta, theta, deltaTheta);
				if (score < minClassScore)
					minClassScore = score;
			}
			if (nc == 0) {
				sortedClasses.add(new Score(classes.get(nc).getName(), minClassScore));
			} else {
				int i = 0;
				while (i < sortedClasses.size() && sortedClasses.get(i).getScore() < minClassScore)
					i++;
				sortedClasses.add(i, new Score(classes.get(nc).getName(), minClassScore));
			}
		}

		return sortedClasses;
	}

}
