package fr.inria.corese.core.kgram.api.core;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * Interface of Property Path Regex
 * 
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public interface Regex {
	
	int UNDEF	= -1;
	int LABEL	= 0;
	int NOT 	= 1;
	int SEQ 	= 2;
	int STAR 	= 3;
	int PLUS 	= 4;
	int OPTION 	= 5;
	int COUNT 	= 6;
	int ALT		= 7;
	int PARA	= 8;
	int TEST	= 9;
	int CHECK	= 10;
	int REVERSE	= 11;

	
	String getName();
	
	String getLongName();
        
        IDatatype getDatatypeValue();
	
	String toRegex();
	
	int retype();
	
	int getArity();
	
	boolean isConstant();
	
	boolean isAlt();
	
	boolean isPara();

	boolean isSeq();
	
	boolean isOpt();

	boolean isNot();
		
	boolean isDistinct();

	boolean isShort();

	// @deprecated
	boolean isInverse();

	// @deprecated
	void setInverse(boolean b);
	
	// SPARQL 1.1 reverse ^
	boolean isReverse();
	
	void setReverse(boolean b);

	boolean isStar();
	
	boolean isPlus();
	
	boolean isCounter();

	int getMin();
	
	int getMax();
	
	int getWeight();
	
	Regex getArg(int n);

	Regex reverse();
	
	Regex transform();

	Regex translate();
	
	boolean isNotOrReverse();

	int regLength();
	
	Expr getExpr();

}
