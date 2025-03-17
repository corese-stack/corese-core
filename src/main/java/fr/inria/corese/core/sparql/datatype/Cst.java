package fr.inria.corese.core.sparql.datatype;

public interface Cst {
	/**
	 * implementation Class name for datatypes
	 */
    String pack      = "fr.inria.corese.core.sparql.datatype.";
	String extension = "fr.inria.corese.core.sparql.datatype.extension.";
	
	String jDatatype		= pack + "CoreseDatatype";
	String jTypeString		= pack + "CoreseString";
	String jTypeBoolean        = pack + "CoreseBoolean";
	String jTypeXMLString 	= pack + "CoreseXMLLiteral";
	String jTypeDouble		= pack + "CoreseDouble";
	String jTypeFloat		= pack + "CoreseFloat";
	String jTypeDecimal    	= pack + "CoreseDecimal";
	String jTypeInteger	= pack + "CoreseInteger";
	String jTypeInt            = pack + "CoreseInt";
        
	String jTypeGenericInteger	= pack + "CoreseGenericInteger";
	String jTypeLong       	= pack + "CoreseLong";
	String jTypeLiteral       	= pack + "CoreseLiteral";
	String jTypeUndef  	= pack + "CoreseUndefLiteral";
	String jTypeDate       	= pack + "CoreseDate";
	String jTypeDateTime       = pack + "CoreseDateTime";
	String jTypeDay    	= pack + "CoreseDay";
	String jTypeMonth          = pack + "CoreseMonth";
	String jTypeYear   	= pack + "CoreseYear";
	String jTypeGeneric        = pack + "CoreseGeneric";

	String jTypeURI		= pack + "CoreseURI";
	String jTypeURILiteral     = pack + "CoreseURILiteral";
	String jTypeBlank		= pack + "CoreseBlankNode";
	String jTypeArray		= pack + "CoreseArray";
        
        String jTypeJSON		= extension + "CoreseJSON";


}
