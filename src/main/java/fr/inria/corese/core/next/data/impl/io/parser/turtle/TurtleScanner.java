package fr.inria.corese.core.next.data.impl.io.parser.turtle;

import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.impl.io.parser.support.TurtleTriGDocument;
import fr.inria.corese.core.next.data.spi.io.parser.AbstractRDFParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Streaming RDF 1.1 Turtle parser without tokens, parse trees, or ANTLR.
 *
 * <p>Uses a character-level scanner that
 * streams resolved terms directly to the model with minimal allocations.
 * IRIs are cached with a bounded cache; blank nodes are scoped to one document.
 * Memory is O(longest term + distinct blank node labels + IRI cache size).</p>
 *
 * <p>Supports the full Turtle 1.1 grammar: {@code @prefix}/{@code PREFIX},
 * {@code @base}/{@code BASE}, prefixed names, relative IRIs, blank node
 * property lists, RDF collections, long string literals, and all numeric and
 * boolean literal forms.</p>
 */
public final class TurtleScanner extends AbstractRDFParser {

    public TurtleScanner(Model model, ValueFactory factory) {
        this(model, factory, new TurtleParserOptions.Builder().build());
    }

    public TurtleScanner(Model model, ValueFactory factory, RDFParsingOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.TURTLE;
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
        new TurtleTriGDocument(
                Objects.requireNonNull(reader, "reader"),
                getModel(), getValueFactory(), base
        ).parse();
    }
}
