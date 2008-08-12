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

import fr.lri.swingstates.gestures.GestureUtils;
import fr.lri.swingstates.gestures.shapeMatching.ShapeMatchingClassifier;

/**
 * A classifier that implements $1 algorithm to classify gestures.
 * 
 * @author Caroline Appert
 * 
 */
public class Dollar1Classifier extends ShapeMatchingClassifier {

	public Dollar1Classifier() {
		super();
	}
	
	public double distance(String gesture1, String gesture2) {
		int index1 = classesNames.indexOf(gesture1);
		int index2 = classesNames.indexOf(gesture2);
		
		if(!Double.isNaN(distances[index1][index2])) {
			return distances[index1][index2];
		}
		
		Vector<Point2D> template1 = getTemplate(gesture1);
		Vector<Point2D> template2 = getTemplate(gesture2);
		
		double minDis = distance(template1, template2);
		ResampledGestureClass gc1 = classes.get(index1);
		Vector<Vector<Point2D>> examples1 = gc1.getResampledGestures();
		for (Iterator<Vector<Point2D>> iterator1 = examples1.iterator(); iterator1.hasNext();) {
			Vector<Point2D> ex1  = iterator1.next();
			double dis = distance(ex1, template2);
			if(dis < minDis) {
				minDis = dis;
			}
		}
		distances[index1][index2] = minDis;
		
		minDis = distance(template2, template1);
		ResampledGestureClass gc2 = classes.get(index2);
		Vector<Vector<Point2D>> examples2 = gc2.getResampledGestures();
		for (Iterator<Vector<Point2D>> iterator = examples2.iterator(); iterator.hasNext();) {
			Vector<Point2D> ex2  = iterator.next();
			double dis = distance(template1, ex2);
			if(dis < minDis) {
				minDis = dis;
			}
		}
		distances[index2][index1] = minDis;
		
		return distances[index1][index2];
	}
	
	public double distance(Vector<Point2D> inputPointsResampled1, Vector<Point2D> inputPointsResampled2) {
		Vector<Point2D> rotatedPoints1 = new Vector<Point2D>();
		Vector<Point2D> rotatedPoints2 = new Vector<Point2D>();
		GestureUtils.rotateToZero(inputPointsResampled1, rotatedPoints1);
		GestureUtils.rotateToZero(inputPointsResampled2, rotatedPoints2);
//		double dis = GestureUtils.distanceAtBestAngle(rotatedPoints1, rotatedPoints2, -theta, theta, deltaTheta);
		return GestureUtils.pathDistance(rotatedPoints1, rotatedPoints2);
//		return dis;
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
