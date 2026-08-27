package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;
import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Stateless term-building helpers extracted from {@link SparqlAstBuilder}.
 * <p>
 * This class handles every {@code term*} conversion that does <em>not</em> require
 * mutable blank-node state (anonymous blank node counter / label set stay in
 * {@link SparqlAstBuilder}).
 * <p>
 * The {@code blankNodeResolver} callback is injected at construction time so that
 * {@link #termFromGraphTerm} can delegate blank-node creation back to
 * {@link SparqlAstBuilder#termFromBlankNode} without introducing a circular
 * dependency.
 */
public final class SparqlTermBuilder {

    private final Function<SparqlParser.BlankNodeContext, TermAst> blankNodeResolver;

    /**
     * Constructs a {@code SparqlTermBuilder} with a blank-node resolver callback.
     *
     * @param options           parser options (base IRI, etc.)
     * @param blankNodeResolver callback that handles {@code BlankNodeContext} and
     *                          updates the mutable blank-node label set held by
     *                          {@link SparqlAstBuilder}
     */
    public SparqlTermBuilder(SparqlParserOptions options,
                             Function<SparqlParser.BlankNodeContext, TermAst> blankNodeResolver) {
        Objects.requireNonNull(options, "options");
        this.blankNodeResolver = blankNodeResolver;
    }

    /**
     * Convenience constructor that leaves blank-node resolution unsupported.
     * Use this only when {@link #termFromGraphTerm} will never encounter a
     * {@code blankNode()} alternative.
     */
    public SparqlTermBuilder(SparqlParserOptions options) {
        this(options, ctx -> {
            throw new UnsupportedOperationException(
                    "Blank node resolution requires a blankNodeResolver — use the two-argument constructor");
        });
    }

    /**
     * Variable token text can be "?s" or "$s" depending on grammar.
     */
    public VarAst variable(String tokenText) {
        if (tokenText == null || tokenText.isBlank()) {
            throw new IllegalArgumentException("Variable token text is null/blank");
        }
        String t = tokenText.trim();
        if (t.startsWith("?") || t.startsWith("$")) t = t.substring(1);
        return new VarAst(t);
    }

    /**
     * IRI term as raw text:
     * <ul>
     *   <li>{@code <http://...>}</li>
     *   <li>{@code foaf:Person}</li>
     *   <li>{@code a}</li>
     * </ul>
     */
    public IriAst iri(String raw) {
        if (raw == null) throw new IllegalArgumentException("IRI raw is null");
        if (raw.equals("a")) return new IriAst("<" + RDF.type.getIRI().stringValue() + ">");
        return new IriAst(raw);
    }

    /**
     * Literal term.
     *
     * @param lexical  the literal lexical form (often including quotes at this stage)
     * @param lang     language tag without '@' (e.g. "fr"), or {@code null}
     * @param datatype datatype IRI/QName text (e.g. "xsd:integer"), or {@code null}
     */
    public LiteralAst literal(String lexical, String lang, String datatype) {
        if (lexical == null) throw new IllegalArgumentException("Literal lexical is null");
        return new LiteralAst(lexical, lang, datatype);
    }

    // --- ANTLR-context converters ---

    public TermAst termFromVar(SparqlParser.Var_Context ctx) {
        return variable(ctx.getText());
    }

    public TermAst termFromIriRef(SparqlParser.IriRefContext ctx) {
        return iri(ctx.getText());
    }

    public TermAst termFromBooleanLiteral(SparqlParser.BooleanLiteralContext ctx) {
        if (ctx.FALSE() != null) {
            return new LiteralAst("false", null, XSD.xsdBoolean.getIRI().stringValue());
        } else if (ctx.TRUE() != null) {
            return new LiteralAst("true", null, XSD.xsdBoolean.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected value for boolean literal");
        }
    }

    public TermAst termFromRdfLiteral(SparqlParser.RdfLiteralContext ctx) {
        String lexical = ctx.string_().getText();
        String lang = null;
        String datatype = null;

        if (ctx.LANGTAG() != null) {
            String t = ctx.LANGTAG().getText(); // ex: "@fr"
            lang = t.startsWith("@") ? t.substring(1) : t;
        } else if (ctx.DOUBLE_CARET() != null && ctx.iriRef() != null) {
            datatype = ctx.iriRef().getText(); // ex: xsd:integer or <iri>
        }
        return literal(lexical, lang, datatype);
    }

    public TermAst termFromNumericLiteral(SparqlParser.NumericLiteralContext ctx) {
        if (ctx.numericLiteralUnsigned() != null) {
            return termFromNumericLiteralUnsigned(ctx.numericLiteralUnsigned());
        } else if (ctx.numericLiteralPositive() != null) {
            return termFromNumericLiteralPositive(ctx.numericLiteralPositive());
        } else if (ctx.numericLiteralNegative() != null) {
            return termFromNumericLiteralNegative(ctx.numericLiteralNegative());
        } else {
            throw new QueryEvaluationException("Unexpected content for numeric literal");
        }
    }

    public TermAst termFromNumericLiteralUnsigned(SparqlParser.NumericLiteralUnsignedContext ctx) {
        if (ctx.INTEGER() != null) {
            return literal(ctx.getText(), null, XSD.xsdUnsignedInt.getIRI().stringValue());
        } else if (ctx.DECIMAL() != null) {
            return literal(ctx.getText(), null, XSD.xsdDecimal.getIRI().stringValue());
        } else if (ctx.DOUBLE() != null) {
            return literal(ctx.getText(), null, XSD.xsdDouble.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected content for positive numeric literal");
        }
    }

    public TermAst termFromNumericLiteralPositive(SparqlParser.NumericLiteralPositiveContext ctx) {
        if (ctx.INTEGER_POSITIVE() != null) {
            return literal(ctx.getText(), null, XSD.xsdPositiveInteger.getIRI().stringValue());
        } else if (ctx.DECIMAL_POSITIVE() != null) {
            return literal(ctx.getText(), null, XSD.xsdDecimal.getIRI().stringValue());
        } else if (ctx.DOUBLE_POSITIVE() != null) {
            return literal(ctx.getText(), null, XSD.xsdDouble.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected content for positive numeric literal");
        }
    }

    public TermAst termFromNumericLiteralNegative(SparqlParser.NumericLiteralNegativeContext ctx) {
        if (ctx.INTEGER_NEGATIVE() != null) {
            return literal(ctx.getText(), null, XSD.xsdNegativeInteger.getIRI().stringValue());
        } else if (ctx.DECIMAL_NEGATIVE() != null) {
            return literal(ctx.getText(), null, XSD.xsdDecimal.getIRI().stringValue());
        } else if (ctx.DOUBLE_NEGATIVE() != null) {
            return literal(ctx.getText(), null, XSD.xsdDouble.getIRI().stringValue());
        } else {
            throw new QueryEvaluationException("Unexpected content for negative numeric literal");
        }
    }

    public TermAst termFromVerbSimple(SparqlParser.VerbSimpleContext ctx) {
        return termFromVar(ctx.var_());
    }

    // --- ANTLR dispatch converters (moved from SparqlAstBuilder) ---

    /**
     * Converts a {@code verb} context (predicate in a triple) to a term.
     * The {@code a} keyword is mapped to {@code rdf:type}; otherwise delegates
     * to {@link #termFromVarOrIriRef}.
     */
    public TermAst termFromVerb(SparqlParser.VerbContext ctx) {
        if (ctx.A() != null) return iri("a");
        return termFromVarOrIriRef(ctx.varOrIri());
    }

    /**
     * Converts a {@code varOrTerm} context, dispatching on variable vs. graph term.
     */
    public TermAst termFromVarOrTerm(SparqlParser.VarOrTermContext ctx) {
        if (ctx.var_() != null) return termFromVar(ctx.var_());
        return termFromGraphTerm(ctx.graphTerm());
    }

    /**
     * Converts a {@code varOrIri} context, dispatching on variable vs. IRI reference.
     */
    public TermAst termFromVarOrIriRef(SparqlParser.VarOrIriContext ctx) {
        if (ctx.var_() != null) {
            return termFromVar(ctx.var_());
        }
        return termFromIriRef(ctx.iriRef());
    }

    /**
     * Converts a {@code graphTerm} context, dispatching on IRI, RDF literal,
     * numeric literal, boolean literal, blank node, or NIL.
     * <p>
     * Blank-node creation is delegated to the {@code blankNodeResolver} supplied
     * at construction time so that the mutable label set in
     * {@link SparqlAstBuilder} is properly updated.
     */
    public TermAst termFromGraphTerm(SparqlParser.GraphTermContext ctx) {
        if (ctx.iriRef() != null) {
            return termFromIriRef(ctx.iriRef());
        }
        if (ctx.rdfLiteral() != null) {
            return termFromRdfLiteral(ctx.rdfLiteral());
        }
        if (ctx.numericLiteral() != null) {
            return termFromNumericLiteral(ctx.numericLiteral());
        }
        if (ctx.booleanLiteral() != null) {
            return termFromBooleanLiteral(ctx.booleanLiteral());
        }
        if (ctx.blankNode() != null) {
            return blankNodeResolver.apply(ctx.blankNode());
        }
        if (ctx.NIL() != null) {
            return iri("()");
        } // NIL = () in SPARQL
        return iri(ctx.getText());
    }

    /**
     * Converts an {@code objectList} context to a list of terms by iterating
     * each {@code object_} child and delegating to the provided object converter.
     *
     * @param ctx             the objectList parse context
     * @param objectConverter function converting a single {@code object_} to a term
     */
    public List<TermAst> termListFromObjectList(SparqlParser.ObjectListContext ctx,
                                                Function<SparqlParser.Object_Context, TermAst> objectConverter) {
        List<TermAst> out = new ArrayList<>();
        for (var obj : ctx.object_()) {
            out.add(objectConverter.apply(obj));
        }
        return out;
    }
}
