/*  
 *   Authors: Caroline Appert (caroline.appert@lri.fr)
 *   Copyright (c) Universite Paris-Sud XI, 2007. All Rights Reserved
 *   Licensed under the GNU LGPL. For full terms see the file COPYING.
*/
package fr.lri.swingstates.gestures;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import fr.lri.swingstates.canvas.CEllipse;
import fr.lri.swingstates.canvas.CExtensionalTag;
import fr.lri.swingstates.canvas.CIntentionalTag;
import fr.lri.swingstates.canvas.CPolyLine;
import fr.lri.swingstates.canvas.CRectangle;
import fr.lri.swingstates.canvas.CShape;
import fr.lri.swingstates.canvas.CStateMachine;
import fr.lri.swingstates.canvas.Canvas;
import fr.lri.swingstates.sm.State;
import fr.lri.swingstates.sm.Transition;
import fr.lri.swingstates.sm.transitions.KeyPress;
import fr.lri.swingstates.sm.transitions.Press;

/**
 * A widget displaying a collection of gestures (used in Training application).
 * 
 * @author Caroline Appert
 * 
 */
public class GestureExamples extends JScrollPane {

	private static int sizePreview = 50;

	private Canvas canvas;
	private JFrame topLevelContainer;
	private int lastX, lastY;
	private Training training;

	/**
	 * Tag assigned to every graphical gesture examples.
	 * 
	 * @author Caroline Appert
	 */
	private class GestureBoundingBox extends CExtensionalTag {
		Gesture containedGesture;

		public GestureBoundingBox(Gesture gesture) {
			super();
			this.containedGesture = gesture;
		}

		public Gesture getContainedGesture() {
			return containedGesture;
		}
	};

	/**
	 * Builds a widget for displaying a set of gestures.
	 * @param topLevelContainer The main frame in which this widget is packed.
	 * @param training The training application.
	 */
	public GestureExamples(JFrame topLevelContainer, Training training) {
		super();
		this.training = training;
		this.topLevelContainer = topLevelContainer;
		canvas = new Canvas(200, 100);
		setViewportView(canvas);
		lastX = 0;
		lastY = 0;

		new CStateMachine(canvas) {
			CShape selected = null;
			GestureBoundingBox tagSelected;
			public State start = new State() {
				Transition selectGesture = new PressOnTag(GestureBoundingBox.class, BUTTON1, ">> selection") {
					public void action() {
						tagSelected = (GestureBoundingBox) getTag();
						canvas.requestFocus();
						selected = getShape();
						selected.setOutlinePaint(Color.RED);
					}
				};
			};
			public State selection = new State() {
				Transition deleteGesture = new KeyPress(KeyEvent.VK_BACK_SPACE, ">> start") {
					public void action() {
						deleteGesture(tagSelected, selected);
					}
				};
				Transition selectAnotherGesture = new PressOnTag(GestureBoundingBox.class, BUTTON1) {
					public void action() {
						tagSelected = (GestureBoundingBox) getTag();
						selected.setOutlinePaint(Color.BLACK);
						canvas.requestFocus();
						selected = getShape();
						selected.setOutlinePaint(Color.RED);
					}
				};
				Transition deselect = new Press(BUTTON1, ">> start") {
					public void action() {
						selected.setOutlinePaint(Color.BLACK);
					}
				};
			};
		};

	}

	private void fillBlanks() {
		CIntentionalTag tag = canvas.getTag(GestureBoundingBox.class);
		tag.reset();
		CShape next;
		int x = 0;
		int y = 0;
		while (tag.hasNext()) {
			next = tag.nextShape();
			next.setReferencePoint(0.5, 0.5).translateTo(x + sizePreview / 2, y + sizePreview / 2);
			x += sizePreview;
			if ((x + sizePreview) > topLevelContainer.getWidth()) {
				x = 0;
				y += sizePreview;
				if ((y + sizePreview) > canvas.getPreferredSize().getHeight()) {
					canvas.setPreferredSize(new Dimension((int) canvas.getPreferredSize().getWidth(), y + sizePreview));
					canvas.revalidate();
				}
			}
		}
		lastX = x;
		lastY = y;
	}

	private void deleteGesture(GestureBoundingBox tagSelected, CShape bbGesture) {
		canvas.removeShapes(bbGesture.getHierarchy());
		fillBlanks();
		for (Iterator<AbstractClassifier> iterator = training.getClassifiers().iterator(); iterator.hasNext();)
			iterator.next().removeGesture(tagSelected.getContainedGesture());

		training.updateInfos(false);
	}

	/**
	 * Displays a polyline of a gesture example in a bounding box in a
	 * SwingStates <code>Canvas</code>. The gesture is displayed with a red
	 * circle at its start point.
	 * 
	 * @param canvas
	 *            The SwingStates <code>Canvas</code>.
	 * @param polyline
	 *            The polyline of gesture example.
	 * @param x
	 *            The x-coordinate of the bounding box upper left corner in
	 *            canvas coordinate system.
	 * @param y
	 *            The y-coordinate of the bounding box upper left corner in
	 *            canvas coordinate system.
	 * @param sizeBoundingBox
	 *            The size of the bounding box side of this gesture example.
	 * @param sizeSpan
	 *            The blank space between bounding box and this gesture.
	 * @param sizeStartPoint
	 *            The size of the red starting circle.
	 * 
	 * @return the polyline for this gesture example that has been added to the
	 *         canvas.
	 */
	public static CShape showPreview(Canvas canvas, CPolyLine polyline, int x, int y, int sizeBoundingBox, int sizeSpan, double sizeStartPoint) {
		double maxSide = Math.max(polyline.getHeight(), polyline.getWidth());
		double dscale = (sizeBoundingBox - 2 * sizeSpan) / maxSide;
		CRectangle gestureBB = canvas.newRectangle(x, y, sizeBoundingBox, sizeBoundingBox);
		CPolyLine exampleView = (CPolyLine) polyline.duplicate().setFilled(false);

		Canvas initialCanvas = exampleView.getCanvas();
		if (initialCanvas != null)
			initialCanvas.removeShape(exampleView);
		canvas.addShape(exampleView);
		exampleView.scaleBy(dscale).setReferencePoint(0.5, 0.5).translateBy(x + sizeBoundingBox / 2 - exampleView.getCenterX(), y + sizeBoundingBox / 2 - exampleView.getCenterY());

		gestureBB.setFillPaint(new Color(250, 240, 230));

		exampleView.fixReferenceShapeToCurrent().setPickable(false);
		gestureBB.fixReferenceShapeToCurrent();
		CEllipse startPoint = canvas.newEllipse(exampleView.getStartX() - sizeStartPoint / 2, exampleView.getStartY() - sizeStartPoint / 2, sizeStartPoint, sizeStartPoint);
		startPoint.setFillPaint(Color.RED).setOutlinePaint(Color.RED).setPickable(false);

		gestureBB.addChild(exampleView);
		exampleView.addChild(startPoint);

		return gestureBB;
	}

	/**
	 * Displays a polyline of a gesture example in a bounding box in a
	 * SwingStates <code>Canvas</code>. The gesture is displayed with a red
	 * circle at its start point and an orange arrow head at its end point.
	 * 
	 * @param canvas
	 *            The SwingStates <code>Canvas</code>.
	 * @param polyline
	 *            The polyline of gesture example.
	 * @param x
	 *            The x-coordinate of the bounding box upper left corner in
	 *            canvas coordinate system.
	 * @param y
	 *            The y-coordinate of the bounding box upper left corner in
	 *            canvas coordinate system.
	 * @param sizeBoundingBox
	 *            The size of the bounding box side of this gesture example.
	 * @param sizeSpan
	 *            The blank space between bounding box and this gesture.
	 * @param sizeStartPoint
	 *            The size of the red starting circle.
	 * 
	 * @return the polyline for this gesture example that has been added to the
	 *         canvas.
	 */
	public static CShape showArrowPreview(Canvas canvas, CPolyLine polyline, int x, int y, int sizeBoundingBox, int sizeSpan, double sizeStartPoint) {
		double maxSide = Math.max(polyline.getHeight(), polyline.getWidth());
		double dscale = (sizeBoundingBox - 2 * sizeSpan) / maxSide;
		CRectangle gestureBB = canvas.newRectangle(x, y, sizeBoundingBox, sizeBoundingBox);
		CPolyLine exampleView = (CPolyLine) polyline.duplicate().setFilled(false);

		Canvas initialCanvas = exampleView.getCanvas();
		if (initialCanvas != null)
			initialCanvas.removeShape(exampleView);
		canvas.addShape(exampleView);
		exampleView.scaleBy(dscale).setReferencePoint(0.5, 0.5).translateBy(x + sizeBoundingBox / 2 - exampleView.getCenterX(), y + sizeBoundingBox / 2 - exampleView.getCenterY());

		gestureBB.setFillPaint(new Color(250, 240, 230));

		exampleView.fixReferenceShapeToCurrent().setPickable(false);

		gestureBB.fixReferenceShapeToCurrent().setOutlined(false);
		CEllipse startPoint = canvas.newEllipse(exampleView.getStartX() - sizeStartPoint / 2, exampleView.getStartY() - sizeStartPoint / 2, sizeStartPoint, sizeStartPoint);
		startPoint.setFillPaint(Color.ORANGE).setOutlinePaint(Color.ORANGE).setPickable(false);

		CPolyLine arrow = exampleView.getArrow(Math.PI / 4, 4);
		arrow.setOutlinePaint(Color.RED);
		canvas.addShape(arrow);
		arrow.setParent(exampleView);

		gestureBB.addChild(exampleView);
		exampleView.addChild(startPoint);

		return gestureBB;
	}

	/**
	 * Adds a gesture to this GestureExamples widget.
	 * 
	 * @param example
	 *            The gesture to add.
	 */
	public void addExample(Gesture example) {
		CPolyLine graphicalExample = example.asPolyLine();
		double maxSide = Math.max(graphicalExample.getHeight(), graphicalExample.getWidth());
		double dscale = 40 / maxSide;
		if ((lastX + sizePreview) > topLevelContainer.getWidth()) {
			lastX = 0;
			lastY += sizePreview;
			if ((lastY + sizePreview) > canvas.getPreferredSize().getHeight()) {
				canvas.setPreferredSize(new Dimension((int) canvas.getPreferredSize().getWidth(), lastY + sizePreview));
				canvas.revalidate();
			}
		}
		CRectangle gestureBB = canvas.newRectangle(lastX + 1, lastY + 1, sizePreview - 2, sizePreview - 2);
		graphicalExample.setFilled(false);
		canvas.addShape(graphicalExample);

		gestureBB.addTag(new GestureBoundingBox(example));

		double sizeStartPoint = 5 / dscale;
		CEllipse startPoint = canvas.newEllipse(graphicalExample.getStartX() - sizeStartPoint / 2, graphicalExample.getStartY() - sizeStartPoint / 2, sizeStartPoint, sizeStartPoint);
		startPoint.setFillPaint(Color.RED).setOutlinePaint(Color.RED).setPickable(false);
		gestureBB.setFillPaint(new Color(250, 240, 230));
		gestureBB.addChild(graphicalExample);
		graphicalExample.addChild(startPoint);
		graphicalExample.translateBy(lastX + sizePreview / 2 - graphicalExample.getCenterX(), lastY + sizePreview / 2 - graphicalExample.getCenterY()).scaleBy(dscale).setPickable(false);

		lastX += sizePreview;
	}

}
