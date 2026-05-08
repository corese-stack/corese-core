package fr.inria.corese.core.kgram.path;

import java.util.ArrayList;
import java.util.List;

import fr.inria.corese.core.kgram.api.core.Regex;

/**
 * Regex Transition
 * 
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
class Step {
	Regex prop;
	List<State> states;
	State state;
	boolean epsilon = false,
	enter = false,
	leave = false,
	// first of a star
	loop = false,
	check = false,
	walk = false;
	
	Step(){
		states = new ArrayList<State>();
	}
	
	Step(Regex prop){
		this.prop = prop;
		states = new ArrayList<State>();
	}
	
	public String toString(){
		String title = "epsilon";
		if (prop!=null) title = prop.toString();
		return title + " -> " + states.get(0) + "; ";
	}
	
	void setEpsilon(boolean b){
		epsilon = b;
	}
	
	public boolean isEpsilon(){
		return epsilon;
	}
	
	void setEnter(boolean b){
		enter = b;
	}
	

	
	void setLeave(boolean b){
		leave = b;
	}
	
	
	void setLoop(boolean b){
		loop = b;
	}
	
	
	void setWalk(boolean b){
		walk = b;
	}
	
	
	void setState(State state){
		states.add(state);
		this.state = state; 
	}
	
	// constant(p) or not(constant(p))
	public Regex getProperty(){
		return prop;
	}
	
	
	public State getState(){
		return state;
	}
	
}
