package fr.inria.corese.core.next.query.impl.sparql.bridge;


import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.tool.NodeImpl;
import fr.inria.corese.core.sparql.triple.cst.RDFS;
import fr.inria.corese.core.sparql.triple.parser.Constant;

/**
 * Converts AST terms ({@link TermAst}) into KGRAM {@link Node} instances.
 *
 * <p>
 * This class is part of the SPARQL → KGRAM bridge layer. It is responsible
 * for adapting high-level SPARQL AST term representations
 * ({@link VarAst}, {@link IriAst}, {@link LiteralAst})
 * into executable KGRAM {@link Node} objects used by the Corese next engine.
 * </p>
 *
 * <h3>Supported term types</h3>
 * <ul>
 *     <li>{@link VarAst} → variable node ({@link NodeImpl#createVariable(String)})</li>
 *     <li>{@link IriAst} → resource node ({@link NodeImpl#createResource(String)})</li>
 *     <li>{@link LiteralAst} → literal node backed by {@link Constant}</li>
 * </ul>
 *
 * <p>
 * This adapter is stateless and provides only static conversion methods.
 * </p>
 *
 * <h3>Literal handling (current behavior)</h3>
 * <ul>
 *     <li>If a language tag is present, a language literal is created.</li>
 *     <li>If a datatype IRI is present, a typed literal is created.</li>
 *     <li>If neither is present, the literal defaults to {@code xsd:string}.</li>
 * </ul>
 *
 */
public final class CoreseTermAdapter {

    /**
     * Private constructor to prevent instantiation.
     * This class is intended to be used statically.
     */
    private CoreseTermAdapter() {
    }


    /**
     * Converts a SPARQL AST {@link TermAst} into a KGRAM {@link Node}.
     *
     * @param term AST term (variable, IRI or literal), must not be null
     * @return corresponding KGRAM node
     * @throws IllegalArgumentException if term is null or of unknown type
     */
    public static Node toNode(TermAst term) {
        if (term == null) {
            throw new IllegalArgumentException("term is null");
        }
        return switch (term) {
            case VarAst varAst -> NodeImpl.createVariable(varAst.name());
            case IriAst iriAst -> NodeImpl.createResource(iriAst.raw());
            case LiteralAst literalAst -> toLiteralNode(literalAst);
            default -> throw new IllegalArgumentException("Unknown term type: " + term.getClass().getName());
        };
    }

    /**
     * Converts a {@link LiteralAst} into a KGRAM literal {@link Node}.
     *
     * <p>
     * The lexical form, datatype, and language tag are mapped to a
     * {@link Constant}, which is then wrapped into a {@link NodeImpl}.
     * </p>
     *
     * <ul>
     *     <li>If a language tag is present → language literal</li>
     *     <li>If a datatype is present → typed literal</li>
     *     <li>If neither → default string literal</li>
     * </ul>
     *
     * @param literalAst literal AST node
     * @return KGRAM node representing the literal
     */
    private static Node toLiteralNode(LiteralAst literalAst) {
        String lexical = literalAst.lexical();
        String datatype = literalAst.datatype();
        String lang = literalAst.lang();
        Constant constant;
        if (lang != null && !lang.isBlank()) {
            constant = Constant.create(lexical, RDFS.qxsdString, lang);
        } else if (datatype != null && !datatype.isBlank()) {
            constant = Constant.create(lexical, datatype);
        } else {
            constant = Constant.createString(lexical);
        }
        return NodeImpl.createNode(constant);
    }
}