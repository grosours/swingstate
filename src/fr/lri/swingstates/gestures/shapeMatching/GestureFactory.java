package fr.lri.swingstates.gestures.shapeMatching;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import fr.lri.swingstates.canvas.Canvas;
import fr.lri.swingstates.gestures.Gesture;

/**
 * This class builds several predefined gestures in a bounding box 100x100.
 * 
 * @author Caroline Appert
 *
 */
public class GestureFactory {

	public static Vector<Point2D> rotateGesture(Vector<Point2D> gesture, double angleInRadians) {
		Vector<Point2D> rotated = new Vector<Point2D>();
		AffineTransform rotation = AffineTransform.getRotateInstance(angleInRadians);
		for(Iterator<Point2D> iterator = gesture.iterator(); iterator.hasNext(); ) {
			Point2D rotatedPoint = new Point2D.Double();
			rotation.transform(iterator.next(), rotatedPoint);
			rotated.add(rotatedPoint);
		}
		return rotated;
	}
	
	public static Gesture mirrorGesture(Gesture gesture, boolean vertical) {
		Gesture symmetric = new Gesture();
//		AffineTransform rotation = AffineTransform.getRotateInstance(angleInRadians);
//		for(Iterator<Point2D> iterator = gesture.getPoints().iterator(); iterator.hasNext(); ) {
//			Point2D rotatedPoint = new Point2D.Double();
//			rotation.transform(iterator.next(), rotatedPoint);
//			rotated.addPoint(rotatedPoint);
//		}
		return symmetric;
	}
	
	public static Vector<Point2D> getVGesture() {
		Vector<Point2D> gesture = new Vector<Point2D>();
		gesture.add(new Point2D.Double(0, 0));
		gesture.add(new Point2D.Double(50, 100));
		gesture.add(new Point2D.Double(100, 0));
		return gesture;
	}
	
	public static Vector<Point2D> getLineGesture() {
		Vector<Point2D> gesture = new Vector<Point2D>();
		gesture.add(new Point2D.Double(0, 50));
		gesture.add(new Point2D.Double(100, 50));
		return gesture;
	}
	
	public static Gesture getOpenCircleGesture() {
		return null;
	}
	
	public static Vector<Point2D> getGammaGesture() {
		Vector<Point2D> gesture = new Vector<Point2D>();
		gesture.add(new Point2D.Double(0, 0));
		
		GeneralPath gp = new GeneralPath();
		gp.moveTo(0, 0);
		gp.curveTo(100, 50, 100, 100, 50, 100);
		gp.curveTo(0, 100, 0, 50, 100, 0);
		double[] coords = new double[6];
		for (PathIterator pi = gp.getPathIterator(null, 1); ! pi.isDone(); pi.next()) {
			pi.currentSegment(coords);
			gesture.add(new Point2D.Double(coords[0], coords[1]));
        }
		
		return gesture;
	}
	
	public static Vector<Point2D> getUGesture() {
		Vector<Point2D> gesture = new Vector<Point2D>();
		gesture.add(new Point2D.Double(0, 0));
		gesture.add(new Point2D.Double(0, 50));
		
		GeneralPath gp = new GeneralPath();
		gp.moveTo(0, 50);
		gp.quadTo(0, 100, 50, 100);
		gp.quadTo(100, 100, 100, 50);
		double[] coords = new double[6];
		for (PathIterator pi = gp.getPathIterator(null, 1); ! pi.isDone(); pi.next()) {
			pi.currentSegment(coords);
			gesture.add(new Point2D.Double(coords[0], coords[1]));
        }
		
		gesture.add(new Point2D.Double(100, 0));
		return gesture;
	}
	
	public static Vector<Point2D> getCrossGesture() {
		Vector<Point2D> gesture = new Vector<Point2D>();
		gesture.add(new Point2D.Double(0, 50));
		gesture.add(new Point2D.Double(100, 50));
		gesture.add(new Point2D.Double(100, 50));
		
		GeneralPath gp = new GeneralPath();
		gp.moveTo(0, 50);
		gp.quadTo(0, 100, 50, 100);
		gp.quadTo(100, 100, 100, 50);
		double[] coords = new double[6];
		for (PathIterator pi = gp.getPathIterator(null, 1); ! pi.isDone(); pi.next()) {
			pi.currentSegment(coords);
			gesture.add(new Point2D.Double(coords[0], coords[1]));
        }
		
		gesture.add(new Point2D.Double(100, 0));
		return gesture;
	}
	
	static void createClassifierTest() {
		ShapeMatchingClassifier d1c = new ShapeMatchingClassifier();
		
		d1c.addClass("V1", getVGesture());
		d1c.addClass("V2", rotateGesture(getVGesture(), Math.PI));
		d1c.addClass("U1", getUGesture());
		d1c.addClass("U2", rotateGesture(getUGesture(), Math.PI));
		d1c.addClass("Gamma1", getGammaGesture());
		d1c.addClass("V3", rotateGesture(getVGesture(), Math.PI/2));
		d1c.addClass("V4", rotateGesture(getVGesture(), 3*Math.PI/2));
		
		d1c.addClass("L1", getLineGesture());
		d1c.addClass("L2", rotateGesture(getLineGesture(), Math.PI));
		d1c.addClass("L3", rotateGesture(getLineGesture(), Math.PI/2));
		d1c.addClass("L4", rotateGesture(getLineGesture(), 3*Math.PI/2));
		
		d1c.save(new File("classifier"+File.separator+"automatic"+File.separator+"automatic-classifier.cl"));
		
		Canvas canvas = d1c.getRepresentatives(true, 50, 6);
		JScrollPane jsp = new JScrollPane(canvas);
		jsp.setPreferredSize(new Dimension(100, 400));
		JFrame frame = new JFrame();
		
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(jsp, BorderLayout.WEST);
		frame.getContentPane().add(d1c.getRecognitionArea(400, 400), BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}
	
	public static void main(String[] args) {
		createClassifierTest();
	}
}

