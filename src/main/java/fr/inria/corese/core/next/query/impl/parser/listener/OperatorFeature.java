package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

/**
 * Creates any SPARQL operator including:
 * * &&
 * * ||
 * * !
 * * bound,
 * * isIri and isUri
 * * isBlank
 * * isLiteral
 * * str
 * * lang
 * * datatype
 * * =
 * * !=
 * * <
 * * <=
 * * >
 * * >=
 * * /
 * * +
 * * -
 * * sameTerm
 * * langMatches
 * * regex
 */
public class OperatorFeature extends AbstractSparqlFeature {
    protected OperatorFeature(SparqlAstBuilder builder) {
        super(builder);
    }
}
