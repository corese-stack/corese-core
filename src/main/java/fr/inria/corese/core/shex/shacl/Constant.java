package fr.inria.corese.core.shex.shacl;

/**
 *
 * @author Olivier Corby - Inria I3S - 2020
 */
public interface Constant {

    String RDF_TYPE = "a";
    String SH_SHAPE = "sh:NodeShape";
    String SH_PATH = "sh:path";
    String SH_NODE = "sh:node";
    String SH_PROPERTY = "sh:property";
    String SH_MINCOUNT = "sh:minCount";
    String SH_MAXCOUNT = "sh:maxCount";
    String SH_INVERSEPATH = "sh:inversePath";
    String SH_ALTERNATIVEPATH = "sh:alternativePath";
    String SH_ONE = "sh:xone";
    String SH_AND = "sh:and";
    String SH_OR = "sh:or";
    String SH_NOT = "sh:not";
    String SH_QUALIFIED_VALUE_SHAPE = "sh:qualifiedValueShape";
    String SH_QUALIFIED_MIN_COUNT  = "sh:qualifiedMinCount";
    String SH_QUALIFIED_MAX_COUNT  = "sh:qualifiedMaxCount";
    String SH_QUALIFIED_DISJOINT   = "sh:qualifiedValueShapesDisjoint";
    String SH_CLOSED = "sh:closed";
    
    String SH_DATATYPE = "sh:datatype";
    String SH_IN = "sh:in";
    String SH_HAS_VALUE = "sh:hasValue";
    String SH_MIN_EXCLUSIVE = "sh:minExclusive";
    String SH_MIN_INCLUSIVE = "sh:minInclusive";
    String SH_MAX_EXCLUSIVE = "sh:maxExclusive";
    String SH_MAX_INCLUSIVE = "sh:maxInclusive";
    String SH_NODE_KIND = "sh:nodeKind";
    String SH_UNDEF = "sh:Undef";
    String SH_BLANK = "sh:BlankNode";
    String SH_IRI_OR_BLANK = "sh:BlankNodeOrIRI";
    String SH_LITERAL = "sh:Literal";
    String SH_IRI = "sh:IRI";
    String SH_PATTERN = "sh:pattern";
    String SH_FLAGS = "sh:flags";
    String SH_MAXLENGTH = "sh:maxLength";
    String SH_MINLENGTH = "sh:minLength";
    String SH_LANGUAGE_IN = "sh:languageIn";
    
    String SHEX_EXTRA      = "shex:extra";
    String SHEX_OPTIONAL   = "shex:optional";
    String SHEX_COUNT      = "shex:count";
    String SHEX_CONSTRAINT = "shex:constraint";
    String SHEX_MINCOUNT   = "shex:minCount";
    String SHEX_MAXCOUNT   = "shex:maxCount";

}
