package fr.inria.corese.core.next.query.kgram.path;

import java.util.ArrayList;
import java.util.List;

/**
 * Stack of epsilon transitions with path lengths.
 * Checks it does not loop.
 * use case: (p* / q*)*
 * 
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public class Stack {
	
	List<Step> steps;
	List<Integer> sizes;
	
	Stack(){
		steps = new ArrayList<>();
		sizes = new ArrayList<>();
	}


}
