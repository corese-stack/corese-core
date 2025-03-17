package fr.inria.corese.core.sparql.datatype;

public interface RDF {
    
	 String XML   =  "http://www.w3.org/XML/1998/namespace";
	 String RDF   =  "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	 String RDFS  =  "http://www.w3.org/2000/01/rdf-schema#";
	 String XSD   =  "http://www.w3.org/2001/XMLSchema#";
	 String OWL   =  "http://www.w3.org/2002/07/owl#";
	 String RDF_HTML   =  RDF + "HTML";
         
         String FIRST = RDF+"first";
         String REST  = RDF+"rest";
         String OWL_SAME_AS = OWL+"sameAs";
	 
	 String XSDPrefix  = "xsd";
	 String RDFPrefix =  "rdf";
	 String RDFSPrefix =  "rdfs";
	 String XMLPrefix =  "xml";
	 String OWLPrefix =  "owl";
	 
	String RDFSRESOURCE= RDFS+"Resource";
	String RDFSLITERAL 	= RDFS+"Literal";
	String XMLLITERAL  	= RDF+"XMLLiteral";
	String HTML  	= RDF+"HTML";
	String LANGSTRING  = RDF+"langString";
	String BLANKSEED   = "_:";
	
	String qxsdString 	 = "xsd:string";
	String qxsdInteger  = "xsd:integer";
	String qxsdBoolean  = "xsd:boolean";
	String qxsdlangString= "rdf:langString";
	String qrdfsLiteral = "rdfs:Literal";

	String xsdboolean 	= XSD+"boolean";
	String xsdinteger 	= XSD+"integer";
	String xsdlong 	= XSD+"long";
	String xsdint 	= XSD+"int";
	String xsdshort 	= XSD+"short";
	String xsdbyte	= XSD+"byte";
	String xsddecimal 	= XSD+"decimal";
	String xsdfloat 	= XSD+"float";
	String xsddouble 	= XSD+"double";

	String xsdnonNegativeInteger = XSD+"nonNegativeInteger";
	String xsdnonPositiveInteger = XSD+"nonPositiveInteger";
	String xsdpositiveInteger 	  = XSD+"positiveInteger";
	String xsdnegativeInteger    = XSD+"negativeInteger";

	String xsdunsignedLong    = XSD+"unsignedLong";
	String xsdunsignedInt     = XSD+"unsignedInt";
	String xsdunsignedShort   = XSD+"unsignedShort";
	String xsdunsignedByte    = XSD+"unsignedByte";

	String rdflangString 	= RDF+"langString";
	String xsdstring 	= XSD+"string";
	String xsdnormalizedString 	= XSD+"normalizedString";
	String xsdtoken 	= XSD+"token";
	String xsdnmtoken 	= XSD+"NMTOKEN";
	String xsdanyURI 	= XSD+"anyURI";
	String xsdname 	= XSD+"Name";
	String xsdncname 	= XSD+"NCName";
	String xsdlanguage = XSD+"language";

	String xsdduration = XSD+"duration";
	String xsddaytimeduration = XSD+"dayTimeDuration";
	String xsddate 	= XSD+"date";
	String xsddateTime = XSD+"dateTime";
	String xsdday 		= XSD+"gDay";
	String xsdmonth 	= XSD+"gMonth";
	String xsdyear 	= XSD+"gYear";


}
