package fr.inria.corese.core.logic;

public interface OWL {
	
	String OWL   =  "http://www.w3.org/2002/07/owl#";

	String CLASS            = OWL + "Class";
	String THING            = OWL + "Thing";
	
	String INTERSECTIONOF   = OWL + "intersectionOf";
	String UNIONOF          = OWL + "unionOf";
	String EQUIVALENTCLASS  = OWL + "equivalentClass";
	String COMPLEMENTOF     = OWL + "complementOf";
 	String DISJOINTWITH     = OWL + "disjointWith";
       
	String ALLVALUESFROM    = OWL + "allValuesFrom";
	String SOMEVALUESFROM   = OWL + "someValuesFrom";
	String ONCLASS          = OWL + "onClass";
        
	
	String INVERSEOF        = OWL + "inverseOf";
	String EQUIVALENTPROPERTY= OWL + "equivalentProperty";
	String SYMMETRIC        = OWL + "SymmetricProperty";
	String TRANSITIVE       = OWL + "TransitiveProperty";
	String REFLEXIVE        = OWL + "ReflexiveProperty";
	
	String TOPOBJECTPROPERTY= OWL + "topObjectProperty";
	String TOPDATAPROPERTY  = OWL + "topDataProperty";
	String SAMEAS           = OWL + "sameAs";
	
}
