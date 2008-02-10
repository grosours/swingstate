/*  
 *   Authors: Caroline Appert (caroline.appert@lri.fr)
 *   Copyright (c) Universite Paris-Sud XI, 2007. All Rights Reserved
 *   Licensed under the GNU LGPL. For full terms see the file COPYING.
*/
package fr.lri.swingstates.gestures.rubine;

/*******************************************************************************
 * The algorithm and original C code are: (C) Copyright, 1990 by Dean Rubine,
 * Carnegie Mellon University Permission to use this code for noncommercial
 * purposes is hereby granted. Permission to copy and distribute this code is
 * hereby granted provided this copyright notice is retained. All other rights
 * reserved.
 ******************************************************************************/

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import fr.lri.swingstates.canvas.CPolyLine;
import fr.lri.swingstates.gestures.AbstractClassifier;
import fr.lri.swingstates.gestures.Gesture;
import fr.lri.swingstates.gestures.GestureClass;
import fr.lri.swingstates.gestures.Score;

/**
 * A classifier that implements rubine's algorithm to classify gestures.
 * 
 * @author Caroline Appert
 * 
 */
public class RubineClassifier extends AbstractClassifier {

	private static double EPSILON = 1.0e-6;

	private ArrayList<Double> cnst = null;
	private Vector<Vector<Double>> weights = null;
	private Matrix invAvgCov;
	private boolean compiled;

//	private double probabilityThreshold = 1.0;
	private int mahalanobisThreshold = 1000;

//	private transient double currentProbability = 0;
	private transient double currentDistance = 0;

	/**
	 * Builds a new classifier by loading its definition in a file.
	 * 
	 * @param file
	 *            The name of the file containing the definition of the
	 *            classifier.
	 * @return The newly created classifier.
	 */
	public static RubineClassifier newClassifier(String file) {
		return newClassifier(new File(file));
	}

	/**
	 * Builds a new classifier by loading its definition in a url.
	 * 
	 * @param url
	 *            The url containing the definition of the classifier.
	 * @return The newly created classifier.
	 */
	public static RubineClassifier newClassifier(URL url) {
		RubineClassifier c = new RubineClassifier();
		try {
			URLConnection urlc = null;
			urlc = url.openConnection();
			c.read(new DataInputStream(urlc.getInputStream()));
			c.compile();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return c;
	}

	private double mahalanobisDistance(Vector<Double> v, Vector<Double> u, Matrix sigma) {
		Vector<Double> tmp = VectorUtility.minus(v, u);
		double quadForm = VectorUtility.quadraticForm(tmp, sigma);
		return quadForm;
	}

	private void fix(Matrix avgcov) {
		double[] det = new double[1];
		BitVector bv = new BitVector();
		// just add the features one by one, discarding any that cause the
		// matrix to be non-invertible
		bv.zero();
		for (int i = 0; i < RubineGestureClass.NFEATURES; i++) {
			bv.set(i);
			avgcov.slice(bv, bv).invert(det);
			if (Math.abs(det[0]) <= EPSILON)
				bv.clear(i);
		}
		Matrix m = avgcov.slice(bv, bv).invert(det);
		if (Math.abs(det[0]) <= EPSILON)
			System.err.println("Can't fix classifier!\n");

		invAvgCov = m.deSlice(0.0, RubineGestureClass.NFEATURES, RubineGestureClass.NFEATURES, bv, bv);
	}

	/**
	 * Builds a new classifier by loading its definition in a file.
	 * 
	 * @param filename
	 *            The name of the file containing the definition of the
	 *            classifier.
	 * @return The newly created classifier.
	 */
	public static RubineClassifier newClassifier(File filename) {
		RubineClassifier c = new RubineClassifier();
		try {
			c.read(new DataInputStream(new FileInputStream(filename)));
			c.compile();
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
	public void reset() {
		cnst = null;
		weights = null;
		init();
		super.reset();
		compiled = false;
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
		weights.remove(i);
		cnst.remove(i);
		compiled = false;
	}

	/**
	 * Returns a graphical representation for a given class of gestures. The
	 * graphical representation is the one which minimizes the distance with
	 * vector of features characterizing this gesture class.
	 * 
	 * @param className
	 *            the name of the gesture class.
	 * @return A representative polyline for the gesture class having name
	 *         <code>className</code>.
	 */
	public CPolyLine getRepresentative(String className) {
		compile();
		int i = 0;
		GestureClass gestureClass = null;

		for (; i < classes.size(); i++) {
			gestureClass = classes.get(i);
			if (className.compareTo(gestureClass.getName()) == 0)
				break;
		}

		if (i >= classes.size())
			return null;

		CPolyLine representative = null;
		Gesture next = null;
		double maxValue = Double.MIN_VALUE;

		for (Iterator<Gesture> iterator = gestureClass.getGestures().iterator(); iterator.hasNext();) {
			next = iterator.next();
			Vector<Double> fv = RubineGestureClass.compute(next);
			double value = VectorUtility.scalarProduct(weights.get(i), fv) + cnst.get(i);
			if (value > maxValue) {
				maxValue = value;
				representative = next.asPolyLine();
			}
		}
		if (representative != null)
			representative.setFilled(false);
		return representative;
	}

	void compute() {
		if (classes.size() == 0)
			return;

		Matrix avgcov = new Matrix(RubineGestureClass.NFEATURES, RubineGestureClass.NFEATURES, true);
		int ne = 0;
		int nc;
		ArrayList<GestureClass> d = classes;

		for (nc = 0; nc < classes.size(); nc++) {
			ne += d.get(nc).getNumberOfExamples();
			/* should do : avgcov += d->Sumcov; for triangular */
			for (int i = 0; i < RubineGestureClass.NFEATURES; i++)
				for (int j = i; j < RubineGestureClass.NFEATURES; j++)
					avgcov.items[i][j] += ((RubineGestureClass) d.get(nc)).getSumCov().items[i][j];
		}

		int denom = ne - classes.size();
		if (denom <= 0) {
			System.out.println("no examples, denom=" + denom + "\n");
			return;
		}

		double oneoverdenom = 1.0 / denom;
		/* should have : avgcov *= oneoverdenom and detriangularize */
		for (int i = 0; i < RubineGestureClass.NFEATURES; i++)
			for (int j = i; j < RubineGestureClass.NFEATURES; j++) {
				avgcov.items[i][j] *= oneoverdenom;
				avgcov.items[j][i] = avgcov.items[i][j];
			}

		/* invert the avg covariance matrix */
		double[] det = new double[1];

		invAvgCov = avgcov.invert(det);
		if (Math.abs(det[0]) <= EPSILON)
			fix(avgcov);

		/* now compute discrimination functions */
		d = classes;

		init();

		Vector<Vector<Double>> w = weights;
		ArrayList<Double> cst = cnst;
		for (nc = 0; nc < classes.size(); nc++) {
			if (nc >= w.size())
				w.add(VectorUtility.mult(((RubineGestureClass) d.get(nc)).getAverage(), invAvgCov));
			else
				w.set(nc, VectorUtility.mult(((RubineGestureClass) d.get(nc)).getAverage(), invAvgCov));
			cst.set(nc, -0.5 * VectorUtility.scalarProduct(w.get(nc), ((RubineGestureClass) d.get(nc)).getAverage()));
			/* could add log(priorprob class) to cnst */
		}
		compiled = true;
	}

	private void init() {
		if (cnst == null) {
			cnst = new ArrayList<Double>();
			for (int i = 0; i < classes.size(); i++)
				cnst.add(0.0);
		}
		if (weights == null) {
			weights = new Vector<Vector<Double>>();
			for (int i = 0; i < classes.size(); i++)
				weights.add(new Vector<Double>(RubineGestureClass.NFEATURES));
		}
	}

	/**
	 * Compiles this classifier (i.e. performs training).
	 */
	private void compile() {
		if (!compiled)
			compute();
	}

	/**
	 * Recognizes a gesture.
	 * 
	 * @param g
	 *            The gesture to recognize
	 * @return The class of gestures that best fit to g.
	 */
	public String classify(Gesture g) {
		compile();

		Vector<Double> fv = RubineGestureClass.compute(g);
		Vector<Vector<Double>> wghts = weights;
		ArrayList<Double> cst = cnst;
		ArrayList<GestureClass> d = classes;
		double[] values = new double[classes.size()];
		GestureClass maxclass = null;
		double maxvalue = -Double.MAX_VALUE;

		for (int nc = 0; nc < classes.size(); nc++) {
			double value = VectorUtility.scalarProduct(wghts.get(nc), fv) + cst.get(nc);
			values[nc] = value;
			if (value > maxvalue) {
				maxclass = d.get(nc);
				maxvalue = value;
			}
		}

		/* compute probability of non-ambiguity */
//		double denom = 0.;
//		for (int i = 0; i < classes.size(); i++) {
//			double delta = values[i] - maxvalue;
//			denom += Math.exp(delta);
//		}
//		currentProbability = 1.0 / denom;

		// calculate distance to mean of chosen class
		currentDistance = mahalanobisDistance(fv, ((RubineGestureClass) maxclass).getAverage(), invAvgCov);

//		System.out.println("currentProbability: " + currentProbability);
//		System.out.println("probabilityThreshold: " + probabilityThreshold);
//		System.out.println("currentDistance: " + currentDistance);
//		System.out.println("mahalanobisThreshold: " + mahalanobisThreshold);
//		if (currentProbability >= probabilityThreshold && currentDistance <= mahalanobisThreshold)
//			return maxclass.getName();
		if (currentDistance <= mahalanobisThreshold)
			return maxclass.getName();
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addClass(String className) {
		if (findClass(className) != null)
			return;
		RubineGestureClass gcr = new RubineGestureClass(className);
		classes.add(gcr);
		init();
		weights.add(new Vector<Double>(RubineGestureClass.NFEATURES));
		cnst.add(0.0);
		compiled = false;
	}

	/**
	 * Computes the vector of features for a given class of gestures.
	 * <ul>
	 * <li>(0) cosinus of initial angle</li>
	 * <li>(1) sinus of initial angle</li>
	 * <li>(2) length of bounding box diagonal</li>
	 * <li>(3) angle of bounding box diagonal</li>
	 * <li>(4) length between start and end points</li>
	 * <li>(5) cosinus of angle between start and end points</li>
	 * <li>(6) sinus of angle between start and end points</li>
	 * <li>(7) arc length of path</li>
	 * <li>(8) total angle traversed</li>
	 * <li>(9) sum of abs vals of angles traversed</li>
	 * <li>(10) sum of squares of angles traversed</li>
	 * <li>(11) duration of path</li>
	 * <li>(12) maximum speed</li>
	 * </ul>
	 * 
	 * @param className
	 *            The name of the class of gesture.
	 * @return The vector of features for the <code>className</code> class of
	 *         gestures.
	 */
	public Vector<Double> getFeatures(String className) {
		RubineGestureClass gClass = (RubineGestureClass) findClass(className);
		return gClass.getAverage();
	}

	/**
	 * @param className
	 *            The name of the class of gesture.
	 * @return The number of examples in <code>className</code> class of
	 *         gestures.
	 */
	public int getNbGestureExamples(String className) {
		RubineGestureClass gClass = (RubineGestureClass) findClass(className);
		return gClass.getNumberOfExamples();
	}

	protected Object read(DataInputStream in) throws IOException {
		int nClasses = in.readInt();
		for (int i = 0; i < nClasses; i++) {
			RubineGestureClass c = new RubineGestureClass();
			c.read(in);
			classes.add(c);
		}

		return this;
	}

	/**
	 * @return The probability of non ambiguity for the last recognized gesture.
	 */
//	public double getCurrentProbability() {
//		return currentProbability;
//	}

	/**
	 * @return The distance of the last recognized gesture to the average vector
	 *         of its gesture class.
	 */
	public double getCurrentDistance() {
		return currentDistance;
	}

	/**
	 * Sets a minimum probability of non ambiguity for recognition. If the
	 * probability is less, the gesture is non recognized (i.e. method
	 * <code>classify</code> returns null). By default, this probability is
	 * set to 1.
	 * 
	 * @param proba
	 *            The minimum probability of non ambiguity.
	 */
//	public void setMinimumProbability(double proba) {
//		probabilityThreshold = proba;
//	}

	/**
	 * @return The minimum probability of non ambiguity for recognition.
	 */
//	public double getMinimumProbability() {
//		return probabilityThreshold;
//	}

	/**
	 * Sets a maximum distance threshold for recognition. If the distance to the
	 * average vector of the closest gesture class is higher than distance
	 * threshold, the gesture is non recognized (i.e. method
	 * <code>classify</code> returns null). By default, this distance is set
	 * to 1000.
	 * 
	 * @param dist
	 *            The maximum distance to the average vector of recognized
	 *            class.
	 */
	public void setMaximumDistance(int dist) {
		mahalanobisThreshold = dist;
	}

	/**
	 * @return The maximum distance threshold for recognition
	 */
	public int getMaximumDistance() {
		return mahalanobisThreshold;
	}

	/**
	 * {@inheritDoc}
	 */
	public Vector<Score> sortedClasses(Gesture g) {
		compile();
		Vector<Score> sortedClasses = new Vector<Score>();

		Vector<Double> fv = RubineGestureClass.compute(g);
		Vector<Vector<Double>> wghts = weights;
		ArrayList<Double> cst = cnst;

//		for (int nc = 0; nc < classes.size(); nc++) {
//			double value = VectorUtility.scalarProduct(wghts.get(nc), fv) + cst.get(nc);
//			if (nc == 0) {
//				sortedClasses.add(new Score(classes.get(nc).getName(), value));
//			} else {
//				int i = 0;
//				while (i < sortedClasses.size() && sortedClasses.get(i).getScore() > value)
//					i++;
//				sortedClasses.add(i, new Score(classes.get(nc).getName(), value));
//			}
//		}
		
		Vector<Double> sortedScalarProduct = new Vector<Double>();
		for (int nc = 0; nc < classes.size(); nc++) {
			double value = VectorUtility.scalarProduct(wghts.get(nc), fv) + cst.get(nc);
			if (nc == 0) {
				sortedClasses.add(new Score(classes.get(nc).getName(), 
						mahalanobisDistance(fv, ((RubineGestureClass) classes.get(nc)).getAverage(), invAvgCov)));
				sortedScalarProduct.add(value);
			} else {
				int i = 0;
				while (i < sortedScalarProduct.size() && sortedScalarProduct.get(i) > value)
					i++;
				sortedClasses.add(i, new Score(classes.get(nc).getName(), 
						mahalanobisDistance(fv, ((RubineGestureClass) classes.get(nc)).getAverage(), invAvgCov)));
				sortedScalarProduct.add(i, value);
			}
		}
		return sortedClasses;
	}
}
