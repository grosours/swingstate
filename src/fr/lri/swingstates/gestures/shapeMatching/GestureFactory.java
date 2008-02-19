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

import fr.lri.swingstates.canvas.CPolyLine;
import fr.lri.swingstates.canvas.Canvas;

/**
 * This class builds several predefined gestures in a bounding box 100x100.
 * 
 * @author Caroline Appert
 *
 */
public class GestureFactory {
	
	public static short VERTICAL_MIRROR   = 1;
	public static short HORIZONTAL_MIRROR = 2;

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
	
	public static Vector<Point2D> mirrorGesture(Vector<Point2D> gesture, short axis) {
		Vector<Point2D> mirror = new Vector<Point2D>();
		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;
		for(Iterator<Point2D> iterator = gesture.iterator(); iterator.hasNext(); ) {
			Point2D point = iterator.next();
			minX = Math.min(point.getX(), minX);
			maxX = Math.max(point.getX(), maxX);
			minY = Math.min(point.getY(), minY);
			maxY = Math.max(point.getY(), maxY);
		}
		if(axis == HORIZONTAL_MIRROR) {
			for(Iterator<Point2D> iterator = gesture.iterator(); iterator.hasNext(); ) {
				Point2D originalPoint = iterator.next();
				Point2D mirrorPoint = new Point2D.Double(originalPoint.getX(), (maxY - originalPoint.getY()) + minY);
				mirror.add(mirrorPoint);
			}
		} else {
			// axis == VERTICAL_MIRROR
			for(Iterator<Point2D> iterator = gesture.iterator(); iterator.hasNext(); ) {
				Point2D originalPoint = iterator.next();
				Point2D mirrorPoint = new Point2D.Double((maxX - originalPoint.getX()) + minX, originalPoint.getY());
				mirror.add(mirrorPoint);
			}
		}
		return mirror;
	}
	
	public static Vector<Point2D> getVGesture() {
		Vector<Point2D> gesture = new Vector<Point2D>();
		gesture.add(new Point2D.Double(0, 0));
		gesture.add(new Point2D.Double(50, 100));
		gesture.add(new Point2D.Double(100, 0));
		return gesture;
	}
	
	public static Vector<Point2D> getLGesture() {
		Vector<Point2D> gesture = new Vector<Point2D>();
		gesture.add(new Point2D.Double(0, 0));
		gesture.add(new Point2D.Double(0, 100));
		gesture.add(new Point2D.Double(100, 100));
		return gesture;
	}
	
	public static Vector<Point2D> getSpiralGesture(double nbRevolutions, double a, double b) {
		Vector<Point2D> gesture = new Vector<Point2D>();
		double maxTheta = nbRevolutions * (Math.PI * 2) ;
		double radius;
		for(double angle = 0; angle <= maxTheta; angle += Math.PI / 24) {
			radius = a + b * angle;
			gesture.add(new Point2D.Double(radius * Math.cos(angle), radius * Math.sin(angle)));
		}
		return gesture;
	}
	
	public static Vector<Point2D> getHelixGesture(double nbRevolutions, double h_increment, double h, double k, double a, double b) {
		Vector<Point2D> gesture = new Vector<Point2D>();
		double maxTheta = nbRevolutions * (Math.PI * 2) ;
		double y_center = k;
		for(double angle = 0; angle <= maxTheta; angle += Math.PI / 24) {
			// increment the y-coordinate of the ellipse center
			y_center += h_increment;
			gesture.add(new Point2D.Double(h + a * Math.cos(angle), y_center + b * Math.sin(angle)));
		}
		return gesture;
	}
	
	public static Vector<Point2D> getWaveGesture(int nbWaves) {
		Vector<Point2D> gesture = new Vector<Point2D>();
		GeneralPath gp = new GeneralPath();
		gp.moveTo(0, 50);
		float xInit = 0;
		float xEnd;
		float xMiddle;
		float yInit = 50;
		float h = 50;
		for(int i = 0; i < nbWaves; i++) {
			xEnd = xInit + 50;
			xMiddle = (xInit + xEnd) / 2;
			gp.curveTo(
					xMiddle - (50f / 6), yInit - h, 
					xMiddle + (50f / 6), yInit - h, 
					xEnd, yInit);
			xInit += 50;
			h = -h;
		}
		double[] coords = new double[6];
		for (PathIterator pi = gp.getPathIterator(null, 1); ! pi.isDone(); pi.next()) {
			pi.currentSegment(coords);
			gesture.add(new Point2D.Double(coords[0], coords[1]));
        }
		
		return gesture;
	}
	
	public static Vector<Point2D> getZigZagGesture(int nbZigZags) {
		Vector<Point2D> gesture = new Vector<Point2D>();
		gesture.add(new Point2D.Double(0, 0));
		for(int i = 0; i < nbZigZags; i++) {
			gesture.add(new Point2D.Double(100*i + 50, 100));
			gesture.add(new Point2D.Double(100*i + 100, 0));
		}
		return gesture;
	}
	
	public static Vector<Point2D> getArchesGesture(int nbArches) {
		Vector<Point2D> gesture = new Vector<Point2D>();
		GeneralPath gp = new GeneralPath();
		gp.moveTo(0, 50);
		float xInit = 0;
		float xEnd;
		float xMiddle;
		float yInit = 50;
		float h = 50;
		for(int i = 0; i < nbArches; i++) {
			xEnd = xInit + 50;
			xMiddle = (xInit + xEnd) / 2;
			gp.curveTo(
					xMiddle - (50f / 6), yInit - h, 
					xMiddle + (50f / 6), yInit - h, 
					xEnd, yInit);
			xInit += 50;
		}
		double[] coords = new double[6];
		for (PathIterator pi = gp.getPathIterator(null, 1); ! pi.isDone(); pi.next()) {
			pi.currentSegment(coords);
			gesture.add(new Point2D.Double(coords[0], coords[1]));
        }
		
		return gesture;
	}
	
	public static Vector<Point2D> getLineGesture() {
		Vector<Point2D> gesture = new Vector<Point2D>();
		gesture.add(new Point2D.Double(0, 50));
		gesture.add(new Point2D.Double(100, 50));
		return gesture;
	}
	
	public static Vector<Point2D> getOpenCircleGesture() {
		CPolyLine polyline = new CPolyLine(0, 50);
		polyline.arcTo(0, 2*Math.PI - Math.PI/8, 50, 50);
		Vector<Point2D> gesture = new Vector<Point2D>();
		GeneralPath gp = (GeneralPath)polyline.getShape();
		double[] coords = new double[6];
		for (PathIterator pi = gp.getPathIterator(null, 1); ! pi.isDone(); pi.next()) {
			pi.currentSegment(coords);
			gesture.add(new Point2D.Double(coords[0], coords[1]));
        }
		return gesture;
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
		gesture.add(new Point2D.Double(50, 100));
		gesture.add(new Point2D.Double(50, 0));
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
		d1c.addClass("V5", mirrorGesture(rotateGesture(getVGesture(), Math.PI), VERTICAL_MIRROR));
		
		d1c.addClass("Line1", getLineGesture());
		d1c.addClass("Line2", rotateGesture(getLineGesture(), Math.PI));
		d1c.addClass("Line3", rotateGesture(getLineGesture(), Math.PI/2));
		d1c.addClass("Line4", rotateGesture(getLineGesture(), 3*Math.PI/2));
		
		d1c.addClass("O1", getOpenCircleGesture());
		
		d1c.addClass("X1", getCrossGesture());
		d1c.addClass("X2", mirrorGesture(getCrossGesture(), VERTICAL_MIRROR));
		d1c.addClass("X3", mirrorGesture(getCrossGesture(), HORIZONTAL_MIRROR));
		
		d1c.addClass("Spiral1", getSpiralGesture(1.5, 10, 10));
		
		d1c.addClass("Helix1", getHelixGesture(2, 1.5, 50, 50, 50, 20));
		
		d1c.addClass("Wave1", getWaveGesture(1));
		d1c.addClass("Wave2", getWaveGesture(2));
		d1c.addClass("Wave3", getWaveGesture(3));
		d1c.addClass("Wave4", getWaveGesture(4));
		
		d1c.addClass("Arche1", getArchesGesture(1));
		d1c.addClass("Arche2", getArchesGesture(2));
		d1c.addClass("Arche3", getArchesGesture(3));
		d1c.addClass("Arche4", getArchesGesture(4));
		
		d1c.addClass("ZigZag1", getZigZagGesture(1));
		d1c.addClass("ZigZag2", getZigZagGesture(2));
		d1c.addClass("ZigZag3", getZigZagGesture(3));
		d1c.addClass("ZigZag4", getZigZagGesture(4));
		
		d1c.addClass("Angle1", getLGesture());
		
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

