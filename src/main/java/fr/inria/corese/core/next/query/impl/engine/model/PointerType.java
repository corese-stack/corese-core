package fr.inria.corese.core.next.query.impl.engine.model;

/**
 * Pointer type for object that can be object of CoresePointer
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2019
 */
public enum PointerType {

    UNDEF("pointer"),
    MAPPINGS("mappings"),
    MAPPING("mapping"),
    GRAPH("graph"),
    NODE("node"),
    TRIPLE("triple"),
    PATH("path"),
    QUERY("query"),
    EXPRESSION("expression"),
    STATEMENT("statement"),
    DATASET("dataset"),
    PRODUCER("producer"),
    METADATA("metadata"),
    CONTEXT("context"),
    NSMANAGER("nsmanager"),
    VISITOR("visitor")
    ;

    private static final String DATATYPE_NAMESPACE = "http://ns.inria.fr/corese/datatype/";

    final String name;

    PointerType(String n) {
        name = DATATYPE_NAMESPACE + n;
    }

}
