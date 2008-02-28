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
import java.util.Iterator;
import java.util.Vector;

import fr.lri.swingstates.canvas.CPolyLine;
import fr.lri.swingstates.gestures.AbstractClassifier;
import fr.lri.swingstates.gestures.Gesture;
import fr.lri.swingstates.gestures.GestureUtils;
import fr.lri.swingstates.gestures.Score;

public class ShapeMatchingClassifier extends AbstractClassifier {
	
	class ClassifiedAndResampled {

		private String className;
		private Vector<Point2D> resampled;

		public ClassifiedAndResampled(String className, Vector<Point2D> resampled) {
			this.className = className;
			this.resampled = resampled;
		}

		public String getClassName() {
			return className;
		}

		public Vector<Point2D> getResampled() {
			return resampled;
		}

	}

	private double theta = Math.PI / 8;
	private double deltaTheta = Math.PI / 90;

	private int nbPoints = 64;

	private double currentDistance = -1;
	private double maximumDistance = 30;
	private double sizeScaleToSquare = 100;

	/**
	 * {@inheritDoc}
	 */
	public String classify(Gesture g) {
		double minScore = Double.MAX_VALUE;
		double currentScore;

		Vector<Point2D> inputPointsResampled = new Vector<Point2D>();
		GestureUtils.resample(g.getPoints(), nbPoints, inputPointsResampled);
		GestureUtils.scaleToSquare(inputPointsResampled, sizeScaleToSquare, inputPointsResampled);
		GestureUtils.translateToOrigin(inputPointsResampled, inputPointsResampled);

		int match = 0;
		int cpt = 0;
		for (Iterator<Vector<Point2D>> templatesIterator = templates.iterator(); templatesIterator.hasNext();) {
			Vector<Point2D> next = templatesIterator.next();
			currentScore = GestureUtils.pathDistance(inputPointsResampled, next);
			if (currentScore < minScore) {
				minScore = currentScore;
				match = cpt;
			}
			cpt++;
		}

		currentDistance = minScore;
		if (currentDistance > maximumDistance)
			return null;
		return classesNames.get(match);
	}
	
	public ClassifiedAndResampled classifyAndResample(Gesture g) {
		double minScore = Double.MAX_VALUE;
		double currentScore;
		
		Vector<Point2D> inputPointsResampled = new Vector<Point2D>();
		GestureUtils.resample(g.getPoints(), nbPoints, inputPointsResampled);
		Vector<Point2D> inputPointsResampledCopy = new Vector<Point2D>();
		for (Iterator<Point2D> iterator = inputPointsResampled.iterator(); iterator.hasNext(); )
			inputPointsResampledCopy.add((Point2D)iterator.next().clone());
		GestureUtils.scaleToSquare(inputPointsResampled, sizeScaleToSquare, inputPointsResampled);
		GestureUtils.translateToOrigin(inputPointsResampled, inputPointsResampled);

		int match = 0;
		int cpt = 0;
		for (Iterator<Vector<Point2D>> templatesIterator = templates.iterator(); templatesIterator.hasNext();) {
			Vector<Point2D> next = templatesIterator.next();
			currentScore = GestureUtils.pathDistance(inputPointsResampled, next);
			if (currentScore < minScore) {
				minScore = currentScore;
				match = cpt;
			}
			cpt++;
		}

		currentDistance = minScore;
		if (currentDistance > maximumDistance)
			return null;
		return new ClassifiedAndResampled(classesNames.get(match), inputPointsResampledCopy);
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
	 * Adds a class and set the template for this class.
	 * @param className The name of the class to add
	 * @param template The template for the class <code>className</code>
	 */
	public void addClass(String className, Vector<Point2D> template) {
		int index = addClass(className);
		if (index != -1) {
			Vector<Point2D> newPoints = new Vector<Point2D>();
			GestureUtils.resample(template, getNbPoints(), newPoints);
			GestureUtils.scaleToSquare(newPoints, getSizeScaleToSquare(), newPoints);
			GestureUtils.translateToOrigin(newPoints, newPoints);
			setTemplate(className, newPoints);
		}
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
		}
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
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Vector<Score> sortedClasses(Gesture g) {
		Vector<Score> sortedScores = new Vector<Score>();

		Vector<Point2D> inputPointsResampled = new Vector<Point2D>();
		GestureUtils.resample(g.getPoints(), nbPoints, inputPointsResampled);
		GestureUtils.scaleToSquare(inputPointsResampled, sizeScaleToSquare, inputPointsResampled);
		GestureUtils.translateToOrigin(inputPointsResampled, inputPointsResampled);

		double score;
		double minClassScore = 0;
		for (int nc = 0; nc < classesNames.size(); nc++) {
			minClassScore = Integer.MAX_VALUE;
			Vector<Point2D> gesturePoints = templates.get(nc);
			score = GestureUtils.distanceAtBestAngle(inputPointsResampled, gesturePoints, -theta, theta, deltaTheta);
			if (score < minClassScore)
				minClassScore = score;
			if (nc == 0) {
				sortedScores.add(new Score(classesNames.get(nc), minClassScore));
			} else {
				int i = 0;
				while (i < sortedScores.size() && sortedScores.get(i).getScore() < minClassScore)
					i++;
				sortedScores.add(i, new Score(classesNames.get(nc), minClassScore));
			}
		}

		return sortedScores;
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

	/**
	 * {@inheritDoc}
	 */
	public CPolyLine getRepresentative(String className) {
		Vector<Point2D> template = getTemplate(className);
		CPolyLine polyline = new CPolyLine(template.firstElement());
		Iterator<Point2D> iterator = template.iterator();
		iterator.next();
		while(iterator.hasNext())
			polyline.lineTo(iterator.next());
		return polyline;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeExample(Gesture gesture)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Removing an example from a ShapeMatchingClassifier does not make sense.");
	}

	/**
	 * {@inheritDoc}
	 */
	public void addExample(String className, Gesture example)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Adding an example to a class in a ShapeMatchingClassifier does not make sense.");
	}

	/**
	 * {@inheritDoc}
	 */
	public Vector<Gesture> getExamples(String className)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException("A class in a ShapeMatchingClassifier contains only one template which can be obtained using the method getTemplate(String).");
	}
	
	
}
