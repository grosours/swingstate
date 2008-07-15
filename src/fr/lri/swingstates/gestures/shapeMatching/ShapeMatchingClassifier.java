/*  
 *   Authors: Caroline Appert (caroline.appert@lri.fr)
 *   Copyright (c) Universite Paris-Sud XI, 2007. All Rights Reserved
 *   Licensed under the GNU LGPL. For full terms see the file COPYING.
 */
package fr.lri.swingstates.gestures.shapeMatching;

import java.awt.geom.Point2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import fr.lri.swingstates.gestures.AbstractClassifier;
import fr.lri.swingstates.gestures.Gesture;
import fr.lri.swingstates.gestures.GestureClass;
import fr.lri.swingstates.gestures.GestureUtils;
import fr.lri.swingstates.gestures.Score;

/**
 * A very simple recognizer that performs simple shape matching based on a single example per class (one template):
 * <ol>
 * <li> Resample the gesture to classify so it contains the number of uniformly spaced points as the gesture templates contained in this classifier. </li>
 * <li> For each template, scale the input gesture so its bounding box matches the bounding box of the template
 * and compute the sum of distances point to point between the template points and the input gesture points. </li>
 * <li> Returns the name of the class for the template that minimizes this sum of distances. </li>
 * </ol>
 * 
 * @author Caroline Appert
 *
 */
public class ShapeMatchingClassifier extends AbstractClassifier {

	class ResampledGestureClass extends GestureClass {

		private Vector<Vector<Point2D>> resampledGestures = new Vector<Vector<Point2D>>();

		ResampledGestureClass() {
			super();
		}

		ResampledGestureClass(String n) {
			super(n);
		}

		/**
		 * {@inheritDoc} Each time a gesture is added, a vector of points
		 * corresponding to this gesture as resampled, rotated and scaled is
		 * computed and stored in <code>resampledGestures</code>.
		 */
		public void addExample(Gesture gesture) {
			super.addExample(gesture);
			Vector<Point2D> newPoints = new Vector<Point2D>();
			GestureUtils.resample(gesture.getPoints(), ShapeMatchingClassifier.this.getNbPoints(), newPoints);
			GestureUtils.scaleToSquare(newPoints, ShapeMatchingClassifier.this.getSizeScaleToSquare(), newPoints);
			GestureUtils.translateToOrigin(newPoints, newPoints);
			resampledGestures.add(newPoints);
		}
		
		public void addResampledExample(Vector<Point2D> gesture) {
			super.addExample(null);
			resampledGestures.add(gesture);
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
		 * @return The vector of gesture examples as resampled and scaled.
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
			int nbPoints = ShapeMatchingClassifier.this.getNbPoints();
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

	protected ArrayList<ResampledGestureClass>    classes = new ArrayList<ResampledGestureClass>();

	protected double theta = Math.PI / 8;
	protected double deltaTheta = Math.PI / 90;

	private int nbPoints = 100;

	private double currentDistance = -1;
	private double maximumDistance = 30;
	private double sizeScaleToSquare = 100;
	protected int  minimumStrokeLength = 20;

	/**
	 * {@inheritDoc}
	 */
	public Vector<Double> distance(String gesture1, String gesture2) {
		Vector<Double> res = new Vector<Double>();
		res.add(GestureUtils.pathDistance(getTemplate(gesture1), getTemplate(gesture2)));
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	public double distance(Gesture gesture, String gesture2) {
		Vector<Point2D> inputPointsResampled = normalize(gesture);
		return distance(inputPointsResampled, gesture2);
	}

	public Vector<Point2D> normalize(Gesture gesture) {
		Vector<Point2D> inputPointsResampled = new Vector<Point2D>();
		GestureUtils.resample(gesture.getPoints(), nbPoints, inputPointsResampled);
		GestureUtils.scaleToSquare(inputPointsResampled, sizeScaleToSquare, inputPointsResampled);
		GestureUtils.translateToOrigin(inputPointsResampled, inputPointsResampled);
		return inputPointsResampled;
	}

	public double distance(Vector<Point2D> inputPointsResampled, String gesture2) {
		return distance(inputPointsResampled, getTemplate(gesture2));
	}

	public double distance(Vector<Point2D> inputPointsResampled1, Vector<Point2D> inputPointsResampled2) {
		return GestureUtils.pathDistance(inputPointsResampled1, inputPointsResampled2);
	}

	/**
	 * {@inheritDoc}
	 */
	public String classify(Gesture g) {
		if(GestureUtils.pathLength(g.getPoints()) < minimumStrokeLength) return null;

		double minScore = Double.MAX_VALUE;
		double currentScore;
		GestureClass recognized = null;

		Vector<Point2D> inputPointsResampled = normalize(g);

		for (Iterator<ResampledGestureClass> classesIterator = classes.iterator(); classesIterator.hasNext();) {
			ResampledGestureClass nextClass = classesIterator.next();
			if(nextClass.getResampledGestures().size() > 0) {
			for (Iterator<Vector<Point2D>> gesturesIterator = nextClass.getResampledGestures().iterator(); gesturesIterator.hasNext();) {
				Vector<Point2D> gesturePoints = gesturesIterator.next();
				currentScore = distance(inputPointsResampled, gesturePoints);
				if (currentScore < minScore) {
					minScore = currentScore;
					recognized = nextClass;
				}
			}
			} else {
				Vector<Point2D> gesturePoints = getTemplates().get(getClassesNames().indexOf(nextClass.getName()));
				currentScore = distance(inputPointsResampled, gesturePoints);
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
	 * Classifies a gesture and return the collection of resampled points for the input gesture.
	 * @param g The input gesture.
	 * @return a NamedGesture that contains the name of the recognized class and the set of resampled points.
	 */
	public NamedGesture classifyAndResample(Gesture g) {
		if(GestureUtils.pathLength(g.getPoints()) < minimumStrokeLength) return null;

		double minScore = Double.MAX_VALUE;
		double currentScore;
		GestureClass recognized = null;

		Vector<Point2D> inputPointsResampled = normalize(g);
		Vector<Point2D> bestTemplate = null;

		for (Iterator<ResampledGestureClass> classesIterator = classes.iterator(); classesIterator.hasNext();) {
			ResampledGestureClass nextClass = classesIterator.next();
			if(nextClass.getResampledGestures().size() > 0) {
				for (Iterator<Vector<Point2D>> gesturesIterator = nextClass.getResampledGestures().iterator(); gesturesIterator.hasNext();) {
					Vector<Point2D> gesturePoints = gesturesIterator.next();
					currentScore = distance(inputPointsResampled, gesturePoints);
					if (currentScore < minScore) {
						minScore = currentScore;
						recognized = nextClass;
						bestTemplate = gesturePoints;
					}
				}
			} else {
				Vector<Point2D> gesturePoints = getTemplates().get(getClassesNames().indexOf(nextClass.getName()));
				currentScore = distance(inputPointsResampled, gesturePoints);
				if (currentScore < minScore) {
					minScore = currentScore;
					recognized = nextClass;
					bestTemplate = gesturePoints;
				}
			}
		}
		currentDistance = minScore;
		if (currentDistance > maximumDistance)
			return null;
		Vector<Point2D> bestTemplateCopy = new Vector<Point2D>();
		for (Iterator<Point2D> iterator = bestTemplate.iterator(); iterator.hasNext();) {
			Point2D next = iterator.next();
			bestTemplateCopy.add(new Point2D.Double(next.getX(), next.getY()));
		}
		// previously, there was a copy of inputPointsResampled instead
		return new NamedGesture(recognized.getName(), inputPointsResampled, bestTemplateCopy);
	}

	/**
	 * Builds a new classifier by loading its definition in a file.
	 * 
	 * @param file
	 *            The name of the file containing the definition of the
	 *            classifier.
	 * @return The newly created classifier.
	 */
	public static ShapeMatchingClassifier newClassifier(String file) {
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
	public static ShapeMatchingClassifier newClassifier(File filename) {
		ShapeMatchingClassifier c = new ShapeMatchingClassifier();
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
	public static ShapeMatchingClassifier newClassifier(URL url) {
		ShapeMatchingClassifier c = new ShapeMatchingClassifier();
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
	public int addClass(String className) {
		int index = super.addClass(className);
		if(index == -1) return -1;
		ResampledGestureClass gcr = new ResampledGestureClass(className);
		classes.add(gcr);
		fireClassAdded(className);
		return index;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void renameClass(String previousClassName, String newClassName) {
		int index = classesNames.indexOf(previousClassName);
		if(index == -1) return;
		ResampledGestureClass gc = classes.get(index);
		gc.setName(newClassName);
		super.renameClass(previousClassName, newClassName);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void removeClass(String className) {
		int index = classesNames.indexOf(className);
		if(index == -1) return;
		super.removeClass(className);
		classes.remove(index);
		fireClassRemoved(className);
	}
	
	/**
	 * Adds a class and set the template for this class.
	 * @param className The name of the class to add
	 * @param template The template for the class <code>className</code>
	 */
	public void addClass(String className, Vector<Point2D> template) {
		int index = addClass(className);
		Vector<Point2D> newPoints = new Vector<Point2D>();
		GestureUtils.resample(template, getNbPoints(), newPoints);
		GestureUtils.scaleToSquare(newPoints, getSizeScaleToSquare(), newPoints);
		GestureUtils.translateToOrigin(newPoints, newPoints);
		templates.set(index, newPoints);
	}

	protected Object read(DataInputStream in) throws IOException {
		int nClasses = in.readInt();
		for (int i = 0; i < nClasses; i++) {
			classesNames.add(in.readUTF());
			int nbPoints = in.readInt();
			Vector<Point2D> points = new Vector<Point2D>();
			for (int j = 0; j < nbPoints; j++) {
				points.add(new Point2D.Double(in.readDouble(), in.readDouble()));
			}
			templates.add(points);
			ResampledGestureClass gestureClass = new ResampledGestureClass(classesNames.get(i));
			classes.add(gestureClass);
			gestureClass.read(in);
			
		}
//		try {
//			maximumDistance = in.readDouble();
//			minimumStrokeLength = in.readInt();
//		} catch(IOException ioe) {
//			maximumDistance = 30;
//			minimumStrokeLength = 20;
//		}
		return this;
	}

	protected void write(DataOutputStream out) throws IOException {
		out.writeInt(classesNames.size());
		for (int i = 0; i < classesNames.size(); i++) {
			out.writeUTF(classesNames.get(i));
			out.writeInt(templates.get(i).size());
			for (Iterator<Point2D> iterator = templates.get(i).iterator(); iterator.hasNext();) {
				Point2D next = iterator.next();
				out.writeDouble(next.getX());
				out.writeDouble(next.getY());
			}
			classes.get(i).write(out);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Vector<Score> sortedClasses(Gesture g) {
		Vector<Score> sortedScores = new Vector<Score>();
		Vector<Point2D> inputPointsResampled = normalize(g);

		double score;
		double minClassScore = 0;
		for (int nc = 0; nc < classes.size(); nc++) {
			minClassScore = Integer.MAX_VALUE;
			if(classes.get(nc).getResampledGestures().size() > 0) {
				for (Iterator<Vector<Point2D>> gesturesIterator = classes.get(nc).getResampledGestures().iterator(); gesturesIterator.hasNext();) {
					Vector<Point2D> gesturePoints = gesturesIterator.next();
					score = distance(inputPointsResampled, gesturePoints);
					if (score < minClassScore)
						minClassScore = score;
				}
			} else {
				Vector<Point2D> gesturePoints = getTemplates().get(nc);
				score = distance(inputPointsResampled, gesturePoints);
				if (score < minClassScore)
					minClassScore = score;
			}
			if (nc == 0) {
				sortedScores.add(new Score(classes.get(nc).getName(), minClassScore));
			} else {
				int i = 0;
				while (i < sortedScores.size() && sortedScores.get(i).getScore() < minClassScore)
					i++;
				sortedScores.add(i, new Score(classes.get(nc).getName(), minClassScore));
			}
		}

		return sortedScores;
	}

	/**
	 * Saves the definition of this classifier in a file.
	 * 
	 * @param filename
	 *            The name of the file where to write the definition of the
	 *            classifier.
	 */
	public void save(File filename) {
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new FileOutputStream(filename));
			write(dos);
			dos.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeExample(Gesture example) {
		for (Iterator<ResampledGestureClass> iterator = classes.iterator(); iterator.hasNext();) {
			ResampledGestureClass next = iterator.next();
			if(next != null) {
				next.removeExample(example);
				fireExampleRemoved(next.getName(), example);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void addExample(String className, Gesture example) {
		int index = classesNames.indexOf(className);
		if(index == -1) return;
		ResampledGestureClass gestureClass = classes.get(index);
		if(gestureClass != null) {
			gestureClass.addExample(example);
			fireExampleAdded(className, example);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Vector<Gesture> getExamples(String className)
			throws UnsupportedOperationException {
		int index = classesNames.indexOf(className);
		if(index == -1) return null;
		ResampledGestureClass gc = classes.get(index);
		return gc.getGestures();
	}

	/**
	 * The side size of the bounding box to which the gesture is scaled after having being resampled.
	 * @param size The side size of the bounding box
	 */
	public void setSizeScaleToSquare(int size) {
		this.sizeScaleToSquare = size;
	}

	/**
	 * Sets the template gesture for a given existing class of gestures in this classifier.
	 * @param className the name of the class of gestures.
	 * @param template the template for the class className.
	 */
	public void setTemplate(String className, Vector<Point2D> template) {
		int index = classesNames.indexOf(className);
		if(index == -1) return;
		templates.remove(index);
		Vector<Point2D> newPoints = new Vector<Point2D>();
		GestureUtils.resample(template, getNbPoints(), newPoints);
		GestureUtils.scaleToSquare(newPoints, getSizeScaleToSquare(), newPoints);
		GestureUtils.translateToOrigin(newPoints, newPoints);
		templates.add(index, newPoints);
		fireTemplateSet(className, newPoints);
	}

	/**
	 * @return The maximum score threshold for recognition.
	 */
	public double getThreshold() {
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
	public void setThreshold(double maximumDistance) {
		this.maximumDistance = maximumDistance;
	}

	public int getMinimumStrokeLength() {
		return minimumStrokeLength;
	}

	public void setMinimumStrokeLength(int minimumStrokeLength) {
		this.minimumStrokeLength = minimumStrokeLength;
	}
	
	public int getNbPoints() {
		return nbPoints;
	}

	public double getSizeScaleToSquare() {
		return sizeScaleToSquare;
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
	public void reset() {
		super.reset();
		classes.clear();
	}

}
