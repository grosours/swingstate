package fr.lri.swingstates.sm.transitions;

import java.awt.geom.Point2D;
import java.util.EventObject;

import fr.lri.swingstates.events.VirtualPositionEvent;

/**
 * A transition triggered on a location.
 * OnPosition transitions allow developpers to retrieve the location where this transition has been triggered:
 *  <pre>
 * 	Transition tshape = new EventOnPosition (BUTTON1) {
 * 		public void action() {
 * 			System.out.println("The transition has been triggered at "+getPoint());
 * 		}
 * 	}
 * 	</pre>
 * 	
 * @author Caroline Appert
 */
public class EventOnPosition extends Event {
	
	/*
	 * The position at which this transition has occured (set dynamically)
	 */
	protected Point2D position = null;
	
	/**
	 * Builds a transition on a position with no modifier.
	 * @param keyEvent The string describing the events for which this transition must be triggered: "Down", "Move", "Drag", "Release", "Click"
	 * @param outState The name of the output state
	 */
	public EventOnPosition(String keyEvent, String outState) {
		super(keyEvent, outState);
	}
	
	/**
	 * Builds a transition on a position with no modifier that loops on the current state.
	 * @param keyEvent The string describing the events for which this transition must be triggered: "Down", "Move", "Drag", "Release", "Click"
	 */
	public EventOnPosition(String keyEvent) {
		super(keyEvent);
	}
	
	/**
	 * Builds a transition on a position with no modifier that can be triggered by any virtual events
	 * whose type is a subclass of <code>eventClass</code>.
	 * @param eventClass The class of events
	 * @param outState The name of the output state
	 */
	public EventOnPosition(Class eventClass, String outState) {
		super(eventClass, outState);
	}
	
	/**
	 * Builds a transition on a position with no modifier that can be triggered by any virtual events
	 * whose type is a subclass of <code>eventClass</code>.
	 * @param eventClass The class of events
	 */
	public EventOnPosition(Class eventClass) {
		super(eventClass);
	}
	
	/**
	 * Returns the location at which this transition has occured.
	 * @return the location at which the mouse event firing this transition has occured.
	 */
	public Point2D getPoint(){
		return position;
	}
	
	/**
	 * Stores the position at which this transition has occured.
	 * @param pt The position
	 */
	protected void setPoint(Point2D pt){
		position = pt;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		if(classEvent != null) return "EventOnPosition("+classEvent.getSimpleName()+".class)";
		else return "EventOnPosition("+event+")";
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean matches(EventObject eventObject) {
		if(classEvent != null) {
			if(!classEvent.isAssignableFrom(eventObject.getClass())) {
				return false;
			}
			triggeringEvent = eventObject;
		} else {
			if(!super.matches(eventObject)) return false;
		}
		if(eventObject instanceof VirtualPositionEvent) {
			setPoint(((VirtualPositionEvent)eventObject).getPoint());
			return true;
		}
		return false;
	}
}
