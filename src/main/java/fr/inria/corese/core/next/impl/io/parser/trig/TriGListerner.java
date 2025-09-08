package fr.inria.corese.core.next.impl.io.parser.trig;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.parser.RDFParserBaseIRIOptions;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.parser.antlr.TriGBaseListener;
import fr.inria.corese.core.next.impl.parser.antlr.TriGParser;
import fr.inria.corese.core.next.api.ValueFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.net.URISyntaxException;
/**
 * Listener for the ANTLR4 generated parser for TriG.
 * This listener traverses the parse tree and builds the RDF model,
 * supporting named graphs. It includes unescaping logic for URIs and literals.
 */
public class TriGListerner extends TriGBaseListener {
    private final Model model;
    private String baseURI;
    private final Map<String, String> prefixMap = new HashMap<>();
    private final ValueFactory factory;

    private Resource currentSubject;
    private IRI currentPredicate;
    private Resource currentGraph;


    /**
     * Constructor for the TriGListerner.
     *
     * @param model   The RDF model to populate.
     * @param factory The ValueFactory for creating RDF resources.
     * @param options IOOptions for configuration (if any).
     */
    public TriGListerner(Model model, ValueFactory factory, IOOptions options) {
        this.model = model;
        this.factory = factory;

        if (options instanceof RDFParserBaseIRIOptions) {
            RDFParserBaseIRIOptions baseIRIOptions = (RDFParserBaseIRIOptions) options;
            this.baseURI = baseIRIOptions.getBase() != null ? baseIRIOptions.getBase() : "";
        } else {
            this.baseURI = "";
        }
    }

    @Override
    public void exitBase(TriGParser.BaseContext ctx) {
        String newBase = ctx.IRIREF().getText();
        newBase = newBase.substring(1, newBase.length() - 1);

        try {
            URI currentBaseUri = new URI(this.baseURI);
            URI resolvedBaseUri = currentBaseUri.resolve(newBase);
            this.baseURI = resolvedBaseUri.toString();
        } catch (URISyntaxException e) {
            this.baseURI = newBase;
        }

    }

    @Override
    public void exitPrefixID(TriGParser.PrefixIDContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        String iri = ctx.IRIREF().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        iri = iri.substring(1, iri.length() - 1);

        String resolvedIRI;
        try {
            URI base = new URI(baseURI);
            URI resolved = base.resolve(iri);
            resolvedIRI = resolved.toString();
        } catch (URISyntaxException e) {
            resolvedIRI = baseURI + iri;
        }
        prefixMap.put(prefix, resolvedIRI);
        model.setNamespace(prefix, resolvedIRI);
    }

    @Override
    public void exitSparqlBase(TriGParser.SparqlBaseContext ctx) {
        String newBase = ctx.IRIREF().getText();
        newBase = newBase.substring(1, newBase.length() - 1);
        baseURI = newBase;
    }

    // Add this new method to handle SPARQL-style PREFIX declarations
    @Override
    public void exitSparqlPrefix(TriGParser.SparqlPrefixContext ctx) {
        String prefix = ctx.PNAME_NS().getText();
        String iri = ctx.IRIREF().getText();
        prefix = prefix.substring(0, prefix.length() - 1);
        iri = iri.substring(1, iri.length() - 1);

        String resolvedIRI;
        try {
            URI base = new URI(baseURI);
            URI resolved = base.resolve(iri);
            resolvedIRI = resolved.toString();
        } catch (URISyntaxException e) {
            resolvedIRI = baseURI + iri;
        }
        prefixMap.put(prefix, resolvedIRI);
        model.setNamespace(prefix, resolvedIRI);
    }

    @Override
    public void enterBlock(TriGParser.BlockContext ctx) {
        currentGraph = ctx.Graph_w() != null && ctx.labelOrSubject() != null
                ? extractLabelOrSubject(ctx.labelOrSubject())
                : null;
    }

    @Override
    public void exitBlock(TriGParser.BlockContext ctx) {
        currentGraph = null;
    }

    @Override
    public void enterTriplesOrGraph(TriGParser.TriplesOrGraphContext ctx) {
        if (ctx.labelOrSubject() != null && ctx.predicateObjectList() != null) {
            currentSubject = extractLabelOrSubject(ctx.labelOrSubject());
            processPredicateObjectList(ctx.predicateObjectList());
        } else if (ctx.labelOrSubject() != null && ctx.predicateObjectList() == null) {
            Resource potentialGraph = extractLabelOrSubject(ctx.labelOrSubject());
            currentGraph = potentialGraph;
        }
    }

    @Override
    public void enterTriples(TriGParser.TriplesContext ctx) {
        if (ctx.subject() != null) {
            currentSubject = extractSubject(ctx.subject());
        } else if (ctx.blankNodePropertyList() != null) {
            currentSubject = processBlankNodePropertyList(ctx.blankNodePropertyList());
        }

        if (ctx.predicateObjectList() != null) {
            processPredicateObjectList(ctx.predicateObjectList());
        }
    }

    @Override
    public void enterTriples2(TriGParser.Triples2Context ctx) {
        if (ctx.blankNodePropertyList() != null) {
            currentSubject = processBlankNodePropertyList(ctx.blankNodePropertyList());

            if (ctx.predicateObjectList() != null) {
                processPredicateObjectList(ctx.predicateObjectList());
            }
        } else if (ctx.collection() != null) {
            currentSubject = processCollection(ctx.collection());

            if (ctx.predicateObjectList() != null) {
                processPredicateObjectList(ctx.predicateObjectList());
            }
        }
    }

    private void processPredicateObjectList(TriGParser.PredicateObjectListContext ctx) {
        List<TriGParser.VerbContext> verbs = ctx.verb();
        List<TriGParser.ObjectListContext> objLists = ctx.objectList();

        for (int i = 0; i < verbs.size(); i++) {
            currentPredicate = extractVerb(verbs.get(i));
            List<TriGParser.ObjectContext> objects = objLists.get(i).object();
            for (TriGParser.ObjectContext objCtx : objects) {
                Value object = extractObject(objCtx);
                model.add(currentSubject, currentPredicate, object, currentGraph);
            }
        }
    }

    private Value extractObject(TriGParser.ObjectContext ctx) {
        if (ctx.iri() != null) return factory.createIRI(resolveIRI(ctx.iri().getText()));
        if (ctx.blank() != null) return extractBlank(ctx.blank());
        if (ctx.literal() != null) return extractLiteral(ctx.literal());
        if (ctx.blankNodePropertyList() != null) return processBlankNodePropertyList(ctx.blankNodePropertyList());
        throw new RuntimeException("Unsupported object: " + ctx.getText());
    }

    private Resource processBlankNodePropertyList(TriGParser.BlankNodePropertyListContext ctx) {
        Resource bnode = factory.createBNode();
        Resource savedSubject = currentSubject;
        currentSubject = bnode;

        if (ctx.predicateObjectList() != null) {
            processPredicateObjectList(ctx.predicateObjectList());
        }

        currentSubject = savedSubject;
        return bnode;
    }

    private Resource extractSubject(TriGParser.SubjectContext ctx) {
        if (ctx.iri() != null) {
            return factory.createIRI(resolveIRI(ctx.iri().getText()));
        }
        if (ctx.blank() != null) {
            TriGParser.BlankNodeContext node = ctx.blank().blankNode();
            if (node != null) {
                if (node.BLANK_NODE_LABEL() != null) {
                    return factory.createBNode(node.BLANK_NODE_LABEL().getText().substring(2));
                }
                if (node.ANON() != null) {
                    return factory.createBNode();
                }
            } else if (ctx.blank().collection() != null) {
                return processCollection(ctx.blank().collection());
            }
        }
        throw new RuntimeException("Unsupported subject: " + ctx.getText());
    }

    private Resource extractBlank(TriGParser.BlankContext ctx) {
        TriGParser.BlankNodeContext node = ctx.blankNode();
        if (node != null) {
            if (node.BLANK_NODE_LABEL() != null)
                return factory.createBNode(node.BLANK_NODE_LABEL().getText().substring(2));
            if (node.ANON() != null)
                return factory.createBNode();
        }

        TriGParser.CollectionContext collection = ctx.collection();
        if (collection != null) {
            return processCollection(collection);
        }

        throw new RuntimeException("Unsupported blank node structure: " + ctx.getText());
    }

    private Resource processCollection(TriGParser.CollectionContext ctx) {
        List<TriGParser.ObjectContext> objects = ctx.object();

        if (objects.isEmpty()) {
            return factory.createIRI(RDF.nil.getIRI().stringValue());
        }

        Resource head = factory.createBNode();
        Resource current = head;

        for (int i = 0; i < objects.size(); i++) {
            Value object = extractObject(objects.get(i));

            model.add(current, factory.createIRI(RDF.first.getIRI().stringValue()), object, currentGraph);

            if (i == objects.size() - 1) {
                model.add(current, factory.createIRI(RDF.rest.getIRI().stringValue()),
                        factory.createIRI(RDF.nil.getIRI().stringValue()), currentGraph);
            } else {
                Resource next = factory.createBNode();
                model.add(current, factory.createIRI(RDF.rest.getIRI().stringValue()), next, currentGraph);
                current = next;
            }
        }

        return head;
    }

    private Resource extractLabelOrSubject(TriGParser.LabelOrSubjectContext ctx) {
        if (ctx.iri() != null) {
            return factory.createIRI(resolveIRI(ctx.iri().getText()));
        }
        if (ctx.blankNode() != null) {
            if (ctx.blankNode().BLANK_NODE_LABEL() != null) {
                return factory.createBNode(ctx.blankNode().BLANK_NODE_LABEL().getText().substring(2));
            }
            if (ctx.blankNode().ANON() != null) {
                return factory.createBNode();
            }
        }

        throw new RuntimeException("Unsupported labelOrSubject: " + ctx.getText());
    }

    private IRI extractVerb(TriGParser.VerbContext ctx) {
        return factory.createIRI(resolveIRI(ctx.getText()));
    }

    private Literal extractLiteral(TriGParser.LiteralContext ctx) {
        if (ctx.rDFLiteral() != null) {
            String label = unescapeString(ctx.rDFLiteral().string().getText());
            if (ctx.rDFLiteral().LANGTAG() != null)
                return factory.createLiteral(label, ctx.rDFLiteral().LANGTAG().getText().substring(1));
            if (ctx.rDFLiteral().iri() != null)
                return factory.createLiteral(label, factory.createIRI(resolveIRI(ctx.rDFLiteral().iri().getText())));
            return factory.createLiteral(label);
        }
        if (ctx.BooleanLiteral() != null)
            return factory.createLiteral(ctx.BooleanLiteral().getText(), XSD.BOOLEAN.getIRI());
        if (ctx.numericLiteral() != null) {
            if (ctx.numericLiteral().INTEGER() != null)
                return factory.createLiteral(ctx.numericLiteral().INTEGER().getText(), XSD.INTEGER.getIRI());
            if (ctx.numericLiteral().DECIMAL() != null)
                return factory.createLiteral(ctx.numericLiteral().DECIMAL().getText(), XSD.DECIMAL.getIRI());
            if (ctx.numericLiteral().DOUBLE() != null)
                return factory.createLiteral(ctx.numericLiteral().DOUBLE().getText(), XSD.DOUBLE.getIRI());
        }
        throw new RuntimeException("Unsupported literal: " + ctx.getText());
    }

    private String resolveIRI(String raw) {
        raw = raw.trim();

        if (raw.startsWith("<") && raw.endsWith(">")) {
            String iri = raw.substring(1, raw.length() - 1);
            iri = unescapeIRI(iri);
            if (isAbsoluteIRI(iri)) {
                return iri;
            }
            return resolveRelativeIRI(iri);
        }

        if (raw.equals("a")) {
            return RDF.type.getIRI().stringValue();
        } else if (raw.contains(":")) {
            String[] parts = raw.split(":", 2);
            String prefix = parts[0];
            String localName = parts[1];

            localName = unescapeIRI(localName);

            if (prefix.isEmpty()) {
                String defaultNS = prefixMap.get("");
                if (defaultNS != null) {
                    return defaultNS + localName;
                } else {
                    return resolveRelativeIRI(localName);
                }
            }

            String ns = prefixMap.get(prefix);
            if (ns != null) {
                return ns + localName;
            }
            throw new IllegalArgumentException("Undeclared prefix: " + prefix);
        }

        raw = unescapeIRI(raw);
        return resolveRelativeIRI(raw);
    }

    private boolean isAbsoluteIRI(String iri) {
        return iri.contains(":") && !iri.startsWith(":");
    }

    private String resolveRelativeIRI(String relativeIRI) {
        try {

            URI base = (baseURI != null && !baseURI.isEmpty())
                    ? new URI(baseURI)
                    : new URI("http://example.org/");


            URI resolved = base.resolve(relativeIRI);

            return resolved.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("URI syntax error during resolution: Base: " + baseURI + ", Relative: " + relativeIRI, e);
        }
    }


    private String unescapeIRI(String rawIri) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawIri.length(); i++) {
            char c = rawIri.charAt(i);
            if (c == '\\') {
                if (i + 1 < rawIri.length()) {
                    char next = rawIri.charAt(i + 1);
                    if (next == 'u' || next == 'U') {
                        int len = (next == 'u') ? 4 : 8;
                        if (i + len + 1 <= rawIri.length()) {
                            try {
                                String hex = rawIri.substring(i + 2, i + 2 + len);
                                int codePoint = Integer.parseInt(hex, 16);

                                if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                                    throw new IllegalArgumentException("Surrogates not allowed in IRIREF: \\u" + hex);
                                }

                                sb.appendCodePoint(codePoint);
                                i += len + 1;
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Invalid hexadecimal value in IRI escape.", e);
                            }
                        } else {
                            throw new IllegalArgumentException("Incomplete Unicode escape in IRI.");
                        }
                    } else {
                        sb.append(next);
                        i++;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String unescapeString(String text) {
        if (text == null || text.length() < 2) {
            return text;
        }

        boolean isMultiline = text.startsWith("\"\"\"") || text.startsWith("'''");
        String content;
        if (isMultiline) {
            content = text.substring(3, text.length() - 3);
        } else {
            content = text.substring(1, text.length() - 1);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\') {
                if (i + 1 < content.length()) {
                    char next = content.charAt(i + 1);
                    switch (next) {
                        case 't':
                            sb.append('\t');
                            i++;
                            break;
                        case 'n':
                            sb.append('\n');
                            i++;
                            break;
                        case 'r':
                            sb.append('\r');
                            i++;
                            break;
                        case 'b':
                            sb.append('\b');
                            i++;
                            break;
                        case 'f':
                            sb.append('\f');
                            i++;
                            break;
                        case '\"':
                            sb.append('\"');
                            i++;
                            break;
                        case '\'':
                            sb.append('\'');
                            i++;
                            break;
                        case '\\':
                            sb.append('\\');
                            i++;
                            break;
                        case 'u':
                        case 'U':
                            int len = (next == 'u') ? 4 : 8;
                            if (i + len + 1 <= content.length()) {
                                try {
                                    String hex = content.substring(i + 2, i + 2 + len);
                                    int codePoint = Integer.parseInt(hex, 16);

                                    if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                                        throw new IllegalArgumentException("Invalid Unicode escape sequence: Surrogate code points are not allowed.");
                                    }

                                    sb.appendCodePoint(codePoint);
                                    i += len + 1;
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException("Invalid Unicode escape sequence: Invalid hexadecimal value.", e);
                                }
                            } else {
                                throw new IllegalArgumentException("Incomplete Unicode escape sequence.");
                            }
                            break;
                        default:
                            sb.append(c).append(next);
                            i++;
                            break;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}