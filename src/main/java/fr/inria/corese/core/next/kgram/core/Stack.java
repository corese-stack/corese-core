package fr.inria.corese.core.next.kgram.core;

import fr.inria.corese.core.next.kgram.api.core.ExpType;

import java.util.ArrayList;

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

	
	public int getLevel(){
		return level;
	}
	
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
		StringBuilder str = new StringBuilder(); //"[" + level +"] ";
		int i = 0;
		for (Exp e : this){
			str.append(i++).append(" ").append(e).append(", ");
		}
		return str.toString();
	}
	
}
