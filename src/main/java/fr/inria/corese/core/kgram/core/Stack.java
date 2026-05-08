package fr.inria.corese.core.kgram.core;

import java.util.ArrayList;

import fr.inria.corese.core.kgram.api.core.ExpType;

/**
 * 
 *  KGRAM stack of expressions
 * 
 * @author Olivier Corby, Edelweiss, INRIA 2009
 */
public class Stack extends ArrayList<Exp> implements ExpType {
	
	int level = 0;
	
	public static Stack create(Exp e){
		Stack st = new Stack();
		st.add(e);
		return st;
	}
	
//	public Exp get(int n){
	
	Stack copy(){
		Stack st = new Stack();
		st.addAll(this);
		return st;
	}
	
	
	/**
	 * Push all elements of AND in the stack
	 */
	Stack and(Exp exp, int n){
		remove(n);
		int i = 0;
		for (Exp e : exp){
                    if (e.getBind() != null){
                       add(n + i++, e.getBind()); 
                    }
                    add(n + i++, e);
		}
		return this;
	}
                
        Stack addCopy(int n, Exp exp) {
            Stack copy = copy();
            copy.add(n, exp);
            return copy;
        }
        
	
        @Override
	public String toString(){
		String str = ""; //"[" + level +"] ";
		int i = 0;
		for (Exp e : this){
			str += i++ + " " + e + ", ";
		}
		return str;
	}
	
}
