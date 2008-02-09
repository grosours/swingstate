/*  
 *   Authors: Caroline Appert (caroline.appert@lri.fr)
 *   Copyright (c) Universite Paris-Sud XI, 2007. All Rights Reserved
 *   Licensed under the GNU LGPL. For full terms see the file COPYING.
*/
package fr.lri.swingstates.sm;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.util.EventObject;

import fr.lri.swingstates.events.Utils;
import fr.lri.swingstates.events.VirtualPositionEvent;
import fr.lri.swingstates.sm.StateMachine.State.Event;
import fr.lri.swingstates.sm.StateMachine.State.Transition;


/**
 * A state machine to handle basic input events (mouse events and keyboard events).
 * 
 * <p> The complete list of event types of a BasicInputStateMachine is:
 * <ul>
 * <li> <code>Press</code>: pressing a mouse button anywhere;
 * <li> <code>Release</code>: releasing a mouse button anywhere;
 * <li> <code>Click</code>: clicking (pressing and releasing in quick succession) a mouse button anywhere;
 * <li> <code>Move</code>: moving the mouse with no button pressed anywhere;
 * <li> <code>Drag</code>: moving the mouse with a button pressed anywhere;
 * <li> <code>KeyPress, KeyRelease, KeyType</code>: typing a key (pressing, releasing, press then release in quick succession);
 * <li> <code>TimeOut</code>: delay specified by armTimer expired.
 * <li> <code>Event</code>: high-level events
 * </ul>
 * An event type in a state machine is the name of the class of a transition:
 * <pre>
 * 	// declare a transition to state s2 when pressing the left mouse button..
 * 	Transition t = new Press (BUTTON1) {
 * 		...
 * 	}
 * </pre>
 * </p>
 * <p>
 * A state machine implements <code>MouseListener</code>, <code>MouseWheelListener</code>, <code>MouseMotionListener</code> and <code>KeyListener</code>, 
 * it can be attached to any awt component to control it:
 * <pre>
 * 	JPanel panel = new JPanel();
 * 	BasicInputStateMachine sm = new BasicInputStateMachine() {
 * 		Point2D pt;
 *		public State start = new State() {
 *			Transition press = new Press(BUTTON1, ">> pressed") {
 *				public void action() {
 *					pt = getPoint();
 *					armTimer(500, false);
 *				}
 *			};
 *		};
 *			
 *		public State pressed = new State() {
 *			Transition timeOut = new TimeOut(">> start") {
 *				public void action() {
 *					System.out.println("long press at: "+pt.getX()+", "+pt.getY());
 *				}
 *			};
 *			Transition release = new Release(BUTTON1, ">> start") { };
 *		};
 *	};
 *		
 *	sm.addAsListenerOf(panel);
 * </pre>
 * </p>
 * @see fr.lri.swingstates.sm.StateMachine
 * @author Caroline Appert
 *
 */
public class BasicInputStateMachine extends StateMachine implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * Specifies that the mouse must have all buttons released.
	 */
	public static final int NOBUTTON = MouseEvent.NOBUTTON;
	/**
	 * Specifies that the mouse must have any or no button pressed.
	 */
	public static final int ANYBUTTON = -1;
	/**
	 * Specifies that the mouse must have the button 1 pressed.
	 */
	public static final int BUTTON1 = MouseEvent.BUTTON1;
	/**
	 * Specify that the mouse must have the button 2 pressed.
	 */
	public static final int BUTTON2 = MouseEvent.BUTTON2;
	/**
	 * Specifies that the mouse must have the button 3 pressed.
	 */
	public static final int BUTTON3 = MouseEvent.BUTTON3;
	
	/**
	 * Specifies that no keyboard modifiers must be pressed.
	 */
	public static final int NOMODIFIER = 0;
	/**
	 * Specifies that the SHIFT keyboard modifier must be pressed.
	 */
	public static final int SHIFT = 1;
	/**
	 * Specifies that the CONTROL keyboard modifier must be pressed.
	 */
	public static final int CONTROL = 2;
	/**
	 * Specifies that the ALT keyboard modifier must be pressed.
	 */
	public static final int ALT = 3;
	/**
	 * Specifies that the CONTROL and SHIFT keyboard modifiers must be pressed.
	 */
	public static final int CONTROL_SHIFT = 4;
	/**
	 * Specifies that the ALT and SHIFT keyboard modifiers must be pressed.
	 */
	public static final int ALT_SHIFT = 5;
	/**
	 * Specifies that the ALT and CONTROL keyboard modifiers must be pressed.
	 */
	public static final int ALT_CONTROL = 6;
	/**
	 * Specifies that the ALT, CONTROL and SHIFT keyboard modifiers must be pressed.
	 */
	public static final int ALT_CONTROL_SHIFT = 7;
	/**
	 * Specifies that any keyboard modifiers can be pressed.
	 */
	public static final int ANYMODIFIER = 8;

	
	/**
	 * The key string of events that triggered <code>AnimationStopped</code> transitions.
	 */
	public static String ANIMATION_STOPPED   = "AnimationStopped";
	/**
	 * The key string of events that triggered <code>AnimationStarted</code> transitions.
	 */
	public static String ANIMATION_STARTED   = "AnimationStarted";
	/**
	 * The key string of events that triggered <code>AnimationSuspended</code> transitions.
	 */
	public static String ANIMATION_SUSPENDED = "AnimationSuspended";
	/**
	 * The key string of events that triggered <code>AnimationResumed</code> transitions.
	 */
	public static String ANIMATION_RESUMED   = "AnimationResumed";
	
	/**
	 * Builds a state machine that handles basic input events. 
	 */
	public BasicInputStateMachine() {
		super();
	}
	
	/**
	 * Installs this <code>BasicInputStateMachine</code> 
	 * as a listener of a given graphical component.
	 * @param c The graphical component
	 * @return This <code>BasicInputStateMachine</code>.
	 */
	public BasicInputStateMachine addAsListenerOf(Component c) {
		c.addMouseListener(this);
		c.addMouseMotionListener(this);
		c.addMouseWheelListener(this);
		c.addKeyListener(this);
		return this;
	}
	
	/**
	 * Uninstalls this <code>BasicInputStateMachine</code> 
	 * as a listener of a given graphical component.
	 * @param c The graphical component
	 * @return This <code>BasicInputStateMachine</code>.
	 */
	public BasicInputStateMachine removeAsListenerOf(Component c) {
		c.removeMouseListener(this);
		c.removeMouseMotionListener(this);
		c.removeMouseWheelListener(this);
		c.removeKeyListener(this);
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void mouseClicked(MouseEvent arg0) {
		processEvent(arg0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void mouseReleased(MouseEvent arg0) { 
		processEvent(arg0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void mouseEntered(MouseEvent arg0) {
		processEvent(arg0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void mouseExited(MouseEvent arg0) {
		processEvent(arg0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void mouseDragged(MouseEvent arg0) {
		processEvent(arg0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void mouseMoved(MouseEvent arg0) {
		processEvent(arg0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		processEvent(arg0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void mousePressed(MouseEvent arg0) {
		processEvent(arg0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void keyTyped(KeyEvent arg0) { 
		processEvent(arg0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void keyPressed(KeyEvent arg0) {
		processEvent(arg0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void keyReleased(KeyEvent arg0) {
		processEvent(arg0);	
	}
	
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
		Point2D position = null;
		
		/**
		 * Builds a transition on a position with no modifier.
		 * @param keyEvent The string describing the events for which this transition must be triggered: "Down", "Move", "Drag", "Release", "Click"
		 * @param outState The name of the output state
		 */
		public EventOnPosition(String keyEvent, String outState) {
			stateInBuilt.super(keyEvent, outState);
		}
		
		/**
		 * Builds a transition on a position with no modifier that loops on the current state.
		 * @param keyEvent The string describing the events for which this transition must be triggered: "Down", "Move", "Drag", "Release", "Click"
		 */
		public EventOnPosition(String keyEvent) {
			stateInBuilt.super(keyEvent);
		}
		
		/**
		 * Builds a transition on a position with no modifier that can be triggered by any virtual events
		 * whose type is a subclass of <code>eventClass</code>.
		 * @param eventClass The class of events
		 * @param outState The name of the output state
		 */
		public EventOnPosition(Class eventClass, String outState) {
			stateInBuilt.super(eventClass, outState);
		}
		
		/**
		 * Builds a transition on a position with no modifier that can be triggered by any virtual events
		 * whose type is a subclass of <code>eventClass</code>.
		 * @param eventClass The class of events
		 */
		public EventOnPosition(Class eventClass) {
			stateInBuilt.super(eventClass);
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
	
	/**
	 * A transition triggered by a mouse event on a location.
	 * The transition is specified by a button and modifiers.
	 * The position of the mouse when the transition fired can be retrieved.
	 * @author Caroline Appert
	 */
	public abstract class MouseOnPosition extends EventOnPosition {
		
		/**
		 * The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3.
		 */
		int button = BUTTON1;
		
		/**
		 * The modifier : NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT.
		 */
		int modifier = ANYMODIFIER;
		
		/**
		 * Builds a mouse transition with any modifier.
		 * @param outState The name of the output state
		 */
		public MouseOnPosition(String outState){
			this(ANYBUTTON, outState);
		}
		
		/**
		 * Builds a mouse transition with any modifier.
		 */
		public MouseOnPosition(){
			this(ANYBUTTON);
		}
		
		/**
		 * Builds a mouse transition.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param outState The name of the output state
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 */
		public MouseOnPosition(int b, int m, String outState){
			super((String)null, outState);
			modifier = m;
			button = b;
		}
		
		
		/**
		 * Builds a mouse transition with any modifier.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param outState The name of the output state
		 */
		public MouseOnPosition(int b, String outState){
			super((String)null, outState);
			button = b;
		}
		
		/**
		 * Builds a mouse transition that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 */
		public MouseOnPosition(int b, int m){
			super((String)null);
			modifier = m;
			button = b;
		}
		
		/**
		 * Builds a mouse transition with any modifier that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 */
		public MouseOnPosition(int b){
			super((String)null);
			button = b;
		}
		
		
		/**
		 * Returns the button of the mouse event that fires this transition.
		 * @return the button of the mouse event that fires this transition (NOBUTTON, BUTTON1, BUTTON2 or BUTTON3).
		 */
		public int getButton(){
			return button;
		}
		
		/**
		 * Returns the modifier of the mouse event that fires this transition.
		 * @return the modifier : NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 */
		public int getModifier() {
			return modifier;
		}

		/**
		 * {@inheritDoc}
		 */
		public String toString() {
			return getClass().getSuperclass().getSimpleName()+"("+Utils.getButtonAsText(button)+","+Utils.getModifiersAsText(modifier)+")";
		}
		
		/**
		 * @return the awt mouse event that fires this transition.
		 */
		public InputEvent getInputEvent() {
			return (InputEvent)triggeringEvent;
		}
		
		protected boolean matches(EventObject eventObject, int typeEvent) {
			if(!(eventObject instanceof MouseEvent)) return false;
			MouseEvent me = (MouseEvent)eventObject;
			triggeringEvent = me;
			setPoint(me.getPoint());
			return (me.getID() == typeEvent)
				&& (modifier == Utils.modifiers(me) || modifier == ANYMODIFIER)
				&& (button == Utils.button(me) || button == ANYBUTTON);
		}
		
	}
	

	
	/**
	 * A transition triggered by a mouse pressed event.
	 * @author Caroline Appert
	 */
	public class Press extends MouseOnPosition {
		
		/**
		 * Builds a transition triggered by a mouse pressed event with any modifier and any button.
		 * @param outState The name of the output state
		 */
		public Press(String outState){
			super(ANYBUTTON, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse pressed event with any modifier and any button that loops on the current state.
		 */
		public Press(){
			super(ANYBUTTON);
		}
		
		/**
		 * Builds a transition triggered by a mouse pressed event with any modifier that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 */
		public Press(int b) {
			super(b);
		}
		
		/**
		 * Builds a transition triggered by a mouse pressed event that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 */
		public Press(int b, int m) {
			super(b, m);
		}
		
		/**
		 * Builds a transition triggered by a mouse pressed event with any modifier.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param outState The name of the output state
		 */
		public Press(int b, String outState) {
			super(b, ANYMODIFIER, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse pressed event.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 * @param outState The name of the output state
		 */
		public Press(int b, int m, String outState) {
			super(b, m, outState);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean matches(EventObject eventObject) {
			return matches(eventObject, MouseEvent.MOUSE_PRESSED);
		}
	}
	
	/**
	 * A transition triggered by a mouse released event.
	 * @author Caroline Appert
	 */
	public class Release extends MouseOnPosition {
		
		/**
		 * Builds a transition triggered by a mouse released event with any modifier and any button.
		 * @param outState The name of the output state
		 */
		public Release(String outState){
			super(ANYBUTTON, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse released event with any modifier and any button that loops on the current state.
		 */
		public Release(){
			super(ANYBUTTON);
		}
		
		/**
		 * Builds a transition triggered by a mouse released event with any modifier that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 */
		public Release(int b) {
			super(b);
		}
		
		/**
		 * Builds a transition triggered by a mouse released event that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 */
		public Release(int b, int m) {
			super(b, m);
		}
		
		/**
		 * Builds a transition triggered by a mouse released event with any modifier .
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param outState The name of the output state
		 */
		public Release(int b, String outState) {
			super(b, ANYMODIFIER, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse released event.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 * @param outState The name of the output state
		 */
		public Release(int b, int m, String outState) {
			super(b, m, outState);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean matches(EventObject eventObject) {
			return matches(eventObject, MouseEvent.MOUSE_RELEASED);
		}
		
	}
	
	/**
	 * A transition triggered by a mouse clicked event.
	 * A click is defined as a quick succession of mouse press and mouse release, without significant motion in between.
	 * Note that the mouse press and mouse release events are always sent, even when a mouse click event is sent.
	 * @author Caroline Appert
	 */
	public class Click extends MouseOnPosition {
		
		/**
		 * Builds a transition triggered by a mouse clicked event with any modifier and any button.
		 * @param outState The name of the output state
		 */
		public Click(String outState){
			super(ANYBUTTON, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse clicked event with any modifier and any button that loops on the current state.
		 */
		public Click(){
			super(ANYBUTTON);
		}
		
		/**
		 * Builds a transition triggered by a mouse clicked event with any modifier that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 */
		public Click(int b) {
			super(b);
		}
		
		/**
		 * Builds a transition triggered by a mouse clicked event that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 */
		public Click(int b, int m) {
			super(b, m);
		}
		
		/**
		 * Builds a transition with any modifier triggered by a mouse clicked event.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param outState The name of the output state
		 */
		public Click(int b, String outState) {
			super(b, ANYMODIFIER, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse clicked event.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 * @param outState The name of the output state
		 */
		public Click(int b, int m, String outState) {
			super(b, m, outState);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean matches(EventObject eventObject) {
			return matches(eventObject, MouseEvent.MOUSE_CLICKED);
		}
	}
	
	/**
	 * A transition triggered by a mouse moved event with a button pressed.
	 * @author Caroline Appert
	 */
	public class Drag extends MouseOnPosition {
		
		/**
		 * Builds a transition triggered by a mouse dragged event with any modifier and any button.
		 * @param outState The name of the output state
		 */
		public Drag(String outState){
			super(ANYBUTTON, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse dragged event with any modifier and any button that loops on the current state.
		 */
		public Drag(){
			super(ANYBUTTON);
		}
		
		/**
		 * Builds a transition triggered by a mouse dragged event with any modifier that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 */
		public Drag(int b) {
			super(b);
		}
		
		/**
		 * Builds a transition triggered by a mouse dragged event that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 */
		public Drag(int b, int m) {
			super(b, m);
		}
		
		/**
		 * Builds a transition triggered by a mouse dragged event with any modifier.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param outState The name of the output state
		 */
		public Drag(int b, String outState) {
			super(b, ANYMODIFIER, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse dragged event.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 * @param outState The name of the output state
		 */
		public Drag(int b, int m, String outState) {
			super(b, m, outState);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean matches(EventObject eventObject) {
			return matches(eventObject, MouseEvent.MOUSE_DRAGGED);
		}
	}
	
	/**
	 * A transition triggered when the mouse cursor enters a graphical component.
	 * @author Caroline Appert
	 */
	public class Enter extends MouseOnPosition {
		
		/**
		 * Builds a transition triggered by a mouse entered event with any modifier and any button.
		 * @param outState The name of the output state
		 */
		public Enter(String outState){
			super(ANYBUTTON, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse entered event with any modifier and any button that loops on the current state.
		 */
		public Enter(){
			super(ANYBUTTON);
		}
		
		/**
		 * Builds a transition triggered by a mouse entered event with any modifier that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 */
		public Enter(int b) {
			super(b);
		}
		
		/**
		 * Builds a transition triggered by a mouse entered event that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 */
		public Enter(int b, int m) {
			super(b, m);
		}
		
		/**
		 * Builds a transition triggered by a mouse entered event with any modifier.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param outState The name of the output state
		 */
		public Enter(int b, String outState) {
			super(b, ANYMODIFIER, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse entered event.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 * @param outState The name of the output state
		 */
		public Enter(int b, int m, String outState) {
			super(b, m, outState);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean matches(EventObject eventObject) {
			return matches(eventObject, MouseEvent.MOUSE_ENTERED);
		}
	}
	
	/**
	 * A transition triggered when the mouse cursor exited a graphical component.
	 * @author Caroline Appert
	 */
	public class Leave extends MouseOnPosition {
		
		/**
		 * Builds a transition triggered by a mouse exited event with any modifier and any button.
		 * @param outState The name of the output state
		 */
		public Leave(String outState){
			super(ANYBUTTON, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse exited event with any modifier and any button that loops on the current state.
		 */
		public Leave(){
			super(ANYBUTTON);
		}
		
		/**
		 * Builds a transition triggered by a mouse exited event with any modifier that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 */
		public Leave(int b) {
			super(b);
		}
		
		/**
		 * Builds a transition triggered by a mouse exited event that loops on the current state.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 */
		public Leave(int b, int m) {
			super(b, m);
		}
		
		/**
		 * Builds a transition triggered by a mouse exited event with any modifier.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param outState The name of the output state
		 */
		public Leave(int b, String outState) {
			super(b, ANYMODIFIER, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse exited event.
		 * @param b The button of the mouse event: NOBUTTON, BUTTON1, BUTTON2 or BUTTON3
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 * @param outState The name of the output state
		 */
		public Leave(int b, int m, String outState) {
			super(b, m, outState);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean matches(EventObject eventObject) {
			return matches(eventObject, MouseEvent.MOUSE_EXITED);
		}
	}
	
	/**
	 * A transition triggered by a mouse moved event with no button pressed.
	 * @author Caroline Appert
	 */
	public class Move extends MouseOnPosition {
		
		/**
		 * Builds a transition triggered by a mouse moved event with any modifier that loops on the current state.
		 */
		public Move() {
			super(NOBUTTON);
		}
		
		/**
		 * Builds a transition triggered by a mouse moved event that loops on the current state.
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 */
		public Move(int m) {
			super(NOBUTTON, m);
		}
		
		/**
		 * Builds a transition triggered by a mouse moved event with any modifier.
		 * @param outState The name of the output state
		 */
		public Move(String outState) {
			super(NOBUTTON, ANYMODIFIER, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse moved event.
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 * @param outState The name of the output state
		 */
		public Move(int m, String outState) {
			super(NOBUTTON, m, outState);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean matches(EventObject eventObject) {
			return matches(eventObject, MouseEvent.MOUSE_MOVED);
		}
	}
	
	/**
	 * A transition triggered by a mouse wheel event with no button pressed.
	 * @author Caroline Appert
	 */
	public class Wheel extends MouseOnPosition {
		
		/**
		 * Builds a transition triggered by a mouse wheel event with any modifier that loops on the current state.
		 */
		public Wheel() {
			super(NOBUTTON);
		}
		
		/**
		 * Builds a transition triggered by a mouse wheel event that loops on the current state.
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 */
		public Wheel(int m) {
			super(NOBUTTON, m);
		}
		
		/**
		 * Builds a transition triggered by a mouse wheel event with any modifier.
		 * @param outState The name of the output state
		 */
		public Wheel(String outState) {
			super(NOBUTTON, ANYMODIFIER, outState);
		}
		
		/**
		 * Builds a transition triggered by a mouse wheel event.
		 * @param m The modifier: NOMODIFIER, CONTROL, ALT, SHIFT, ALT_CONTROL, CONTROL_SHIFT, ALT_SHIFT or ALT_CONTROL_SHIFT
		 * @param outState The name of the output state
		 */
		public Wheel(int m, String outState) {
			super(NOBUTTON, m, outState);
		}
		
		/**
		 * @return the number of units that should be scrolled in response to this event.
		 * @see java.awt.event.MouseWheelEvent#getScrollAmount()
		 */
		public int getScrollAmount() {
			return ((MouseWheelEvent)triggeringEvent).getWheelRotation();
		}
		
		/**
		 * @return the type of scrolling that should take place in response to this event.
		 * @see java.awt.event.MouseWheelEvent#getScrollType()
		 */
		public int getScrollType() {
			return ((MouseWheelEvent)triggeringEvent).getWheelRotation();
		}
		
		/**
		 * @return This is a convenience method to aid in the implementation of the common-case MouseWheelListener 
		 * - to scroll a ScrollPane or JScrollPane by an amount which conforms to the platform settings.
		 * @see java.awt.event.MouseWheelEvent#getUnitsToScroll()
		 */
		public int getUnitsToScroll() {
			return ((MouseWheelEvent)triggeringEvent).getWheelRotation();
		}
		
		/**
		 * @return the number of "clicks" the mouse wheel was rotated.
		 * @see java.awt.event.MouseWheelEvent#getWheelRotation()
		 */
		public int getWheelRotation() {
			return ((MouseWheelEvent)triggeringEvent).getWheelRotation();
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean matches(EventObject eventObject) {
			return matches(eventObject, MouseEvent.MOUSE_WHEEL);
		}
	}
	
	/**
	 * A transition triggered by a key event.
	 * Keys can be specified either by the ASCII character they represent or by their keycode.
	 * @author Caroline Appert
	 */
	public abstract class KeyTransition extends Transition {
		
		/**
		 * True if this transition must be initiated by any key event.
		 */
		boolean generic = false;
		
		/**
		 * The char corresponding to the key event.
		 */
		char charCode;
		
		/**
		 * The ASCII key code of the key event. 
		 */
		int keyCode;
		
		boolean key = true; 
		
		/**
		 * Builds a transition triggered by a key event with any modifier.
		 * @param outState The name of the output state
		 */
		public KeyTransition(String outState){
			stateInBuilt.super(outState);
			generic = true;
		}
		
		/**
		 * Builds a transition triggered by a key event with any modifier that loops on the current state.
		 */
		public KeyTransition(){
			stateInBuilt.super();
			generic = true;
		}
		
		/**
		 * Builds a transition triggered by a key event with any modifier.
		 * @param character The char corresponding to the key event
		 * @param outState The name of the output state
		 */
		public KeyTransition(char character, String outState){
			stateInBuilt.super(outState);
			this.charCode = character;
			key = false;
		}
		
		/**
		 * Builds a transition triggered by a key event with any modifier.
		 * @param keyCode The key code (ASCII) corresponding to the key event
		 * @param outState The name of the output state
		 */
		public KeyTransition(int keyCode, String outState){
			stateInBuilt.super(outState);
			this.keyCode = keyCode;
		}
		
		/**
		 * Builds a transition triggered by a key event with any modifier that loops on the current state.
		 * @param character The char corresponding to the key event
		 */
		public KeyTransition(char character){
			stateInBuilt.super();
			charCode = character;
			key = false;
		}
		
		/**
		 * Builds a transition triggered by a key event with any modifier that loops on the current state.
		 * @param keyCode The key code (ASCII) corresponding to the key event
		 */
		public KeyTransition(int keyCode){
			stateInBuilt.super();
			this.keyCode = keyCode;
		}
		
		// TODO Add modifiers
		
		/**
		 * Sets the character associated with the key event that triggers this transition.
		 * @param character The character
		 */
		public void setChar(char character){
			charCode = character;
		}
		
		/**
		 * Returns the character associated with the key event that triggers this transition.
		 * @return the character associated with the key event that triggers this transition
		 */
		public char getChar(){
			return charCode;
		}
		
		/**
		 * @param keyCode The key code (ASCII) to associate to the key event that initiates this transition
		 */
		public void setKeyCode(int keyCode){
			this.keyCode = keyCode;
		}
		
		/**
		 * @return the key code (ASCII) associating to the key event that initiates this transition
		 */
		public int getKeyCode(){
			return keyCode;
		}
		
		/**
		 * @return the awt mouse event that fires this transition.
		 */
		public InputEvent getInputEvent() {
			return (InputEvent)triggeringEvent;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public String toString() {
			if(generic) return getClass().getSuperclass().getSimpleName()+"()";
			else {
				if(key) return getClass().getSuperclass().getSimpleName()+"("+keyCode+")";
				else return getClass().getSuperclass().getSimpleName()+"("+charCode+")";
			}
		}
		
		private boolean matchesKeyOrChar(EventObject eventObject) {
			if(!(eventObject instanceof KeyEvent))
				return false;
			KeyEvent ke = (KeyEvent)eventObject;
			triggeringEvent = ke;
			if(generic) {
				keyCode = ke.getKeyCode();
				charCode = ke.getKeyChar();
				return true;
			}
			if(key) {
				if(keyCode == ke.getKeyCode()) {
					charCode = ke.getKeyChar();
					return true;
				}
				return false;
			}
			if(charCode == ke.getKeyChar()) {
				keyCode = ke.getKeyCode();
				return true;
			}
			return false;
		}
		
		protected boolean matches(EventObject eventObject, int typeEvent) {
			if(!(eventObject instanceof KeyEvent))
				return false;
			KeyEvent ke = (KeyEvent)eventObject;
			if(ke.getID() == typeEvent)
				return matchesKeyOrChar(eventObject);
			else return false;
		}
		
	}
	
	/**
	 * A transition triggered by a key typed event.
	 * A key typed event corresponds to the succession of a key press and a key release.
	 * Note however that the key press and key release are always sent.
	 * Keys can be specified either by the ASCII character they represent or by their keycode.
	 * @author Caroline Appert
	 */
	public class KeyType extends KeyTransition {
		
		/**
		 * Builds a transition triggered by a key typed event with any modifier.
		 * @param outState The name of the output state
		 */
		public KeyType(String outState){
			super(outState);
		}
		
		/**
		 * Builds a transition triggered by a key typed event with any modifier that loops on the current state.
		 */
		public KeyType(){
			super();
		}
		
		/**
		 * Builds a transition triggered by a key typed event with any modifier.
		 * @param character The char corresponding to the key event
		 * @param outState The name of the output state
		 */
		public KeyType(char character, String outState){
			super(character, outState);
		}
		
		/**
		 * Builds a transition triggered by a key typed event with any modifier.
		 * @param keyCode The key code (ASCII) corresponding to the key event
		 * @param outState The name of the output state
		 */
		public KeyType(int keyCode, String outState){
			super(keyCode, outState);
		}
		
		/**
		 * Builds a transition triggered by a key typed event with any modifier that loops on the current state.
		 * @param character The char corresponding to the key event
		 */
		public KeyType(char character){
			super(character);
		}
		
		/**
		 * Builds a transition with any modifier triggered by a key typed event that loops on the current state.
		 * @param keyCode The key code (ASCII) corresponding to the key event
		 */
		public KeyType(int keyCode){
			super(keyCode);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean matches(EventObject eventObject) {
			return matches(eventObject, KeyEvent.KEY_TYPED);
		}
	}
	
	/**
	 * A transition triggered by a key pressed event.
	 * Keys can be specified either by the ASCII character they represent or by their keycode.
	 * @author Caroline Appert
	 */
	public class KeyPress extends KeyTransition {
		
		/**
		 * Builds a transition triggered by a key pressed event with any modifier.
		 * @param outState The name of the output state
		 */
		public KeyPress(String outState){
			super(outState);
		}
		
		/**
		 * Builds a transition triggered by a key pressed event with any modifier that loops on the current state.
		 */
		public KeyPress(){
			super();
		}
		
		/**
		 * Builds a transition triggered by a key pressed event with any modifier.
		 * @param character The char corresponding to the key event
		 * @param outState The name of the output state
		 */
		public KeyPress(char character, String outState){
			super(character, outState);
		}
		
		/**
		 * Builds a transition triggered by a key pressed event with any modifier.
		 * @param keyCode The key code (ASCII) corresponding to the key event
		 * @param outState The name of the output state
		 */
		public KeyPress(int keyCode, String outState){
			super(keyCode, outState);
		}
		
		/**
		 * Builds a transition triggered by a key pressed event with any modifier that loops on the current state.
		 * @param character The char corresponding to the key event
		 */
		public KeyPress(char character){
			super(character);
		}
		
		/**
		 * Builds a transition triggered by a key pressed event with any modifier that loops on the current state.
		 * @param keyCode The key code (ASCII) corresponding to the key event
		 */
		public KeyPress(int keyCode){
			super(keyCode);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean matches(EventObject eventObject) {
			return matches(eventObject, KeyEvent.KEY_PRESSED);
		}
	}
	
	/**
	 * A transition triggered by a key released event.
	 * Keys can be specified either by the ASCII character they represent or by their keycode.
	 * @author Caroline Appert
	 */
	public class KeyRelease extends KeyTransition {
		
		/**
		 * Builds a transition triggered by a key released event with any modifier.
		 * @param outState The name of the output state
		 */
		public KeyRelease(String outState){
			super(outState);
		}
		
		/**
		 * Builds a transition triggered by a key released event with any modifier that loops on the current state.
		 */
		public KeyRelease(){
			super();
		}
		
		/**
		 * Builds a transition triggered by a key released event with any modifier.
		 * @param character The char corresponding to the key event
		 * @param outState The name of the output state
		 */
		public KeyRelease(char character, String outState){
			super(character, outState);
		}
		
		/**
		 * Builds a transition triggered by a key released event with any modifier.
		 * @param keyCode The key code (ASCII) corresponding to the key event
		 * @param outState The name of the output state
		 */
		public KeyRelease(int keyCode, String outState){
			super(keyCode, outState);
		}
		
		/**
		 * Builds a transition triggered by a key released event with any modifier that loops on the current state.
		 * @param character The char corresponding to the key event
		 */
		public KeyRelease(char character){
			super(character);
		}
		
		/**
		 * Builds a transition triggered by a key released event with any modifier that loops on the current state.
		 * @param keyCode The key code (ASCII) corresponding to the key event
		 */
		public KeyRelease(int keyCode){
			super(keyCode);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean matches(EventObject eventObject) {
			return matches(eventObject, KeyEvent.KEY_RELEASED);
		}
	}

}
