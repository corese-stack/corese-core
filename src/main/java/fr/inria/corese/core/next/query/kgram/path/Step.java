package fr.inria.corese.core.next.query.kgram.path;

import fr.inria.corese.core.next.query.kgram.api.core.Regex;

import java.util.List;

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
	boolean enter = false,
	leave = false,
	// first of a star
	loop = false,
	check = false,
	walk = false;


	public String toString(){
		String title = "epsilon";
		if (prop!=null) title = prop.toString();
		return title + " -> " + states.getFirst() + "; ";
	}

	
	public boolean isEnter(){
		return enter;
	}
	
	public boolean isLeave(){
		return leave;
	}

	
	public boolean isLoop(){
		return loop;
	}

	public boolean isCheck(){
		return check;
	}

	public boolean isWalk(){
		return walk;
	}
	
	// constant(p) or not(constant(p))
	public Regex getProperty(){
		return prop;
	}
	
	public Regex getRegex(){
		return prop;
	}

	public State getState(){
		return state;
	}
	
}
