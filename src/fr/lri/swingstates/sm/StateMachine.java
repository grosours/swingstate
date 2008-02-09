/*  
 *   Authors: Caroline Appert (caroline.appert@lri.fr) and Michel Beaudouin-Lafon
 *   Copyright (c) Universite Paris-Sud XI, 2007. All Rights Reserved
 *   Licensed under the GNU LGPL. For full terms see the file COPYING.
*/
package fr.lri.swingstates.sm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import fr.lri.swingstates.debug.StateMachineEventListener;
import fr.lri.swingstates.debug.Watcher;
import fr.lri.swingstates.events.VirtualEvent;
import fr.lri.swingstates.events.VirtualTimerEvent;
import fr.lri.swingstates.sm.StateMachine.State.Transition;

/**
 *  
 * <p>A state machine consists of a set of <i>states</i> and a set of <i>transitions</i>.
 * Each transition goes from an input state to an output state (which can be the same),
 * and is labelled by an <i>event</i>, an optional <i>guard</i> and an optional <i>action</i>.
 * At any one time, the machine is one of its states, called the <i>current state</i>. 
 * When the state machine receives an event, it looks for the first outgoing transition 
 * of the current state that matches the event and whose <code>guard</code> method returns True.
 * If it finds such a transition, it fires it, i.e. it calls the current state's <code>leave()</code> method, 
 * then the transition's <code>action()</code> method, finally sets the current state to the transition's output state 
 * and calls the output state's <code>enter()</code> method.
 * If no transition matches, the event is simply ignored.
 * 
 * <p>The declaration of a state machine uses Java's anonymous class as follows:
 * <ul>
 * <li> the state machine is the instance of an anonymous subclass of StateMachine, whose body contains one field per state;
 * <li> each state is an instance of an anonymous subclass of State, whose body contains one field per outgoing transition of this state; 
 * <li> each transition is an instance of an anonymous subclass of Transition, whose body contain the optional <code>guard</code> and <code>action</code> methods;
 * </ul>
 * Since we are using anonymous classes, each state machine, state and transition can contain its own fields and methods, if needed.
 * Note also that since we are using the nesting of anonymous classes, transitions have access to 
 * the fields and methods of the enclosing state and state machine, and states have access to the
 * fields and methods of the enclosing state machine.
 * 
 * <p>In summary, the structure of a state machine is as follows:
 * <pre>
 * 	StateMachine sm = new StateMachine () {
 * 		// local fields and methods if needed
 * 		...
 * 		public State s1 = new State () {
 * 			// local fields and methods if needed
 * 			...
 * 			public void enter () { ... do something when entering this state ... } // optional
 * 			public void leave () { ... do something when leaving this state ...} // optional
 *			
 *			// declare a transition to state s2 when receiving an event "anEvent"..
 *			// (see class StateMachine.State.Transition for details).
 * 			Transition t1 = new Event ("anEvent", ">> s2") {
 * 				public boolean guard () { ... return True or False ... }
 * 				public void action () { ... do something ... }
 * 			}
 * 			Transition t2 = ...
 * 		}
 * 		
 * 		public State s2 = new State () {
 * 			...
 * 		}
 * 	}
 * </pre>
 *
 *
 * @author Caroline Appert and Michel Beaudouin-Lafon
 */

public abstract class StateMachine implements ActionListener, StateMachineListener {
	
	/**
	 * The key string of events that triggered <code>AnimationStopped</code> transitions.
	 */
	public static String TIME_OUT   = "TimeOut";
	
	protected State currentState = null;
	protected State initialState = null;
	protected State stateInBuilt = null;
	
	private boolean inited = false;
	private boolean active = true;
	private boolean consumes = false;
	
	private Vector<State>            allStates = new Vector<State>();
	private Hashtable<String, State> statesByName = new Hashtable<String, State>();
	
	private Timer timer;
	
	private Watcher                          watcher = null;
	private LinkedList<StateMachineListener> stateMachineListeners = null;
	
	/**
	 * Builds a state machine. 
	 */
	public StateMachine(){
//		I don't understand why fields are not initialized at this stage...
//		reset();
	}
	
	/**
	 * Makes this state machine consume an event.
	 * Any state machine having a lower priority than this state machines 
	 * will not receive the event that this state machine is being processing.
	 * @param c True if this state machine must consume this event, false otherwise.
	 * @return this state machine.
	 */
	public StateMachine consumes(boolean c) {
		this.consumes = c;
		return this;
	}
	
	/**
	 * Tests if this state machine has consumed the last event it processed.
	 * @return true if this state machine has consumed the last event it processed, false otherwise.
	 */
	public boolean hasConsumed() {
		return this.consumes;
	}
	
	/**
	 * Returns this state machine's current state.
	 * @return the current state.
	 */
	public State getCurrentState(){
		return this.currentState;
	}
	
	/**
	 * Returns this state machine's initial state.
	 * @return the initial state.
	 */
	public State getInitialState(){
		return this.initialState;
	}
	
	/**
	 * Returns the vector containing all this state machine's states.
	 * @return the vector containing all the states.
	 */
	public Vector<State> getAllStates(){
		return this.allStates;
	}
	
	/**
	 * Adds the specified state machine event listener to receive state machine events from this state machine. 
	 * State machine events occur when a this state machine is attached, detached, resumed, reset, 
	 * suspended, goes to another state or loops on the current state. 
	 * If l is null, no exception is thrown and no action is performed.
	 * @param l The state machine event listener to add.
	 */
	public synchronized void addStateMachineListener(StateMachineEventListener l) {
		if(l == null) return;
		if(this.watcher == null)
			this.watcher = new Watcher(this);
		this.watcher.addSMEventListener(l);
	}  
	
	/**
	 * Removes the specified state machine event listener so that it no longer receives state machine events from this state machine. 
	 * State machine events occur when a this state machine is attached, detached, resumed, reset, 
	 * suspended, goes to another state or loops on the current state. 
	 * If l is null, no exception is thrown and no action is performed.
	 * @param l The state machine event listener to remove.
	 */
	public synchronized void removeStateMachineListener(StateMachineEventListener l) {
		if(l == null) return;
		if(this.watcher != null)
			this.watcher.removeSMEventListener(l);
		this.watcher = null;
	}
	
	
	/**
	 * Method called when this state machine is reset.
	 * This method does nothing. It can be redefined in derived classes.
	 * @see StateMachine#reset() 
	 */
	public void doReset() { }
	
	/**
	 * Sets the state of this state machine to the initial state.
	 * The initial state is the first state in the order of the declarations.
	 * @return this state machine.
	 */
	public StateMachine reset(){
		if (this.watcher != null) this.watcher.fireSmReset(this.getCurrentState());
		this.doReset();
		this.currentState = this.initialState;
		this.disarmTimer();
		return this;
	}
	
	/**
	 * Returns the active state of this state machine. The machine is active unless <code>suspend()</code> has been called.
	 * @return the active state of this state machine.
	 */
	public boolean isActive() {
		return this.active;
	}
	
	/**
	 * Method called when this state machine is suspended.
	 * This method does nothing. It can be redefined in derived classes.
	 * @see StateMachine#suspend() 
	 */
	public void doSuspend() { }
	
	/**
	 * Makes this state machine inactive.
	 * When a state machine is inactive, it does not process events.
	 */
	public void suspend() {
		if(this.active) this.doSuspend();
		this.active = false;
		if(this.watcher != null) this.watcher.fireSmSuspended();
	}
	
	/**
	 * Makes this state machine be active or inactive (calls <code>resume</code> or <code>suspend</code>).
	 * @param active True to makes this state machine be active, false to makes this state machine be inactive.
	 * @see StateMachine#resume()
	 * @see StateMachine#suspend()
	 */
	public void setActive(boolean active) {
		if(active) this.resume();
		else this.suspend();
	}
	
	/**
	 * Method called when this state machine is resumed.
	 * This method does nothing. It can be redefined in derived classes.
	 * @see StateMachine#resume() 
	 */
	public void doResume() { }
	
	/**
	 * Makes this state machine active.
	 * When a state machine is active, it processes events.
	 */
	public void resume() {
		if(!this.active) this.doResume();
		this.active = true;
		if(this.watcher != null) this.watcher.fireSmResumed();
	}
	
	
	
	/**
	 * Internal initialization of the state machine: resolve the state names into their corresponding objects.
	 * If not called explicitely, this is called automatically the first time a transition is fired.
	 * The only reason to call it explicitely is to avoid a delay when it is called automatically.
	 */
	public void init () {
		if (this.inited)
			return;
		
		/* 
		 * use the reflection interface to get the list of state names
		 * use the allStates vector to map them to state objects
		 * NOTE : this requires that the states are declared as public fields
		 * and makes the assumption that the order of the fields as enumerated
		 * by the reflection API is the same as the order in which the fields
		 * are constructed.
		 */
		Class smClass = this.getClass();
		Field[] publicFields = smClass.getFields();
		int stateIndex = 0;
		for (int i = 0; i < publicFields.length; i++) {
			String fieldName = publicFields[i].getName();
			Class fieldType = publicFields[i].getType();
			
			if (fieldType == State.class) {
				State state = (State) allStates.get(stateIndex++);
				// only use the field name if the state does not already have one
				if (state.name == null) {
					statesByName.put(fieldName, state);
					state.name = fieldName.intern();
				}
				// System.out.print((stateIndex > 1 ? ", ":"") + state.name);
			}
		}
		// System.out.println(".");
		inited = true;
		if(watcher != null) watcher.fireSMInited();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void actionPerformed(ActionEvent arg0) {
		if(arg0.getSource() instanceof Timer){
			processEvent(new VirtualTimerEvent((Timer)arg0.getSource()));
		}
	}
	
	/**
	 * Arms the default timer.
	 * When the timer expires, a <code>TimeOut</code> event is sent to the state machine.
	 * Each state machine has a single timer. 
	 * Calling <code>armTimer</code> before it has expired effectively rearms it.
	 * @param d the delay of the timer.
	 * @param repeat If false, only one <code>TimeOut</code> event is fired. If true, a <code>TimeOut</code> event is fired every <code>d</code> milliseconds. 
	 */
	public void armTimer(int d, boolean repeat) {
		if (this.timer != null) this.timer.stop();
		else this.timer = new Timer(d, this);
		this.timer.setDelay(d);
		this.timer.setInitialDelay(d);
		this.timer.setRepeats(repeat);
		this.timer.start();
	}
	
	/**
	 * Arms a tagged timer.
	 * When the timer expires, a <code>TimeOut</code> event is sent to the state machine.
	 * Calling <code>armTimer</code> before it has expired effectively rearms it.
	 * @param tag the tag.
	 * @param d the delay of the timer.
	 * @param repeat If false, only one <code>TimeOut</code> event is fired. If true, a <code>TimeOut</code> event is fired every <code>d</code> milliseconds. 
	 */
	public void armTimer(String tag, int d, boolean repeat) {
		TaggedTimer t = TaggedTimer.getTimer(tag);
		if (t != null) t.stop();
		else t = new TaggedTimer(tag, d, this);
		t.setDelay(d);
		t.setInitialDelay(d);
		t.setRepeats(repeat);
		t.start();
	}
	
	/**
	 * Disarms the timer.
	 */
	public void disarmTimer(){
		if(this.timer != null) this.timer.stop();
	}
	
	/**
	 * Disarms a tagged timer.
	 * @param tag the tag.
	 */
	public void disarmTimer(String tag){
		TaggedTimer t = TaggedTimer.getTimer(tag);
		if (t != null) t.stop();
	}
	
	/**
	 * Adds the specified state machine listener to receive events fired by this state machine. 
	 * Use the method <code>sendEvent</code> to make a state machine fire an event. 
	 * @param listener The state machine listener to add.
	 * @see StateMachine#fireEvent(EventObject)
	 * @see StateMachine#removeStateMachineListener(StateMachineEventListener)
	 */
	public synchronized void addStateMachineListener(StateMachineListener listener) {
		if(stateMachineListeners == null) stateMachineListeners = new LinkedList<StateMachineListener>();
		stateMachineListeners.add(listener);
	}
	
	/**
	 * Removes the specified state machine listener. 
	 * @param listener The state machine listener to remove.
	 */
	public synchronized void removeStateMachineListener(StateMachineListener listener) {
		if(stateMachineListeners == null) return;
		stateMachineListeners.remove(listener);
	}
	
	/**
	 * Makes this state machine fire a virtual event having a 
	 * given name that will be heard by all
	 * its <code>StateMachineListener</code>.
	 * @param nameEvent The name of the <code>VirtualEvent</code> to fire.
	 */
	public void fireEvent(String nameEvent) {
		if(stateMachineListeners == null) return;
		VirtualEvent event = new VirtualEvent(nameEvent);
		for(Iterator<StateMachineListener> i = stateMachineListeners.iterator(); i.hasNext(); ) {
			i.next().eventOccured(event);
		}
	}
	
	/**
	 * Makes this state machine fire an event that will be heard by all
	 * its <code>StateMachineListener</code>.
	 * @param event The event to fire.
	 * @see StateMachine#addStateMachineListener(StateMachineEventListener)
	 */
	public void fireEvent(EventObject event) {
		if(stateMachineListeners == null) return;
		for(Iterator<StateMachineListener> i = stateMachineListeners.iterator(); i.hasNext(); ) {
			StateMachineListener next = i.next();
			next.eventOccured(event);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void eventOccured(EventObject eventObject) {
		processEvent(eventObject);
	}
	
	/**
	 * Processes in the state machine a virtual event having a given name. 
	 * @param event The name of the virtual event to process
	 */
	public void processEvent(String event) {
		processEvent(new VirtualEvent(event));
	}
	
	protected Transition fireTransition(EventObject event) {
		LinkedList<Transition> trans = currentState.transitions;
		Transition hasFired = null;
		if(trans!=null){
			for(Iterator i = trans.iterator(); i.hasNext(); ){
				if(hasFired != null) break;
				Transition t = (Transition)(i.next());
				if(t.matches(event)) {
					if (fireTransition(t)) {
						hasFired = t;
						return hasFired;
					}
				}
			}
		}
		return hasFired;
	}
	
	/**
	 * Processes in the state machine the virtual event received. 
	 * @param event The virtual event to process
	 */	
	public void processEvent(final EventObject event) {
		if(!isActive()) return;
		if(SwingUtilities.isEventDispatchThread()) {
			fireTransition(event);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					fireTransition(event);
				}
			});
		}
	}
	
	/**
	 * Attempt to fire transition <code>t</code>.
	 * If the transition's guard evaluates to true, leave the current state,
	 * execute the transition's action and enter the destination state.
	 * If the destination state is not specified, stay in the current state
	 * and do not execute the leave/enter actions.
	 * @param t the transition to fire
	 * @return true if the transition was fired, false otherwise
	 */
	protected boolean fireTransition (State.Transition t) {
		if (! t.guard()) {
			return false;
		}
		if (t.getOutputState () != null && t.getOutputState() != currentState) currentState.leave();
		t.action();
		if (t.getOutputState () != null && t.getOutputState() != currentState) {
			if(watcher != null) {
				watcher.fireStateChanged(t, currentState, t.getOutputState());
			}
			currentState = t.getOutputState ();
			t.getOutputState().enter();
		} else {
			if(watcher != null) watcher.fireStateLooped(t, currentState);
		}
		return true;
	}
	
	/**
	 * Look up a state by its name. The state's name can be set explicitely when creating it (<code>new State("myState")</code>),
	 * otherwise the state name is the name of the field where it is stored in the state machine (<code>public State myState = new State()</code>).
	 * Note that for this to work, the field must be declared public.
	 * @param s the name of the state to look up
	 * @return the state object
	 * @throws StateNotFoundException if the state is not found in this machine.
	 */
	public State getState(String s) throws StateNotFoundException {
		if (! inited)
			init();
		State state = (State)(statesByName.get(s));
		if (state == null) 
			throw new StateNotFoundException(s);
		return state;
		/*
		 * for reasons not clear to me, the code below always raises an exception
		 * saying that we don't have access to the value of the field :-(
		 * That's why we use the workaround in StateMachine.init() and State()
		 * to map a field name to a state
		 Class c = StateMachine.this.getClass();
		 Field[] publicFields = c.getFields();
		 for (int i = 0; i < publicFields.length; i++) {
		 String fieldName = publicFields[i].getName();
		 Class fieldType = publicFields[i].getType();
		 System.out.println("Name: " + c.getName() + ":" + fieldName + 
		 ", Type: " + fieldType.getName());
		 try {
		 Object fieldValue = publicFields[i].get(StateMachine.this);
		 if (fieldValue instanceof StateMachine.State && s.equals(fieldName)) {
		 return (State) fieldValue;
		 }
		 } catch (Exception e) {
		 System.out.println(e);
		 }
		 
		 }
		 System.err.println("StateMachine error : The state "+s+" does not exist");
		 return null;
		 */
	}			
	
	
	/**
	 * A state of a state machine.
	 * 
	 * <p>This is an inner class of <code>StateMachine</code> and is meant to be used as follows:
	 * 
	 * <pre>
	 * 	StateMachine sm = new StateMachine ("sm") {
	 * 		...
	 * 		public State myState = new State() {
	 *  	 		... declare transitions ...
	 * 		}
	 * 		...
	 * 	}
	 * </pre>
	 * 
	 *  The string name of the state, which is used to specify output states in transitions,
	 *  will be the name of the field (<code>"myState"</code> in this example).
	 *  For this to work, the field must be declared public.
	 * 
	 * @see StateMachine
	 * @author Caroline Appert
	 */
	public abstract class State {
		
		String name = null;
		LinkedList<Transition> transitions;
		
		/**
		 * Builds a new state. 
		 * The string name of the state, which is used to specify output states in transitions,
		 * will be the name of the field this object is assigned to in the parent state machine.
		 * For this to work, the field must be declared public.
		 * The first state in a state machine is its initial state, i.e. the current state
		 * when the machine is first created and when it is reset.
		 */
		public State () {
			stateInBuilt = this;
			transitions = new LinkedList<Transition>();
			allStates.add(this);	// store our address in allStates so it can be later mapped to the state field name (see StateMachine.init())
			if(initialState == null){
				currentState = this;
				initialState = this;
			}  
		}
		
		/**
		 * Builds a new state with a given name.
		 * THe names of the states must be unique within a state machine. 
		 * The first state in a state machine is its initial state, i.e. the current state
		 * when the machine is first created and when it is reset.
		 * @param n The string name of the state, which is used to specify destination state in transitions.
		 */
		public State (String n) {
			this();
			name = n.intern();
			statesByName.put(n,this);
		}
		
		/**
		 * @return The name of this state
		 */
		public String getName(){
			return name;
		}
		
		/**
		 * @return The result of the <code>toString</code> default method.
		 */
		public String oldToString(){
			return super.toString();
		}
		
		/**
		 * @return The output transitions of this state.
		 */
		public LinkedList<Transition> getTransitions(){
			return transitions;
		}
		
		/**
		 * The method called when the parent state machine enters this state.
		 * This method does nothing. It can be redefined in derived classes
		 * to specify what to do when this state becomes active.
		 * This is done as follows when using an anoynous class:
		 * <pre>
		 * 	State s = new State () {
		 * 		public void enter () { ... do something ...}
		 * 		...
		 * 	}
		 * </pre>
		 */
		public void enter(){ }
		
		/**
		 * The method called when the parent state machine leaves this state.
		 * This method does nothing. It can be redefined in derived classes
		 * to specify what to do when this state becomes active.
		 * This is done as follows when using an anoynous class:
		 * <pre>
		 * 	State s = new State () {
		 * 		public void leave () { ... do something ...}
		 * 		...
		 * 	}
		 * </pre>
		 */
		public void leave(){ }
		
		/**
		 * A transition of a state machine.
		 * 
		 * <p>
		 * This is an inner class of <code>StateMachine.State</code> and is meant to
		 * be used as follows:
		 * <pre>
		 * 	Transition t = new &lt;eventtype&gt; (&lt;parameters&gt;, &lt;output state&gt;) {
		 *  		public boolean guard () { ... return True if transitions enabled ... } // optional
		 *  		public void action () { ... transition action ... } // optional
		 * 	}
		 * </pre>
		 * 
		 * <p>The <code>Transition</code> class has many derived classes corresponding to the various types of events that can be handeled by a state machine.
		 * <code>&lt;eventtype&gt;</code> represents one of these classes and &lt;parameters&gt; the corresponding parameters.
		 * The complete list of events is given below, refer to the corresponding classes for further details.
		 * 
		 * <p><code>&lt;output state&gt</code> is the specification of the output state of the transition.
		 * It is a string containing the name of the output state, which is, in general, the name of the
		 * field of the state machine that holds the state (see class StateMachine.State).
		 * In order to make it easier to spot the output state in the declaration, the name can be prefixed by
		 * any combination of the following characters: -, =, &gt; and space. This makes it possible to 
		 * specify a transition to state <code>s2</code> with strings such as <code>"-> s2", "==> s2", ">> s2"</code>, etc.
		 * 
		 * @see StateMachine
		 * @author Caroline Appert
		 */
		public abstract class Transition {
			
			/**
			 * The event object that has triggered this transition.
			 */
			protected EventObject triggeringEvent = null;
			
			/**
			 * The output state of this transition.
			 */
			protected State outputState = null;
			
			/**
			 * The name of the output state of this transition.
			 */
			protected String outputStateName;
			
			/**
			 * Builds a transition with any modifier.
			 * @param keyEvent The string describing the events 
			 * that triggers this transition.
			 * @param outState The name of the output state
			 */
			protected Transition(String outState) {
				setOutputStateName(outState);
				addTransition();
			}
			
			/**
			 * Builds a transition with any modifier 
			 * that loops on the current state.
			 * @param keyEvent The string describing the events 
			 * that triggers this transition.
			 */
			protected Transition() {
				outputStateName = null;
				addTransition();
			}
			
			private ArrayList<String> mustBeBefore() {
				String ownClassName = this.getClass().getSuperclass().getSimpleName();
				ArrayList<String> res = null;
				if(ownClassName.endsWith("OnTag")) {
					res = new ArrayList<String>();
					res.add(ownClassName.substring(0, ownClassName.length()-5));
					res.add(ownClassName.substring(0, ownClassName.length()-5)+"OnShape");
					res.add(ownClassName.substring(0, ownClassName.length()-5)+"OnComponent");
				} else {
					if(ownClassName.endsWith("OnComponent")) {
						res = new ArrayList<String>();
						res.add(ownClassName.substring(0, ownClassName.length()-11));
					} else {
						if(ownClassName.endsWith("OnShape")) {
							res = new ArrayList<String>();
							res.add(ownClassName.substring(0, ownClassName.length()-7));
						} else {
							if(ownClassName.startsWith("Tagged")) {
								res = new ArrayList<String>();
								res.add(ownClassName.substring(6, ownClassName.length()));
							}
						}
					}
				}
				return res;
			}
			
			/**
			 * Adds this transition to the enclosing state.
			 */
			protected void addTransition() {
				ArrayList<String> transitionsAfter = mustBeBefore();
				if(transitionsAfter == null) transitions.add(this);
				else {
					int i = 0;
					for(; i < transitions.size(); i++) {
						if(transitionsAfter.contains(transitions.get(i).getClass().getSuperclass().getSimpleName())) break;
					}
					transitions.add(i, this);
				}
			}
			
			/**
			 * Removes this transition to the enclosing state.
			 */
			protected void removeTransition() {
				transitions.remove(this);
			}
			
			/**
			 * Sets the name of the output state of this transition.
			 * @param outState The name of the output state
			 */
			protected void setOutputStateName(String outState) {
				/* 
				 * trim leading chars: '-=> ' 
				 * so we can write the destination state as
				 * "-> s", "=> s", ">> s", etc.
				 */
				int i = 0;
				int c = outState.charAt(i);
				while (c == '-' || c == '=' || c == '>' || c == ' ') {
					c = outState.charAt(++i);
				}
				
				outputStateName = i > 0 ? outState.substring(i) : outState;
				outputState = null;
			}
			
			/**
			 * @return the output state of this transition.
			 */
			public State getOutputState() {
				if (outputStateName == null) {
					// a null output state means we stay in the same state
					// => we don't call leave/enter
					return null;
				}
				if (outputState == null) {
					try {
						outputState = getState(outputStateName);
					} catch (StateNotFoundException e) {
						System.err.println("Destination state " 
								+ e.getStateName()
								+ " not found in state machine " 
								+ StateMachine.this
								+ ". Make sure the state is declared public.");
						return null;
					}
				}
				return outputState;
			}
			
			/**
			 * @return the input state of this transition.
			 */
			public State getInputState() {
				return State.this;
			}
			
			/**
			 * Tests if an event can trigger that transition.
			 * @param eventObject The event to test
			 * @return True if the <code>eventObject</code> 
			 * can trigger this transition. 
			 */
			public boolean matches(EventObject eventObject) {
				triggeringEvent = eventObject;
				return true;
			}
			
			/**
			 * @return The default string that would be returned 
			 * by java.lang.Object.toString().
			 */
			public String oldToString() {
				return super.toString();
			}
			
			/**
			 * Method called when an event matching this transition 
			 * is received to decide whether it can be fired.
			 * This method always returns true. 
			 * It can be redefined in subclasses, e.g.:
			 * <pre>
			 * 	Transition t = new Press (BUTTON1) {
			 * 		public boolean guard() { ... return true or false ... }
			 * 	}
			 * </pre>
			 * @return True if this transition must be fired.
			 */
			public boolean guard() {
				return true;
			}
			
			/**
			 * Method called when this transition is fired.
			 * This method does nothing. 
			 * It can be redefined in subclasses, e.g.:
			 * <pre>
			 * 	Transition t = new Press (BUTTON1) {
			 * 		public void action() { ... do something ... }
			 * 	}
			 * </pre>
			 */
			public void action() { }
			
			/**
			 * Returns the event that has just been received. 
			 * @return the virtual event that has just been received, 
			 * null if this transition is not fired by a virtual event.
			 */
			public EventObject getEvent() {
				return triggeringEvent;
			}
			
			
		}
		
		/**
		 * A transition triggered in a high-level event.
		 * The <code>Event</code> class can be directly used or extended to define your own events.
		 * The above example shows how a state machine, <code>sm1</code>, can receive "longPress" events provided
		 * by another state machine, <code>sm2</code>.
		 * <pre>
		 * StateMachine sm1, sm2;
		 * ...
		 * sm1 = new StateMachine() {
		 *
		 *			public State start = new State("start") {
		 *				public Transition event = new Event("longPress") {
		 *					public void action() {
		 *						System.out.println("a long press event");
		 *				}
		 *	        };
		 *		};
		 * };
		 *
		 * sm2 = new StateMachine() {
		 * 	
		 * 	public State start = new State("start") {
		 *		public Transition press = new Press(BUTTON1, ">> wait") {
		 *			public void action() {
		 *				armTimer(1000);
		 *			}
		 *		};
		 *	};
		 *	
		 *	public State wait = new State("wait") {
		 *		public Transition release = new Release(BUTTON1, ">> start") {
		 *			public void action() {
		 *				disarmTimer();
		 *			}
		 *		};
		 *		
		 *		public Transition longPress = new TimeOut(">> start") {
		 *			public void action() {
		 *				sm1.processEvent(new VirtualEvent("longPress"));
		 *			}
		 *		};
		 *	};
		 * };
		 *
		 * </pre>
		 * 		
		 * @author Caroline Appert
		 */
		
		public class Event<E> extends Transition {
			
			protected Class<?> classEvent = null;
			
			/**
			 * The string describing the event that triggers this transition.
			 */
			protected String event;
			
			/**
			 * Builds a transition with no modifier.
			 * @param keyEvent The event that triggers this transition
			 * @param outputState The name of the output state
			 */
			public Event(String keyEvent, String outputState) {
				super(outputState);
				event = keyEvent;
			}
			
			/**
			 * Builds a transition on a position with no modifier 
			 * that loops on current state.
			 * @param keyEvent The event that triggers this transition
			 */
			public Event(String keyEvent) {
				super();
				event = keyEvent;
			}
			
			/**
			 * Builds a transition with no modifier that 
			 * is triggered by any virtual events
			 * whose type is a subclass of <code>eventClass</code>.
			 * @param eventClass The class of events
			 * @param outputState The name of the output state
			 */
			public Event(Class eventClass, String outputState) {
				super(outputState);
				classEvent = eventClass;
			}
			
			/**
			 * Builds a transition with no modifier that 
			 * is triggered by any virtual events
			 * whose type is a subclass of <code>eventClass</code>.
			 * @param eventClass The class of events
			 */
			public Event(Class eventClass) {
				super();
				classEvent = eventClass;
			}
			
			/**
			 * {@inheritDoc}
			 */
			public boolean matches(EventObject eventObject) {
				super.matches(eventObject);
				if(classEvent != null) {
					triggeringEvent = eventObject;
					return classEvent.isAssignableFrom(eventObject.getClass());
				} else {
					if (eventObject instanceof VirtualEvent) {
						return event.compareTo(
								((VirtualEvent) eventObject).getNameEvent()) == 0;
					}
				}
				return false;
			}
			
			/**
			 * {@inheritDoc}
			 */
			public String toString() {
				if(classEvent != null) return "Event("+classEvent.getSimpleName()+".class)";
				else return event;
			}

		}
		
		/**
		 * A transition triggered by a timer.
		 * The timer is armed by the <code>armTimer</code> 
		 * method in <code>StateMachine</code>.
		 * @author Caroline Appert
		 * @see StateMachine#armTimer(int, boolean)
		 */
		public class TimeOut extends Event {
			
			/**
			 * Builds a transition triggered by a timer event.
			 */
			public TimeOut() {
				super(TIME_OUT);
			}
			
			/**
			 * Builds a transition triggered by a timer event.
			 * @param outState The name of the output state
			 */
			public TimeOut(String outState) {
				super(TIME_OUT, outState);
			}
			
			/**
			 * {@inheritDoc}
			 */
			public String toString() {
				return TIME_OUT;
			}
			
			/**
			 * {@inheritDoc}
			 */
			public boolean matches(EventObject eventObject) {
				return (eventObject instanceof VirtualTimerEvent) && super.matches(eventObject);
			}
			
		}
		
		/**
		 * A transition triggered by a tagged timer.
		 * The timer is armed by the <code>armTimer</code> 
		 * method in <code>StateMachine</code>.
		 * @author Caroline Appert
		 * @see StateMachine#armTimer(String, int, boolean)
		 */
		public class TaggedTimeOut extends TimeOut {
			
			private String tag;
			
			/**
			 * Builds a transition triggered by a time out event originated by a tagged timer.
			 * @param tag the tag of the timer.
			 * @param outState The name of the output state
			 */
			public TaggedTimeOut(String tag, String outState) {
				super(outState);
				this.tag = tag;
			}
			
			/**
			 * Builds a transition triggered by a time out event originated by a tagged timer.
			 * @param tag the tag of the timer.
			 */
			public TaggedTimeOut(String tag) {
				super();
				this.tag = tag;
			}
			
			/**
			 * {@inheritDoc}
			 */
			public boolean matches(EventObject eventObject) {
				if (super.matches(eventObject) && (((VirtualTimerEvent)eventObject).getTimer() instanceof TaggedTimer))
					return ((TaggedTimer)((((VirtualTimerEvent)eventObject).getTimer()))).getTagName().compareTo(tag) == 0;
				return false;
			}
			
		}
	}
	
}
