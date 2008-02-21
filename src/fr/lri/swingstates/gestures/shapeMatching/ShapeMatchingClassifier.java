package fr.lri.swingstates.gestures.shapeMatching;

import java.awt.Color;
import java.awt.Dimension;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import fr.lri.swingstates.animations.Animation;
import fr.lri.swingstates.canvas.CPolyLine;
import fr.lri.swingstates.canvas.CSegment;
import fr.lri.swingstates.canvas.CShape;
import fr.lri.swingstates.canvas.CStateMachine;
import fr.lri.swingstates.canvas.CText;
import fr.lri.swingstates.canvas.Canvas;
import fr.lri.swingstates.gestures.Gesture;
import fr.lri.swingstates.gestures.GestureExamples;
import fr.lri.swingstates.gestures.dollar1.Dollar1Utils;
import fr.lri.swingstates.sm.State;
import fr.lri.swingstates.sm.Transition;
import fr.lri.swingstates.sm.transitions.Drag;
import fr.lri.swingstates.sm.transitions.Press;
import fr.lri.swingstates.sm.transitions.Release;

public class ShapeMatchingClassifier {

	private double theta = Math.PI / 8;
	private double deltaTheta = Math.PI / 90;

	private int nbPoints = 64;

	private double currentDistance = -1;
	private double maximumDistance = 30;
	private double sizeScaleToSquare = 100;

	Vector<String> classNames = new Vector<String>();
	Vector<Vector<Point2D>> classTemplates = new Vector<Vector<Point2D>>();

	/**
	 * {@inheritDoc}
	 */
	public String classify(Gesture g) {
		double minScore = Double.MAX_VALUE;
		double currentScore;

		Vector<Point2D> inputPointsResampled = new Vector<Point2D>();
		Dollar1Utils.resample(g.getPoints(), nbPoints, inputPointsResampled);
		Dollar1Utils.scaleToSquare(inputPointsResampled, sizeScaleToSquare, inputPointsResampled);
		Dollar1Utils.translateToOrigin(inputPointsResampled, inputPointsResampled);

		int match = 0;
		int cpt = 0;
		for (Iterator<Vector<Point2D>> templatesIterator = classTemplates.iterator(); templatesIterator.hasNext();) {
			Vector<Point2D> next = templatesIterator.next();
			currentScore = Dollar1Utils.pathDistance(inputPointsResampled, next);
			if (currentScore < minScore) {
				minScore = currentScore;
				match = cpt;
			}
			cpt++;
		}

		currentDistance = minScore;
		if (currentDistance > maximumDistance)
			return null;
		return classNames.get(match);
	}
	
	public ClassifiedAndResampled classifyAndResample(Gesture g) {
		double minScore = Double.MAX_VALUE;
		double currentScore;
		
		Vector<Point2D> inputPointsResampled = new Vector<Point2D>();
		Dollar1Utils.resample(g.getPoints(), nbPoints, inputPointsResampled);
		Vector<Point2D> inputPointsResampledCopy = new Vector<Point2D>();
		for (Iterator<Point2D> iterator = inputPointsResampled.iterator(); iterator.hasNext(); )
			inputPointsResampledCopy.add((Point2D)iterator.next().clone());
		Dollar1Utils.scaleToSquare(inputPointsResampled, sizeScaleToSquare, inputPointsResampled);
		Dollar1Utils.translateToOrigin(inputPointsResampled, inputPointsResampled);

		int match = 0;
		int cpt = 0;
		for (Iterator<Vector<Point2D>> templatesIterator = classTemplates.iterator(); templatesIterator.hasNext();) {
			Vector<Point2D> next = templatesIterator.next();
			currentScore = Dollar1Utils.pathDistance(inputPointsResampled, next);
			if (currentScore < minScore) {
				minScore = currentScore;
				match = cpt;
			}
			cpt++;
		}

		currentDistance = minScore;
		if (currentDistance > maximumDistance)
			return null;
//		return classNames.get(match);
		return new ClassifiedAndResampled(classNames.get(match), inputPointsResampledCopy);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeClass(String className) {
		int index = classNames.indexOf(className);
		if (index == -1)
			System.err.println("no class " + className + " in the classifier");
		classNames.remove(index);
		classTemplates.remove(index);
	}

	public Vector<Point2D> getTemplate(String className) {
		int index = classNames.indexOf(className);
		if (index == -1)
			System.err.println("no class " + className + " in the classifier");
		return classTemplates.get(index);
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
	public void addClass(String className, Vector<Point2D> template) {
		int index = classNames.indexOf(className);
		if (index != -1) {
			classNames.remove(index);
			classTemplates.remove(index);
		}
		Vector<Point2D> newPoints = new Vector<Point2D>();
		Dollar1Utils.resample(template, getNbPoints(), newPoints);
		Dollar1Utils.scaleToSquare(newPoints, getSizeScaleToSquare(), newPoints);
		Dollar1Utils.translateToOrigin(newPoints, newPoints);
		
		classNames.add(className);
		classTemplates.add(newPoints);
	}

	protected Object read(DataInputStream in) throws IOException {
		int nClasses = in.readInt();
		for (int i = 0; i < nClasses; i++) {
			classNames.add(in.readUTF());
			int nbPoints = in.readInt();
			Vector<Point2D> points = new Vector<Point2D>();
			for (int j = 0; j < nbPoints; j++) {
				points.add(new Point2D.Double(in.readDouble(), in.readDouble()));
			}
			classTemplates.add(points);
		}
		return this;
	}

	protected void write(DataOutputStream out) throws IOException {
		out.writeInt(classNames.size());
		for (int i = 0; i < classNames.size(); i++) {
			out.writeUTF(classNames.get(i));
			out.writeInt(classTemplates.get(i).size());
			for (Iterator<Point2D> iterator = classTemplates.get(i).iterator(); iterator.hasNext();) {
				Point2D next = iterator.next();
				out.writeDouble(next.getX());
				out.writeDouble(next.getY());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Vector<String> sortedClasses(Gesture g) {
		Vector<String> sortedClasses = new Vector<String>();
		Vector<Double> sortedScores = new Vector<Double>();

		Vector<Point2D> inputPointsResampled = new Vector<Point2D>();
		Dollar1Utils.resample(g.getPoints(), nbPoints, inputPointsResampled);
		Dollar1Utils.scaleToSquare(inputPointsResampled, sizeScaleToSquare, inputPointsResampled);
		Dollar1Utils.translateToOrigin(inputPointsResampled, inputPointsResampled);

		double score;
		double minClassScore = 0;
		for (int nc = 0; nc < classNames.size(); nc++) {
			minClassScore = Integer.MAX_VALUE;
			Vector<Point2D> gesturePoints = classTemplates.get(nc);
			score = Dollar1Utils.distanceAtBestAngle(inputPointsResampled, gesturePoints, -theta, theta, deltaTheta);
			if (score < minClassScore)
				minClassScore = score;
			if (nc == 0) {
				sortedClasses.add(classNames.get(nc));
				sortedScores.add(minClassScore);
			} else {
				int i = 0;
				while (i < sortedScores.size() && sortedScores.get(i) < minClassScore)
					i++;
				sortedClasses.add(i, classNames.get(nc));
				sortedScores.add(i, minClassScore);
			}
		}

		return sortedClasses;
	}

	public int getNbPoints() {
		return nbPoints;
	}

	public double getSizeScaleToSquare() {
		return sizeScaleToSquare;
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

	public Canvas getRepresentatives(boolean displayName, int sizeBoundingBox, int startingPointSize) {
		Canvas canvas = new Canvas(100, 100);
		ArrayList<String> classes = new ArrayList<String>();
		classes.addAll(classNames);
		Collections.sort(classes);
		double yMax = 0;
		double xMax = sizeBoundingBox;
		for (Iterator<String> iterator = classes.iterator(); iterator.hasNext();) {
			String nextClass = iterator.next();
			CPolyLine representative = Dollar1Utils.asPolyLine(getTemplate(nextClass));
			if(displayName) {
				CText name = canvas.newText(0, yMax, nextClass);
				yMax = name.getMaxY();
				xMax = Math.max(xMax, name.getWidth());
			}
			CShape representativeBB = GestureExamples.showPreview(canvas, representative, 0, (int) yMax, sizeBoundingBox, startingPointSize, startingPointSize);
			yMax = representativeBB.getMaxY();
		}
		canvas.setPreferredSize(new Dimension((int)xMax, (int)yMax));
		return canvas;
	}
	
}
