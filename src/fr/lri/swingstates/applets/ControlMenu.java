/*  
 *   Authors: Caroline Appert (caroline.appert@lri.fr)
 *   Copyright (c) Universite Paris-Sud XI, 2007. All Rights Reserved
 *   Licensed under the GNU LGPL. For full terms see the file COPYING.
 */
package fr.lri.swingstates.applets;

import java.awt.Font;
import java.awt.geom.Point2D;

import javax.swing.DefaultBoundedRangeModel;

import fr.lri.swingstates.animations.AnimationTransparency;
import fr.lri.swingstates.canvas.CEllipse;
import fr.lri.swingstates.canvas.CExtensionalTag;
import fr.lri.swingstates.canvas.CIntentionalTag;
import fr.lri.swingstates.canvas.CPolyLine;
import fr.lri.swingstates.canvas.CShape;
import fr.lri.swingstates.canvas.CStateMachine;
import fr.lri.swingstates.canvas.CText;
import fr.lri.swingstates.canvas.Canvas;
import fr.lri.swingstates.events.VirtualEvent;
import fr.lri.swingstates.sm.State;
import fr.lri.swingstates.sm.Transition;
import fr.lri.swingstates.sm.transitions.Drag;
import fr.lri.swingstates.sm.transitions.Press;
import fr.lri.swingstates.sm.transitions.Release;
import fr.lri.swingstates.sm.transitions.TimeOut;

class SimpleMenuItem extends MenuItem {
	
	Command command;
	
	public SimpleMenuItem(Command command) {
		super(command.getCommandName());
		this.command = command;
	}

	public Command getCommand() {
		return command;
	}
	
}

class ControlMenuItem extends SimpleMenuItem {
	public ControlMenuItem(Command command) {
		super(command);
	}
}

abstract class Command extends VirtualEvent {
	
	Canvas canvas;
	
	public Command(Canvas canvas, String commandName) {
		super(commandName);
		this.canvas = canvas;
	}
	
	
	public String getCommandName() {
		return getNameEvent();
	}

	public MenuItem getTagItem(Canvas canvas) {
		return new SimpleMenuItem(this);
	}
	
	public abstract void apply(CShape shape, Point2D pt, double valueParam);


	public Canvas getCanvas() {
		return canvas;
	}

	public void start(CShape shape, Point2D pt) { }
	
}

class ContinuousCommand extends Command {

	private DefaultBoundedRangeModel rangeModel;
	
	public ContinuousCommand(Canvas canvas, String commandName, DefaultBoundedRangeModel rangeModel) {
		super(canvas, commandName);
		this.rangeModel = rangeModel;
	}
	
	public ContinuousCommand(Canvas canvas, String commandName, int value, int extent, int min, int max) {
		super(canvas, commandName);
		this.rangeModel = new DefaultBoundedRangeModel(value, extent, min, max);
	}

	public DefaultBoundedRangeModel getRangeModel() {
		return rangeModel;
	}
	
	public void apply(CShape shape, Point2D pt, double valueParam) {
		rangeModel.setValue((int)valueParam);
	}
	
	public MenuItem getTagItem(Canvas canvas) {
		return new ControlMenuItem(this);
	}
	
}



public class ControlMenu extends Menu {
	private Point2D pInit;
	private CShape background;
	private CText feedback;
	
	public ControlMenu(GraphicalEditor canvas, Command[] items) {
		super(canvas);
		menuLayout(items);
		tagWhole.setDrawable(false).setPickable(false);
		
		feedback = canvas.newText(0, 0, " ", new Font("verdana", Font.PLAIN, 12));
		feedback.setReferencePoint(0, 1).setDrawable(false).setPickable(false);

		interaction = new CStateMachine() {
			
			private Command lastItemVisited;
			private Point2D lastPoint;
			private CShape shapeUnderMenu;
			
			public State menuOff = new State() {
				public void enter() {
					feedback.setDrawable(false);
					disarmTimer();
					hideMenu();
				}
				
				Transition invokeOnShape = new PressOnShape(BUTTON1, ">> menuOn") {
					public void action() {
						pInit = getPoint();
						lastPoint = pInit;
						shapeUnderMenu = getShape();
						showMenu(pInit);
					}
				};
				Transition invoke = new Press(BUTTON1, ">> menuOn") {
					public void action() {
						pInit = getPoint();
						lastPoint = pInit;
						shapeUnderMenu = null;
						showMenu(pInit);
					}
				};
				public void leave() {
					background.setPickable(true);
					// menu is interactive but not visible
					tagWhole.setPickable(true);
					tagLabels.setPickable(false);
					// 300
					armTimer(1000, false);
				}
			};

			public State menuOn = new State() {
				Transition showMenu = new TimeOut() {
					public void action() {
						tagWhole.setDrawable(true);
					}
				};
				Transition stop = new Release(BUTTON1, ">> menuOff") { };
				Transition enterOnContinuousItem = new EnterOnTag(ControlMenuItem.class, ">> continuousItem") {
					public void action() {
						lastItemVisited = ((ControlMenuItem)getTag()).getCommand();
						lastPoint = getPoint();
					}
				};
				Transition enterOnSimpleItem = new EnterOnTag(SimpleMenuItem.class, ">> simpleItem") {
					public void action() {
						lastItemVisited = ((SimpleMenuItem)getTag()).getCommand();
						lastPoint = getPoint();
					}
				};
			};
			
			public State simpleItem = new State() {
				Transition showMenu = new TimeOut() {
					public void action() {
						tagWhole.setDrawable(true);
					}
				};
				Transition stop = new Release(BUTTON1, ">> menuOff") { };
				Transition enterOnContinuousItem = new EnterOnTag(ControlMenuItem.class, ">> continuousItem") {
					public void action() {
						lastItemVisited = ((ControlMenuItem)getTag()).getCommand();
						lastPoint = getPoint();
					}
				};
				Transition enterOnSimpleItem = new EnterOnTag(SimpleMenuItem.class) {
					public void action() {
						lastItemVisited = ((SimpleMenuItem)getTag()).getCommand();
					}
				};
				Transition selectCommand = new EnterOnTag("background", ">> menuOff") {
					public void action() {
						// to see shapes that are potentially under the background shape
						// that we just use to detect crossing
						getTag().setPickable(false);
						lastItemVisited.apply(shapeUnderMenu, getPoint(), 0);
					}
				};
			};
			public State continuousItem = new State() {
				Transition showMenu = new TimeOut() {
					public void action() {
						tagWhole.setDrawable(true);
					}
				};
				Transition stop = new Release(BUTTON1, ">> menuOff") { };
				Transition enterOnContinuousItem = new EnterOnTag(ControlMenuItem.class) {
					public void action() {
						lastItemVisited = ((ControlMenuItem)getTag()).getCommand();
						lastPoint = getPoint();
					}
				};
				Transition enterOnSimpleItem = new EnterOnTag(SimpleMenuItem.class, ">> simpleItem") {
					public void action() {
						lastItemVisited = ((SimpleMenuItem)getTag()).getCommand();
					}
				};
				Transition selectCommand = new EnterOnTag("background", ">> control") {
					public void action() {
						// to see shapes that are potentially under the background shape
						// that we just use to detect crossing
						getTag().setPickable(false);
						lastItemVisited.start(shapeUnderMenu, pInit);
						lastPoint = getPoint();
					}
				};
			};
			public State control = new State() {
				public void enter() {
//					tagWhole.setTransparencyFill(0.5f);
					DefaultBoundedRangeModel rm = ((ContinuousCommand)lastItemVisited).getRangeModel();
					feedback.setText(""+rm.getValue()).aboveAll();
				}
				Transition control = new Drag(BUTTON1) {
					public void action() {
						DefaultBoundedRangeModel rm = ((ContinuousCommand)lastItemVisited).getRangeModel();
						double newVal = rm.getValue()+(getPoint().getX() - lastPoint.getX());
						lastItemVisited.apply(shapeUnderMenu, getPoint(), newVal);
						feedback.setText(""+rm.getValue()).translateTo(getPoint().getX()+10, getPoint().getY()-10).setDrawable(true);
						lastPoint = getPoint();
					}
				};
				Transition stop = new Release(BUTTON1, ">> menuOff") { };
			};
			
		};
		interaction.attachTo(canvas);
	}

	void showMenu(Point2D pt) {
		backgroundDisapear.stop();
		labelsDisapear.stop();
		
		tagWhole.setDrawable(false);
		parent.translateTo(pt.getX(), pt.getY()).setDrawable(true);
		background.setPickable(true).aboveAll();
		tagWhole.setTransparencyFill(1).setTransparencyOutline(1).setPickable(true).aboveAll();
		tagLabels.setPickable(false).aboveAll();
	}

	AnimationTransparency backgroundDisapear = new AnimationTransparency(0);
	AnimationTransparency labelsDisapear = new AnimationTransparency(0);
	
	CStateMachine machineAnimations = new CStateMachine(canvas) {
		public State idle = new State() {
			Transition bgInvisible = new AnimationStarted(backgroundDisapear, ">> bgDisapearing") { };
		};
		public State bgDisapearing = new State() {
			Transition bgInvisible = new AnimationStopped(backgroundDisapear, ">> labelsDisapearing") {
				public void action() {
					tagLabels.animate(labelsDisapear);
				}
			};
		};
		public State labelsDisapearing = new State() {
			Transition labelsInvisible = new AnimationStopped(labelsDisapear, ">> idle") {
				public void action() {
					tagWhole.setDrawable(false).setPickable(false);
				}
			};
		};
	};
	
	CIntentionalTag menuXorLabel = new CIntentionalTag(canvas) {
		public boolean criterion(CShape s) {
			return s.hasTag(tagWhole) && !s.hasTag(tagLabels);
		}
	};
	
	void hideMenu() {
		menuXorLabel.animate(backgroundDisapear);
		background.setPickable(false);
	}
	
	void menuLayout(Command[] items) {
		parent = canvas.newEllipse(-5, -5, 10, 10);
		background = canvas.newRectangle(-200, -200, 400, 400);
		background.setDrawable(false).setParent(parent).addTag("background");
		
		CShape clipCircle = (new CEllipse(-50, -50, 100, 100)).getSubtraction(new CEllipse(-30, -30, 60, 60));
		clipCircle.setFilled(false).setOutlined(false);
		clipCircle.addTag(tagWhole).setParent(parent);

		CPolyLine bgItem;
		CText labelItem;
		double angleStep = 2*Math.PI / items.length;
		for(int i = 0; i < items.length; i++) {
			bgItem = canvas.newPolyLine(0, 0)
			.lineTo(50,0).arcTo(0, -angleStep, 50, 50).close();
			
			CExtensionalTag t = items[i].getTagItem(canvas);
			
			bgItem.addTag(tagWhole).addTag(t);
			bgItem.setReferencePoint(0, 0).rotateBy(i*angleStep)
			.setFillPaint(Menu.BG_COLOR).setOutlinePaint(Menu.BORDER_COLOR).setClip(clipCircle);

			labelItem = (CText) canvas.newText(0, 0, items[i].getCommandName(), Menu.FONT).setPickable(false)
			.addTag(tagWhole).addTag(tagLabels)
			.setReferencePoint(0.5, 0.5).translateTo(
					Math.cos((i*angleStep)+angleStep/2)*40, Math.sin((i*angleStep)+angleStep/2)*40);

			parent.addChild(labelItem).addChild(bgItem);

		}

		parent.addTag(tagWhole);
		tagLabels.aboveAll();
		parent.aboveAll();
	}

}
