package fr.inria.corese.core.kgram.path;

import java.util.ArrayList;
import java.util.List;

import fr.inria.corese.core.kgram.api.core.Regex;

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
	end = false, 
	// start state of a loop exp*
	first = false,
	check = false;
	List<Step> list;
	Step[] steps;
	int loop = -1, count = 0;
	Regex exp;
	
	State(int n){
		num = n;
		list = new ArrayList<Step>();
	}
	
	void add(Step step){
		list.add(step);
	}
	
	void add(int i, Step step){
		list.add(i, step);
	}
	
	void set(Regex e){
		exp = e;
	}
	
	Regex getRegex(){
		return exp;
	}
	
	boolean isBound(){
		return exp != null;
	}
	
	int getCount(){
		return count;
	}
	
	void setCount(int n){
		count = n;
	}
	
	
	int getMin(){
		return getRegex().getMin();
	}
	
	int getMax(){
		return getRegex().getMax();
	}
	
	public List<Step> getSteps(){
		return list;
	}
	
	void setOut(boolean b){
		end = b;
	}
	
	public boolean isFinal(){
		return end;
	}
	
	void setFirst(boolean b){
		first = b;
	}
	
	
	/**
	 * exp+ need check loop at once
	 */
	void setPlus(boolean b){
		check = b;
	}
	
	
	void setLoop(int n){
		loop = n;
	}
	
	
	void compile(){
		steps = new Step[list.size()];
		int i = 0;
		for (Step s : list){
			steps[i++] = s;
		}
	}
	
	Step[] getTransitions(){
		return steps;
	}
	
	public String toString(){
		return "st" + num ;
	}
}
