/*
 * Authors: Caroline Appert (caroline.appert@lri.fr) Copyright (c) Universite
 * Paris-Sud XI, 2007. All Rights Reserved Licensed under the GNU LGPL. For full
 * terms see the file COPYING.
 */
package fr.lri.swingstates.gestures.rubine;

/*******************************************************************************
 * The algorithm and original C code are: (C) Copyright, 1990 by Dean Rubine,
 * Carnegie Mellon University Permission to use this code for noncommercial
 * purposes is hereby granted. Permission to copy and distribute this code is
 * hereby granted provided this copyright notice is retained. All other rights
 * reserved.
 ******************************************************************************/

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Vector;

import fr.lri.swingstates.gestures.Gesture;
import fr.lri.swingstates.gestures.GestureClass;

/**
 * A class of gestures for Rubine recognizer.
 * 
 * @author Caroline Appert
 * 
 */
public class RubineGestureClass extends GestureClass {

	static double DIST_SQ_THRESHOLD = 3 * 3;
	static double SE_TH_ROLLOFF = 4 * 4;

	static final double EPSILON = 1.0e-4;

	static int PF_INIT_COS = 0; /* initial angle (cos) */
	static int PF_INIT_SIN = 1; /* initial angle (sin) */
	static int PF_BB_LEN = 2; /* length of bounding box diagonal */
	static int PF_BB_TH = 3; /* angle of bounding box diagonal */
	static int PF_SE_LEN = 4; /* length between start and end points */
	static int PF_SE_COS = 5; /* cos of angle between start and end points */
	static int PF_SE_SIN = 6; /* sin of angle between start and end points */
	static int PF_LEN = 7; /* arc length of path */
	static int PF_TH = 8; /* total angle traversed */
	static int PF_ATH = 9; /* sum of abs vals of angles traversed */
	static int PF_SQTH = 10; /* sum of squares of angles traversed */
	static int PF_DUR = 11; /* duration of path */

	static final int PF_MAXV = 12; /* maximum speed */
	static final int NFEATURES = 13;

	private Vector<Double> average;
	private Matrix sumCov;

	RubineGestureClass() {
		super();
		average = new Vector<Double>(NFEATURES);
		for (int i = 0; i < NFEATURES; i++)
			average.add(0.0);
		sumCov = new Matrix(NFEATURES, NFEATURES, true);
	}

	RubineGestureClass(String n) {
		this();
		name = n;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean removeExample(Gesture gesture) {
		if (!gestures.contains(gesture))
			return false;

		int i, j;
		int nExamples = gestures.size();

		double[] nfv = new double[NFEATURES];
		Vector<Double> fv = compute(gesture);

		double nm1on = ((double) nExamples - 1) / nExamples;
		double recipn = 1.0 / nExamples;

		/* incrementally update mean vector */
		for (i = 0; i < NFEATURES; i++)
			average.set(i, (average.get(i) - recipn * fv.get(i)) / nm1on);

		/* incrementally update covariance matrix */
		for (i = 0; i < NFEATURES; i++)
			nfv[i] = fv.get(i) - average.get(i);

		/* only upper triangular part computed */
		for (i = 0; i < NFEATURES; i++)
			for (j = i; j < NFEATURES; j++)
				sumCov.items[i][j] -= nm1on * nfv[i] * nfv[j];

		return super.removeExample(gesture);
	}

	static Vector<Double> compute(Gesture g) {

		Vector<Double> compiledData = new Vector<Double>();
		for (int i = 0; i < NFEATURES; i++)
			compiledData.add(0.0);

		if (g.getPoints().size() < 3)
			return compiledData; // a feature vector of all zeros, at least 3
		// points are required to compute initial
		// sin and cos

		// initial cos and sin
		/* compute initial theta */
		Point2D thirdPoint = g.getPoints().get(2);
		/* find angle w.r.t. positive x axis e.g. (1,0) */
		double dx = thirdPoint.getX() - g.getStart().getX();
		double dy = thirdPoint.getY() - g.getStart().getY();
		double dist2 = dx * dx + dy * dy;
		if (dist2 > DIST_SQ_THRESHOLD) {
			double d = Math.sqrt(dist2);
			compiledData.set(PF_INIT_COS, dx / d);
			compiledData.set(PF_INIT_SIN, dy / d);
		}

		// compute features related to bounding box (length and orientation)
		double t1 = g.getMax().getX() - g.getMin().getX();
		double t2 = g.getMax().getY() - g.getMin().getY();
		double bblen = Math.sqrt(t1 * t1 + t2 * t2);
		compiledData.set(PF_BB_LEN, bblen);
		if (bblen * bblen > DIST_SQ_THRESHOLD) {
			double tmp = Math.atan2(t2, t1);
			compiledData.set(PF_BB_TH, tmp);
		}

		t1 = g.getEnd().getX() - g.getStart().getX();
		t2 = g.getEnd().getY() - g.getStart().getY();
		double selen = Math.sqrt(t1 * t1 + t2 * t2);
		compiledData.set(PF_SE_LEN, selen);
		double factor = selen * selen / SE_TH_ROLLOFF;
		if (factor > 1.0)
			factor = 1.0;
		factor = selen > EPSILON ? factor / selen : 0.0;

		compiledData.set(PF_SE_COS, (g.getEnd().getX() - g.getStart().getX()) * factor);
		compiledData.set(PF_SE_SIN, (g.getEnd().getY() - g.getStart().getY()) * factor);

		Point2D next = null;
		Point2D previous = null;
		double magsq = 0;
		int i = 1;
		double length = 0;
		double rotation = 0;
		double sumAbsAngles = 0;
		double sharpness = 0;
		Point2D del = null;
		Point2D delta = new Point2D.Double();
		double maxSpeed = Double.MIN_VALUE;

		for (Iterator<Point2D> iterator = g.getPoints().iterator(); iterator.hasNext();) {
			next = iterator.next();
			if (previous != null) {
				del = new Point2D.Double(previous.getX() - next.getX(), previous.getY() - next.getY()); // delta
				magsq = del.getX() * del.getX() + del.getY() * del.getY();
				if (magsq <= DIST_SQ_THRESHOLD) {
					continue; /* ignore this point */
				}
			}

			double dist = Math.sqrt(magsq);
			length += dist;

			if (i >= 3) {
				double theta1 = del.getX() * delta.getY() - delta.getX() * del.getY();
				double theta2 = del.getX() * delta.getX() + del.getY() * delta.getY();
				double th = Math.atan2(theta1, theta2);
				double absth = Math.abs(th);
				rotation += th;
				sumAbsAngles += absth;
				sharpness += th * th;

				int indexPrevious = g.getPoints().indexOf(previous);
				double lasttime = g.getPointTimes().get(indexPrevious);
				double v = dist / (g.getPointTimes().get(g.getPointTimes().size() - 1) - lasttime);
				if (g.getPointTimes().get(g.getPointTimes().size() - 1) > lasttime && v > maxSpeed)
					maxSpeed = v;
			}

			if (previous != null) {
				delta.setLocation(del.getX(), del.getY());
			}
			previous = next;
			i++;
		}

		compiledData.set(PF_LEN, length);
		compiledData.set(PF_TH, rotation);
		compiledData.set(PF_ATH, sumAbsAngles);
		compiledData.set(PF_SQTH, sharpness);

		compiledData.set(PF_DUR, g.getDuration() * .01); // sensitive to
		// a 1/10th of
		// second

		compiledData.set(PF_MAXV, maxSpeed * 10000);
		return compiledData;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addExample(Gesture gesture) {
		super.addExample(gesture);
		int i, j;
		double[] nfv = new double[NFEATURES];
		Vector<Double> fv = compute(gesture);
		int nExamples = gestures.size();

		double nm1on = ((double) nExamples - 1) / nExamples;
		double recipn = 1.0 / nExamples;

		/* incrementally update covariance matrix */
		for (i = 0; i < NFEATURES; i++)
			nfv[i] = fv.get(i) - average.get(i);
		/* only upper triangular part computed */
		for (i = 0; i < NFEATURES; i++)
			for (j = i; j < NFEATURES; j++)
				sumCov.items[i][j] += nm1on * nfv[i] * nfv[j];
		/* incrementally update mean vector */
		for (i = 0; i < NFEATURES; i++)
			average.set(i, nm1on * average.get(i) + recipn * fv.get(i));

	}

	/**
	 * @return the average vector features of this gesture class.
	 */
	public Vector<Double> getAverage() {
		return average;
	}

	/**
	 * @return the covariance matrix of this gesture class.
	 */
	public Matrix getSumCov() {
		return sumCov;
	}

}
