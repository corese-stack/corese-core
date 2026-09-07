package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.spi.io.IOConstants;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.kgram.api.core.ExprType;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Filter;
import fr.inria.corese.core.next.common.text.RdfText;
import fr.inria.corese.core.sparql.datatype.RDF;
import fr.inria.corese.core.next.data.spi.term.IRIUtils;
import fr.inria.corese.core.sparql.triple.cst.Keyword;
import fr.inria.corese.core.sparql.triple.parser.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Converts Corese-next {@link TermAst} nodes (including {@link ConstraintAst}) into
 * {@link Expression} trees for the SPARQL interpreter, consumable from KGRAM “next” via
 * {@link fr.inria.corese.core.next.query.impl.kgram.api.core.Filter} / {@link AstBackedExpr}.
 *
 */
public final class SparqlAstToExpression {

    private static final ThreadLocal<QueryPrologueAst> CURRENT_PROLOGUE = new ThreadLocal<>();

    public static QueryPrologueAst getCurrentPrologue() {
        return CURRENT_PROLOGUE.get();
    }

    public static void setCurrentPrologue(QueryPrologueAst prologue) {
        if (prologue == null) {
            CURRENT_PROLOGUE.remove();
        } else {
            CURRENT_PROLOGUE.set(prologue);
        }
    }

    private SparqlAstToExpression() {
    }

    /**
     * Converts any {@link TermAst} (variable, literal, IRI, or constraint expression) to {@link Expression}.
     */
    public static Expression convert(TermAst term) {
        return switch (term) {
            case VarAst(String name) -> Variable.create(name);
            case LiteralAst(String lexical, String lang, String datatype) ->
                    literalToConstant(lexical, lang, datatype);
            case IriAst(String raw) -> iriToConstant(raw);
            case ConstraintAst c -> constraintToExpression(c);
            default -> throw new IllegalStateException("Unhandled TermAst: " + term.getClass());
        };
    }

    /**
     * Converts a {@code FILTER} clause, compiling the graph pattern of any embedded
     * {@code EXISTS} / {@code NOT EXISTS} with the given {@link WhereCompiler}.
     */
    public static Filter toNextFilter(FilterAst filterClause, WhereCompiler whereCompiler) {
        Objects.requireNonNull(filterClause, "filterClause");
        return toNextFilter(filterClause.operator(), whereCompiler);
    }

    /**
     * Converts a filter {@link TermAst} to an {@link Expression}, then wraps it as a
     * {@link Filter} with {@link Filter#coreseNextSource()} set to {@code filterExpression}.
     * Filters containing {@code EXISTS} / {@code NOT EXISTS} require
     * {@link #toNextFilter(TermAst, WhereCompiler)} so their graph pattern can be compiled.
     */
    public static Filter toNextFilter(TermAst filterExpression) {
        return toNextFilter(filterExpression, null);
    }

    /**
     * Converts a filter {@link TermAst}, then compiles the graph pattern of every embedded
     * {@code EXISTS} / {@code NOT EXISTS} with the given {@link WhereCompiler}.
     *
     */
    public static Filter toNextFilter(TermAst filterExpression, WhereCompiler whereCompiler) {
        Expression exprTree = convert(filterExpression);
        compileExists(exprTree, whereCompiler);
        initializeExpList(exprTree);
        AstBackedExpr expr = new AstBackedExpr(exprTree, Optional.of(filterExpression));
        return expr.getFilter();
    }

    /**
     * Second pass: compiles the graph pattern carried by every {@link AstBackedExistTerm} of the
     * expression tree. Mirrors {@code compileExist} of the historical pipeline.
     */
    private static void compileExists(Expression expression, WhereCompiler whereCompiler) {
        if (expression instanceof AstBackedExistTerm exist) {
            if (whereCompiler == null) {
                throw new IllegalArgumentException(
                        "EXISTS / NOT EXISTS filter conversion requires a WhereCompiler "
                                + "to compile its graph pattern");
            }
            exist.setCompiledPattern(whereCompiler.compile(exist.patternAst()));
            return;
        }
        if (expression instanceof Term term) {
            for (Expression arg : term.getArgs()) {
                compileExists(arg, whereCompiler);
            }
        }
    }

    /**
     * Initializes the {@code Expr} list ({@code lExp}) of every {@link Term} in the tree.
     */
    private static void initializeExpList(Expression expression) {
        if (expression instanceof Term term) {
            for (Expression arg : term.getArgs()) {
                initializeExpList(arg);
            }
            term.setExpList(new ArrayList<>(term.getArgs()));
        }
    }

    private static Constant literalToConstant(String lexical, String lang, String datatype) {
        if (lang != null && !lang.isEmpty()) {
            return Constant.create(unquoteLexical(lexical), RDF.rdflangString, lang);
        }
        if (datatype != null && !datatype.isEmpty()) {
            return Constant.create(unquoteLexical(lexical), normalizeDatatypeIri(datatype), null);
        }
        return Constant.createString(unquoteLexical(lexical));
    }

    public static String unquoteLexical(String lexical) {
        if (lexical == null || lexical.length() < 2) {
            return lexical;
        }
        String unquotedTriple = stripTripleQuotes(lexical);
        if (unquotedTriple != null) {
            return unquotedTriple;
        }
        return stripSingleQuotes(lexical);
    }

    private static String stripTripleQuotes(String lexical) {
        if (lexical.length() >= 6) {
            if (lexical.startsWith("\"\"\"") && lexical.endsWith("\"\"\"")) {
                return lexical.substring(3, lexical.length() - 3);
            }
            if (lexical.startsWith("'''") && lexical.endsWith("'''")) {
                return lexical.substring(3, lexical.length() - 3);
            }
        }
        return null;
    }

    private static String stripSingleQuotes(String lexical) {
        char quote = lexical.charAt(0);
        if (quote != '"' && quote != '\'') {
            return lexical;
        }
        int endIdx = lexical.endsWith(String.valueOf(quote))
                ? lexical.length() - 1
                : lexical.lastIndexOf(quote);
        if (endIdx > 0) {
            return lexical.substring(1, endIdx);
        }
        return lexical;
    }

    /**
     * Normalizes a datatype IRI string from a literal AST term.
     * <p>If enclosed in angle brackets (&lt;...&gt;), it is treated as an explicit IRI_REF and resolved against BASE.
     * If it already contains a URI scheme delimiter (://), it is preserved as an absolute URI.
     * Otherwise, it is treated as a prefixed name (e.g. {@code xsd:integer}) and expanded.</p>
     */
    private static String normalizeDatatypeIri(String dt) {
        if (dt == null) {
            return null;
        }
        if (dt.startsWith("<") && dt.endsWith(">")) {
            String inner = RdfText.stripAngleBrackets(dt);
            return resolveRelativeIri(inner, CURRENT_PROLOGUE.get());
        }
        if (dt.contains("://")) {
            return dt;
        }
        return resolvePrefixedIri(dt, CURRENT_PROLOGUE.get());
    }

    /**
     * Converts a raw IRI/PName token string into a runtime {@link Constant}.
     * <p>Blank nodes are instantiated as blank constants; IRIs and prefixed names are resolved via
     * the query prologue and instantiated as resource constants.</p>
     */
    private static Constant iriToConstant(String rawIri) {
        if (rawIri == null) {
            return null;
        }
        if (rawIri.startsWith(IOConstants.BLANK_NODE_PREFIX)) {
            return Constant.createBlank(rawIri.substring(IOConstants.BLANK_NODE_PREFIX.length()));
        }
        String resolved = resolveIri(rawIri, CURRENT_PROLOGUE.get());
        return Constant.createResource(resolved, resolved);
    }

    /**
     * Resolves a raw SPARQL term against the query prologue.
     * <ul>
     *   <li>&lt;...&gt; explicit IRI references are resolved against {@code BASE} if relative.</li>
     *   <li>Blank node labels (_:...) are preserved as is.</li>
     *   <li>SPARQL keyword {@code a} resolves to {@code rdf:type}.</li>
     *   <li>Prefixed names (prefix:local) are expanded using declared prefixes or Corese default namespaces.</li>
     * </ul>
     */
    public static String resolveIri(String raw, QueryPrologueAst prologue) {
        if (raw == null) {
            return null;
        }
        if (raw.startsWith("<") && raw.endsWith(">")) {
            String inner = RdfText.stripAngleBrackets(raw);
            return resolveRelativeIri(inner, prologue);
        }
        if (raw.startsWith(IOConstants.BLANK_NODE_PREFIX)) {
            return raw;
        }
        if (raw.equals("a")) {
            return RDF.RDF + "type";
        }
        return resolvePrefixedIri(raw, prologue);
    }

    /**
     * Expands a prefixed name (e.g. {@code ex:item}, {@code :item}) using the query prologue declarations,
     * falling back to default namespaces from {@link NSManager}.
     */
    private static String resolvePrefixedIri(String raw, QueryPrologueAst prologue) {
        int colon = raw.indexOf(':');
        if (colon >= 0) {
            String prefix = raw.substring(0, colon);
            String local = raw.substring(colon + 1);
            String ns = findNamespace(prefix, prologue);
            if (ns != null) {
                return ns + unescapePName(local);
            }
        }
        return raw;
    }

    /**
     * Finds the namespace associated with a prefix in the query prologue, or in the global {@link NSManager}.
     */
    private static String findNamespace(String prefix, QueryPrologueAst prologue) {
        if (prologue != null && prologue.prefixDeclarations() != null) {
            for (PrefixDeclarationAst decl : prologue.prefixDeclarations()) {
                String p = decl.prefix();
                if (p != null && p.endsWith(":")) {
                    p = p.substring(0, p.length() - 1);
                }
                if (Objects.equals(p, prefix)) {
                    return RdfText.stripAngleBrackets(decl.namespace().raw());
                }
            }
        }
        return NSManager.nsm().getNamespace(prefix);
    }

    /**
     * Resolves a relative IRI against the prologue {@code BASE} URI if present and absolute.
     */
    private static String resolveRelativeIri(String clean, QueryPrologueAst prologue) {
        if (prologue != null && prologue.baseIri() != null) {
            String base = RdfText.stripAngleBrackets(prologue.baseIri().raw());
            if (IRIUtils.isAbsoluteIRI(base) && !IRIUtils.isAbsoluteIRI(clean)) {
                return IRIUtils.resolveIRIAgainstBase(base, clean);
            }
        }
        return clean;
    }

    /**
     * Unescapes SPARQL PN_LOCAL_ESC sequences (e.g. \: -&gt; :, \. -&gt; .) and Unicode escapes (\\uXXXX).
     */
    private static String unescapePName(String local) {
        if (local == null || !local.contains("\\")) {
            return local;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = local.length();
        while (i < len) {
            char c = local.charAt(i);
            if (c == '\\' && i + 1 < len) {
                i = appendEscaped(sb, local, i + 1);
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static int appendEscaped(StringBuilder sb, String str, int nextIdx) {
        char next = str.charAt(nextIdx);
        if (next == 'u' || next == 'U') {
            int hexLen = (next == 'u') ? 4 : 8;
            if (nextIdx + 1 + hexLen <= str.length()) {
                String hex = str.substring(nextIdx + 1, nextIdx + 1 + hexLen);
                int codePoint = Integer.parseInt(hex, 16);
                sb.appendCodePoint(codePoint);
                return nextIdx + 1 + hexLen;
            }
        }
        sb.append(next);
        return nextIdx + 1;
    }

    private static Expression constraintToExpression(ConstraintAst constraint) {
        Expression expr = operatorToExpression(constraint);
        if (expr != null) {
            return expr;
        }
        expr = stringAndHashFunctionToExpression(constraint);
        if (expr != null) {
            return expr;
        }
        expr = builtinToExpression(constraint);
        if (expr != null) {
            return expr;
        }
        throw new UnsupportedQueryFeatureException(
                "Filter expression is not supported yet by the next pipeline: "
                        + constraint.getClass().getSimpleName());
    }

    private static Expression operatorToExpression(ConstraintAst constraint) {
        return switch (constraint) {
            case AndAst andAst ->
                    Term.create(KeywordHolder.SEAND,
                            convert(andAst.getLeftArgument()), convert(andAst.getRightArgument()));
            case OrAst orAst ->
                    Term.create(KeywordHolder.SEOR,
                            convert(orAst.getLeftArgument()), convert(orAst.getRightArgument()));
            case EqualsAst equalsAst ->
                    Term.create("=", convert(equalsAst.getLeftArgument()), convert(equalsAst.getRightArgument()));
            case DifferentAst differentAst ->
                    Term.create("!=", convert(differentAst.getLeftArgument()), convert(differentAst.getRightArgument()));
            case LowerThanAst lowerThanAst ->
                    Term.create("<", convert(lowerThanAst.getLeftArgument()), convert(lowerThanAst.getRightArgument()));
            case LowerOrEqualThanAst lowerOrEqualThanAst ->
                    Term.create("<=",
                            convert(lowerOrEqualThanAst.getLeftArgument()),
                            convert(lowerOrEqualThanAst.getRightArgument()));
            case GreaterThanAst greaterThanAst ->
                    Term.create(">",
                            convert(greaterThanAst.getLeftArgument()),
                            convert(greaterThanAst.getRightArgument()));
            case GreaterOrEqualThanAst greaterOrEqualThanAst ->
                    Term.create(">=",
                            convert(greaterOrEqualThanAst.getLeftArgument()),
                            convert(greaterOrEqualThanAst.getRightArgument()));
            case AddAst addAst ->
                    Term.create("+", convert(addAst.getLeftArgument()), convert(addAst.getRightArgument()));
            case SubtractAst subtractAst ->
                    Term.create("-", convert(subtractAst.getLeftArgument()), convert(subtractAst.getRightArgument()));
            case MultiplyAst multiplyAst ->
                    Term.create("*", convert(multiplyAst.getLeftArgument()), convert(multiplyAst.getRightArgument()));
            case DivideAst divideAst ->
                    Term.create("/", convert(divideAst.getLeftArgument()), convert(divideAst.getRightArgument()));
            case UnaryPlusAst unaryPlusAst ->
                    Term.create("+", convert(unaryPlusAst.argument()));
            case UnaryMinusAst unaryMinusAst ->
                    Term.create("-", convert(unaryMinusAst.argument()));
            case BooleanNotAst booleanNotAst ->
                    notTerm(convert(booleanNotAst.argument()));
            default -> null;
        };
    }

    private static Expression stringAndHashFunctionToExpression(ConstraintAst constraint) {
        return switch (constraint) {
            case StrStartsAst strStartsAst ->
                    functionTerm("strstarts",
                            convert(strStartsAst.getLeftArgument()),
                            convert(strStartsAst.getRightArgument()));
            case StrEndsAst strEndsAst ->
                    functionTerm("strends",
                            convert(strEndsAst.getLeftArgument()),
                            convert(strEndsAst.getRightArgument()));
            case ContainsAst containsAst ->
                    functionTerm("contains",
                            convert(containsAst.getLeftArgument()),
                            convert(containsAst.getRightArgument()));
            case StrBeforeAst strBeforeAst ->
                    functionTerm("strbefore",
                            convert(strBeforeAst.getLeftArgument()),
                            convert(strBeforeAst.getRightArgument()));
            case StrAfterAst strAfterAst ->
                    functionTerm("strafter",
                            convert(strAfterAst.getLeftArgument()),
                            convert(strAfterAst.getRightArgument()));
            case StrLangAst strLangAst ->
                    functionTerm("strlang",
                            convert(strLangAst.getLeftArgument()),
                            convert(strLangAst.getRightArgument()));
            case StrDtAst strDtAst ->
                    functionTerm(Processor.STRDT,
                            convert(strDtAst.getLeftArgument()),
                            convert(strDtAst.getRightArgument()));
            case IriFunctionAst iriFunctionAst ->
                    functionTerm("iri", convert(iriFunctionAst.argument()));
            case LcaseAst lcaseAst ->
                    functionTerm("lcase", convert(lcaseAst.argument()));
            case UcaseAst ucaseAst ->
                    functionTerm("ucase", convert(ucaseAst.argument()));
            case EncodeForUriAst encodeForUriAst ->
                    functionTerm("encode_for_uri", convert(encodeForUriAst.argument()));
            case Md5Ast md5Ast ->
                    functionTerm("md5", convert(md5Ast.argument()));
            case Sha1Ast sha1Ast ->
                    functionTerm("sha1", convert(sha1Ast.argument()));
            case Sha256Ast sha256Ast ->
                    functionTerm("sha256", convert(sha256Ast.argument()));
            case Sha384Ast sha384Ast ->
                    functionTerm("sha384", convert(sha384Ast.argument()));
            case Sha512Ast sha512Ast ->
                    functionTerm("sha512", convert(sha512Ast.argument()));
            case ReplaceAst replaceAst ->
                    replaceToTerm(replaceAst);
            case SubstrAst substrAst ->
                    substrToTerm(substrAst);
            case ConcatAst concatAst ->
                    variadicTerm("concat", concatAst.arguments());
            case BinaryRegexAst binaryRegexAst ->
                    regexTerm(convert(binaryRegexAst.getString()), convert(binaryRegexAst.getPattern()));
            case TrinaryRegexAst trinaryRegexAst ->
                    regexTerm(convert(trinaryRegexAst.getString()),
                            convert(trinaryRegexAst.getPattern()),
                            convert(trinaryRegexAst.getFlags()));
            case BnodeAst bnodeAst ->
                    bnodeToTerm(bnodeAst);
            default -> null;
        };
    }

    private static Expression builtinToExpression(ConstraintAst constraint) {
        return switch (constraint) {
            case BoundAst boundAst ->
                    functionTerm(Processor.BOUND, convert(boundAst.argument()));
            case IsIriAst isIriAst ->
                    functionTerm("isIRI", convert(isIriAst.argument()));
            case IsBlankAst isBlankAst ->
                    functionTerm("isBlank", convert(isBlankAst.argument()));
            case IsLiteralAst isLiteralAst ->
                    functionTerm("isLiteral", convert(isLiteralAst.argument()));
            case StrAst strAst ->
                    functionTerm("str", convert(strAst.argument()));
            case LangAst langAst ->
                    functionTerm("lang", convert(langAst.argument()));
            case DatatypeAst datatypeAst ->
                    functionTerm("datatype", convert(datatypeAst.argument()));
            case SameTermAst sameTermAst ->
                    functionTerm("sameTerm",
                            convert(sameTermAst.getLeftArgument()),
                            convert(sameTermAst.getRightArgument()));
            case LangMatchesAst langMatchesAst ->
                    functionTerm("langMatches",
                            convert(langMatchesAst.getLeftArgument()),
                            convert(langMatchesAst.getRightArgument()));
            case FunctionCallAst(TermAst functionName, List<TermAst> arguments) ->
                    functionCallAst(functionName, arguments);
            case CoalesceAst coalesceAst ->
                    variadicTerm(Processor.COALESCE, coalesceAst.arguments());
            case IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr) ->
                    Term.function(Processor.IF, convert(condition), convert(thenExpr), convert(elseExpr));
            case ExistsAst(GroupGraphPatternAst pattern) ->
                    new AstBackedExistTerm(pattern);
            case NotExistsAst(GroupGraphPatternAst pattern) ->
                    notTerm(new AstBackedExistTerm(pattern));
            default -> null;
        };
    }

    /**
     * Builds a boolean negation with the runtime operator code already set.
     *
     * <p>The interpreter resolves operator codes in a later compilation phase
     * ({@code Processor.type(Term, ASTQuery)}) that this bridge never runs, so {@code oper()} is set
     * explicitly here. This keeps {@code !EXISTS { ... }} and {@code NOT EXISTS { ... }}
     * equivalent to KGRAM.</p>
     */
    private static Term notTerm(Expression expression) {
        Term not = Term.create("!", expression);
        not.setOper(ExprType.NOT);
        return not;
    }

    private static Term functionTerm(String name, Expression arg) {
        Term t = Term.function(name);
        t.add(arg);
        return t;
    }

    private static Term functionTerm(String name, Expression a1, Expression a2) {
        Term t = Term.function(name);
        t.add(a1);
        t.add(a2);
        return t;
    }

    private static Term regexTerm(Expression s, Expression pattern) {
        Term t = Term.function("regex");
        t.add(s);
        t.add(pattern);
        return t;
    }

    private static Term regexTerm(Expression s, Expression pattern, Expression flags) {
        Term t = Term.function("regex");
        t.add(s);
        t.add(pattern);
        t.add(flags);
        return t;
    }

    private static Term functionCallAst(TermAst functionName, List<TermAst> arguments) {
        String name = SparqlBuiltinFunctionNameResolver.fromFunctionTerm(functionName);
        Term t = Term.function(name);
        for (TermAst arg : arguments) {
            t.add(convert(arg));
        }
        return t;
    }

    private static Term variadicTerm(String name, List<TermAst> args) {
        Term t = Term.function(name);
        for (TermAst arg : args) {
            t.add(convert(arg));
        }
        return t;
    }

    private static Term replaceToTerm(ReplaceAst r) {
        Term t =
                Term.function(
                        "replace",
                        convert(r.getString()),
                        convert(r.getPattern()),
                        convert(r.getReplacement()));
        if (r.hasFlags()) {
            t.add(convert(r.getFlags()));
        }
        return t;
    }

    private static Term substrToTerm(SubstrAst s) {
        if (s.getLength() != null) {
            return Term.function(
                    "substr",
                    convert(s.getString()),
                    convert(s.getStart()),
                    convert(s.getLength()));
        }
        return Term.function("substr", convert(s.getString()), convert(s.getStart()));
    }

    private static Term bnodeToTerm(BnodeAst b) {
        if (b.getLabel() == null) {
            return Term.function(Processor.BNODE);
        }
        return Term.function(Processor.BNODE, convert(b.getLabel()));
    }

    private static final class KeywordHolder {
        static final String SEAND = Keyword.SEAND;
        static final String SEOR = Keyword.SEOR;
    }
}
