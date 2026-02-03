package fr.inria.corese.core.next.query.kgram.path;

import java.util.ArrayList;
import java.util.List;

/**
 * Regex State
 * 
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
class State {
	int num;
	boolean
	// final state
	end = false ;
	List<Step> list;
 	int loop = -1 ;

	State(int n){
		num = n;
		list = new ArrayList<>();
	}


	public List<Step> getSteps(){
		return list;
	}
	
	public boolean isFinal(){
		return end;
	}
	
	public int getLoop(){
		return loop;
	}


	public String toString(){
		return "st" + num ;
	}
}
