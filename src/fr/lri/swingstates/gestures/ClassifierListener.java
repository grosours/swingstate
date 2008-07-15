package fr.lri.swingstates.gestures;

import java.awt.geom.Point2D;
import java.util.EventListener;
import java.util.Vector;

public interface ClassifierListener extends EventListener {

	void classAdded(String className);
	void classRemoved(String className);
	void exampleAdded(String className, Gesture example);
	void exampleRemoved(String className, Gesture example);
	void templateSet(String className, Vector<Point2D> template);
}
