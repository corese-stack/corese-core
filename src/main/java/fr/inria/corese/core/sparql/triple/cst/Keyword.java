package fr.inria.corese.core.sparql.triple.cst;

public interface Keyword {
	
	String REGEX = "regex";
	String SOR = "|";
	String SEOR = "||";
	String SEAND = "&&";
	String SENOT = "!";
	String SBE = "^";
	
	String SPLUS = "+";
	String SMINUS = "-";
	String SMULT = "*";
	String SDIV = "/";
	String SQ = "?";
	String SEQ = "=";
	String SNEQ = "!=";
	String STLEC = "<=::"; // for classes
	String STLE = "<=:";

	String SINV = "i";
	String SSHORT = "s";
	String SSHORTALL = "sa";
	String SHORT = "short";

	String DISTINCT = "distinct";

	String SDEPTH = "d";
	String SBREADTH = "b";
	
	String MATCH = "match";
	String STNOT = "not";
	
	String SFAIL = "fail";


}
