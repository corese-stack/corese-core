package fr.inria.corese.core.sparql.triple.api;

import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.parser.Atom;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Variable;

/**
 *  API of Atom for backward engine
 * @author corby
 *
 */
public interface ElementClause {
	
	/**
	 * the Atom is a constant?
	 */
    boolean isConstant();
	
	/**
	 * the Atom is a variable?
	 */
    boolean isVariable();
	
	/**
	 * value of the element
	 */
    Constant getConstant();
	
	IDatatype getDatatypeValue();
	
	/**
	 * name of the element
	 */
    Variable getVariable();
	
	Atom getAtom();
	
	String getName();
	
}
