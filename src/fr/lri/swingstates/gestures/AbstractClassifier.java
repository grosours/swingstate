/*  
 *   Authors: Caroline Appert (caroline.appert@lri.fr)
 *   Copyright (c) Universite Paris-Sud XI, 2007. All Rights Reserved
 *   Licensed under the GNU LGPL. For full terms see the file COPYING.
*/
package fr.lri.swingstates.gestures;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import fr.lri.swingstates.canvas.CPolyLine;

/**
 * The base class for a gesture classifier.
 * 
 * @author Caroline Appert
 */
public abstract class AbstractClassifier {

	protected ArrayList<GestureClass> classes = new ArrayList<GestureClass>();

	protected abstract Object read(DataInputStream in) throws IOException;

	protected void write(DataOutputStream out) throws IOException {
		out.writeInt(classes.size());
		for (int i = 0; i < classes.size(); i++)
			classes.get(i).write(out);
	}

	/**
	 * Removes a gesture example from this classifier.
	 * 
	 * @param gesture
	 *            the gesture to remove
	 */
	public void removeGesture(Gesture gesture) {
		for (Iterator<GestureClass> iterator = classes.iterator(); iterator.hasNext();) {
			iterator.next().removeExample(gesture);
		}
	}

	/**
	 * Looks for the class of gestures labeled by a given name.
	 * 
	 * @param className
	 *            The name of the class to look for.
	 * @return the class of gestures labeled by name if it exists, null if it
	 *         does not exist.
	 */
	public GestureClass findClass(String className) {
		ArrayList<GestureClass> c = classes;
		for (int i = 0; i < classes.size(); i++) {
			if (className.compareTo(classes.get(i).getName()) == 0)
				return c.get(i);
		}
		return null;
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
	public abstract CPolyLine getRepresentative(String className);

	/**
	 * Adds a class of gestures to this classifier.
	 * 
	 * @param className
	 *            The name of the class of gestures to add.
	 */
	public abstract void addClass(String className);

	/**
	 * Adds a gesture example to this classifier.
	 * 
	 * @param className
	 *            the gesture example's class
	 * @param example
	 *            the gesture example
	 */
	public void addExample(String className, Gesture example) {
		GestureClass gestureClass = findClass(className);
		gestureClass.addExample(example);
	}

	/**
	 * Removes a class of gestures from this classifier.
	 * 
	 * @param className
	 *            The name of the class of gestures to remove.
	 */
	public abstract void removeClass(String className);

	/**
	 * Renames a class of gestures.
	 * 
	 * @param previousClassName
	 *            The current name of this class of gestures
	 * @param newClassName
	 *            The new name of this class of gestures
	 */
	public void renameClass(String previousClassName, String newClassName) {
		findClass(previousClassName).setName(newClassName);
	}

	/**
	 * Recognizes a gesture.
	 * 
	 * @param g
	 *            The gesture to recognize
	 * @return The name of the class of gestures that best fit to g.
	 */
	public abstract String classify(Gesture g);

	/**
	 * Computes a sorted list of classes contained in this recognizer from the
	 * best match to the the worst match given a gesture.
	 * 
	 * @param g
	 *            The gesture
	 * @return a vector of scores for all the classes registered in this classifier 
	 * 			sorted from the best match (index 0) to the worst match (index n-1),
	 * 			with n the number of classes.
	 * 			A score is a couple (class_name, distance).
	 */
	public abstract Vector<Score> sortedClasses(Gesture g);

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
	 * @return the list of gesture classes contained in this classifier.
	 */
	public ArrayList<String> getClasses() {
		ArrayList<String> classesNames = new ArrayList<String>();
		for(Iterator<GestureClass> iterator = classes.iterator(); iterator.hasNext(); )
			classesNames.add(iterator.next().getName());
		return classesNames;
	}
	
	/**
	 * Resets this classifier (i.e. removes all the classes of gestures).
	 */
	public void reset() {
		classes.clear();
	}

}
