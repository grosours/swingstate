package fr.lri.swingstates.gestures.shapeMatching;

import java.awt.geom.Point2D;
import java.util.Vector;

public class ClassifiedAndResampled {

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
