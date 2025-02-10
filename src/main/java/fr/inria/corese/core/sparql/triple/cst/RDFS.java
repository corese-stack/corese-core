package fr.inria.corese.core.sparql.triple.cst;

import fr.inria.corese.core.sparql.datatype.RDF;

 public interface RDFS extends RDF {

	 String COSNS    = "http://www.inria.fr/acacia/corese";
	 String COSPrefix =  "cos";
	 String COS      = COSNS+"#";
	 String GETPrefix =  "get";
	 String EGETPrefix =  "eget";
	 String CHECKPrefix =  "check";
	 String EGETNS    = COSNS + "/eeval#";

	 String RootPropertyQN =  COSPrefix + ":Property"; // cos:Property
	 String RootPropertyURI = COS + "Property"; //"http://www.inria.fr/acacia/corese#Property";
	 String COSPRAGMANS    = COSNS+"/pragma";
	 String COSPRAGMA    	= COSPRAGMANS+"#";
	 String ACCEPT 		= COS+"accept";
	 String FROM 		= COS+"from";
	 String GETNS    = COSNS + "/eval#";
	 String CHECKNS  = COSNS + "/check#";
	 String PPBN    = COSNS+"/bn#"; // pprint Blank Node
	 String COSSUBSTATEOF = COSNS+"#subStateOf";

	
	 String qxsdInteger 	= "xsd:integer";
	 String qxsdDouble 	= "xsd:double";
	 String qxsdDecimal 	= "xsd:decimal";
	 String qxsdString 	= "xsd:string";
	 String qxsdBoolean 	= "xsd:boolean";
	 String qrdfsLiteral	= "rdfs:Literal";
	 String qrdfsResource 	= "rdfs:Resource";
	 
	 String[] FAKEDT = {RDFSRESOURCE,qrdfsResource, RDFSLITERAL, qrdfsLiteral};


	 String qrdfFirst 	= "rdf:first";
	 String qrdfRest	= "rdf:rest";
	 String qrdfNil 	= "rdf:nil";
	 String qrdftype   = "rdf:type";
	 String rdfssubclassof   = "rdfs:subClassOf";
	 
	 String rdftype    = "rdf:type";

	 String RDFRESOURCE 	= RDF+"resource";
	 String RDFOBJECT 		= RDF+"object";
	 String RDFSUBJECT 	= RDF+"subject";
	 String RDFTYPE 		= RDF+"type";

}
