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
import java.util.Vector;

import fr.lri.swingstates.gestures.GestureUtils;
import fr.lri.swingstates.gestures.shapeMatching.ShapeMatchingClassifier;

/**
 * A classifier that implements $1 algorithm to classify gestures.
 * 
 * @author Caroline Appert
 * 
 */
public class Dollar1Classifier extends ShapeMatchingClassifier {

	/**
	 * {@inheritDoc}
	 */
	public Vector<Double> distance(String gesture1, String gesture2) {
		Vector<Double> res = new Vector<Double>();
		Vector<Point2D> rotatedPoints1 = new Vector<Point2D>();
		Vector<Point2D> rotatedPoints2 = new Vector<Point2D>();
		GestureUtils.rotateToZero(getTemplate(gesture1), rotatedPoints1);
		GestureUtils.rotateToZero(getTemplate(gesture2), rotatedPoints2);
		double dis = GestureUtils.distanceAtBestAngle(rotatedPoints1, rotatedPoints2, -theta, theta, deltaTheta);
		res.add(dis);
		return res;
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

}
