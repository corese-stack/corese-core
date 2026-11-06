package fr.inria.corese.core.next.data.impl.io.parser.trig;

import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.impl.io.parser.support.TurtleTriGDocument;
import fr.inria.corese.core.next.data.spi.io.parser.AbstractRDFParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Streaming RDF 1.1 TriG parser without tokens, parse trees, or ANTLR.
 *
 * <p>Extends the Turtle FSM with named-graph block support:
 * {@code GRAPH <label> \{ ... \}}, bare {@code <label> \{ ... \}}, and
 * anonymous {@code \{ ... \}} default-graph blocks. Triples are separated by
 * {@code .}; the final dot immediately before {@code \}} is optional.</p>
 *
 * <p>All Turtle features (prefixes, base, blank-node property lists, collections,
 * long literals, numeric and boolean literals) are available both at the top level
 * and inside graph blocks.</p>
 */
public final class TriGScanner extends AbstractRDFParser {

    public TriGScanner(Model model, ValueFactory factory) {
        this(model, factory, new TriGParserOptions.Builder().build());
    }

    public TriGScanner(Model model, ValueFactory factory, RDFParsingOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.TRIG;
    }

    @Override
    public void parse(InputStream input) throws ParsingException {
        parse(input, null);
    }

    @Override
    public void parse(InputStream input, String baseURI) throws ParsingException {
        Objects.requireNonNull(input, "input");
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        parse(new InputStreamReader(input, decoder), baseURI);
    }

    @Override
    public void parse(Reader reader, String baseURI) throws ParsingException {
        String base = baseURI;
        if (base == null && getConfig() instanceof BaseIRIOptions o) {
            base = o.getBaseIRI();
        }
        new TriGDocument(
                Objects.requireNonNull(reader, "reader"),
                getModel(), getValueFactory(), base
        ).parse();
    }


    private static final class TriGDocument extends TurtleTriGDocument {

        private static final String EXPECTED_GRAPH_TRIPLE_SEPARATOR =
                "Expected '.' between triples in graph block, got: '";

        /** Active named graph; {@code null} means the default graph. */
        private Resource currentGraph;

        TriGDocument(Reader reader, Model model, ValueFactory factory, String baseURI) {
            super(reader, model, factory, baseURI);
        }

        // ── Route statements to the current named graph ────────────────────

        @Override
        protected void emitTriple(Resource s, IRI p, Value o) {
            model.add(s, p, o, currentGraph);
        }

        // ── TriG document loop ─────────────────────────────────────────────

        @Override
        protected void parseDocument() throws IOException {
            while (true) {
                ws();
                if (peek() == -1) break;
                checkInterrupted();
                trigTopLevel();
            }
        }

        /**
         * Dispatches one top-level TriG item: directive, anonymous graph block,
         * {@code GRAPH} block, or triple/graph-label statement.
         */
        private void trigTopLevel() throws IOException {
            int c = peek();
            switch (c) {
                case '@' -> atDirective();
                case '{' -> wrappedGraph(null);
                case '[' -> blankNodeTopLevel();
                case '(' -> collectionTopLevel();
                case '<' -> resourceTopLevel(intern(iriRefContent()));
                case '_' -> resourceTopLevel(blankNodeLabel());
                case ':' -> {
                    take();
                    resourceTopLevel(intern(lookupPrefix("") + readLocalPart()));
                }
                default -> namedTopLevel(c);
            }
        }

        private void blankNodeTopLevel() throws IOException {
            expect('[');
            ws();
            BNode bnode = factory.createBNode();
            boolean isEmpty = (peek() == ']');
            if (!isEmpty) {
                predicateObjectList(bnode);
                ws();
            }
            expect(']');
            ws();
            if (isEmpty && peek() == '{') {
                wrappedGraph(bnode);
            } else {
                if (!isEmpty && peek() == '{') {
                    throw error("A blank node property list cannot be used as a named graph label");
                }
                blankNodeTriples(bnode);
            }
        }

        private void blankNodeTriples(BNode bnode) throws IOException {
            if (peek() != '.' && peek() != -1) {
                predicateObjectList(bnode);
                ws();
            }
            expect('.');
        }

        private void collectionTopLevel() throws IOException {
            Resource subj = collection();
            ws();
            predicateObjectList(subj);
            ws();
            expect('.');
        }

        private void resourceTopLevel(Resource res) throws IOException {
            ws();
            labelOrTriples(res);
        }

        private void namedTopLevel(int c) throws IOException {
            if (isAsciiAlpha(c) || unicodeNameStart(c)) {
                String name = readName();
                if (peek() == ':') {
                    take();
                    Resource res = intern(lookupPrefix(name) + readLocalPart());
                    ws();
                    labelOrTriples(res);
                } else if (name.equalsIgnoreCase("GRAPH")) {
                    ws();
                    trigGraphBlock();
                } else if (name.equalsIgnoreCase("PREFIX")) {
                    ws();
                    sparqlPrefixDirective();
                } else if (name.equalsIgnoreCase("BASE")) {
                    ws();
                    sparqlBaseDirective();
                } else {
                    throw error("Unexpected keyword at TriG top level: '" + name + "'");
                }
                return;
            }

            throw error("Unexpected character in TriG document: '" + (char) c + "'");
        }

        /**
         * After reading a resource at top level, determines whether it is a
         * named-graph label (followed by {@code {}) or a triple subject.
         */
        private void labelOrTriples(Resource res) throws IOException {
            if (peek() == '{') {
                wrappedGraph(res);
            } else {
                predicateObjectList(res);
                ws();
                expect('.');
            }
        }

        /** Handles {@code GRAPH <label> \{ ... \}}. */
        private void trigGraphBlock() throws IOException {
            int c = peek();
            Resource graphLabel;
            if (c == '<') {
                graphLabel = intern(iriRefContent());
            } else if (c == '_') {
                graphLabel = blankNodeLabel();
            } else if (c == '[') {
                // GRAPH [] { ... } — anonymous blank node as graph label
                expect('[');
                ws();
                expect(']');
                graphLabel = factory.createBNode();
            } else if (c == ':') {
                take();
                graphLabel = intern(lookupPrefix("") + readLocalPart());
            } else if (isAsciiAlpha(c) || unicodeNameStart(c)) {
                String pfx = readName();
                expect(':');
                graphLabel = intern(lookupPrefix(pfx) + readLocalPart());
            } else {
                throw error("Expected graph label IRI or blank node after GRAPH keyword");
            }
            ws();
            wrappedGraph(graphLabel);
        }

        /**
         * Parses {@code \{ items* \}} and routes all contained statements to
         * {@code graphLabel} (or the default graph if {@code null}).
         */
        private void wrappedGraph(Resource graphLabel) throws IOException {
            expect('{');
            Resource saved = currentGraph;
            currentGraph = graphLabel;
            try {
                ws();
                while (peek() != '}' && peek() != -1) {
                    checkInterrupted();
                    graphBlockItem();
                    ws();
                }
            } finally {
                currentGraph = saved;
            }
            expect('}');
        }

        /** Parses one item inside a TriG graph block. */
        private void graphBlockItem() throws IOException {
            if (peek() == '[') {
                Resource subj = blankNodePropertyList();
                ws();
                if (peek() != '.' && peek() != '}') {
                    predicateObjectList(subj);
                    ws();
                }
            } else {
                Resource subj = subject();
                ws();
                predicateObjectList(subj);
                ws();
            }
            graphTripleSeparator();
        }

        private void graphTripleSeparator() throws IOException {
            if (peek() == '.') {
                take();
            } else if (peek() != '}') {
                throw error(EXPECTED_GRAPH_TRIPLE_SEPARATOR + (char) peek() + "'");
            }
        }

    }
}
