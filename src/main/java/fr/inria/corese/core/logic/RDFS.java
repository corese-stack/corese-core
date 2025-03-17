package fr.inria.corese.core.logic;

public interface RDFS {
	
	String RDFS  =  "http://www.w3.org/2000/01/rdf-schema#";
        
	String SUBPROPERTYOF = RDFS + "subPropertyOf";
	String SUBCLASSOF 	 = RDFS + "subClassOf";
	String DOMAIN 		 = RDFS + "domain";
	String RANGE 		 = RDFS + "range";
	String MEMBER 		 = RDFS + "member";
	String MEMBERSHIP 		 = RDFS + "ContainerMembershipProperty";
	String CLASS 	 	 = RDFS + "Class";
	String RESOURCE		 = RDFS + "Resource";
	String LABEL 	 	 = RDFS + "label";
	String COMMENT 	 	 = RDFS + "comment";
        	
}
