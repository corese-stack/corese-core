package fr.inria.corese.core.next.query.kgram.api.query;

/**
 * Olivier Corby - Wimmics INRIA I3S - 2020
 */
public interface AST {

    boolean isSelect();

    boolean isConstruct();

    boolean isUpdate();

    boolean isInsert();

    boolean isDelete();

    boolean hasMetadata(String name);
}
