package fr.inria.corese.core.next.query.impl.sparql.update;

import fr.inria.corese.core.next.data.Models;
import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.io.CoreseIO;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.storage.api.StorageManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Evaluates SPARQL LOAD operations by parsing external RDF data before applying mutations.
 * Blank node labels are kept local and fresh for each load invocation.
 */
final class UpdateLoader {

    private UpdateLoader() { }

    /**
     * Loads RDF data from the given source IRI into the destination graph.
     *
     * @param storage the storage manager to add statements to
     * @param source  the IRI of the external RDF document to load
     * @param target  the destination named graph, or {@code null} for the default graph
     * @throws QueryEvaluationException if reading or parsing the source fails
     */
    static void load(StorageManager storage, Resource source, Resource target) {
        Model model = read(source.stringValue());
        Map<BNode, BNode> blankNodes = new HashMap<>();
        if (target != null) {
            storage.mutations().createGraph(target);
        }
        for (Statement statement : model) {
            Resource subject = (Resource) fresh(statement.getSubject(), blankNodes);
            Value object = fresh(statement.getObject(), blankNodes);
            storage.mutations().add(Values.factory().createStatement(subject, statement.getPredicate(), object, target));
        }
    }

    private static Model read(String source) {
        try {
            URI uri = URI.create(source);
            URLConnection connection = uri.toURL().openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "text/turtle, application/rdf+xml, application/n-triples");
            try (InputStream stream = connection.getInputStream()) {
                String path = uri.getPath();
                String extension = path == null ? "" : path.substring(path.lastIndexOf('.') + 1);
                RDFFormat format = RDFFormat.byMimeType(connection.getContentType())
                        .or(() -> RDFFormat.byExtension(extension)).orElse(RDFFormat.TURTLE);
                Model model = Models.create();
                CoreseIO.rdfParserFactory().createRDFParser(format, model, Values.factory()).parse(stream, source);
                return model;
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new QueryEvaluationException("Could not LOAD " + source, e);
        }
    }

    private static Value fresh(Value value, Map<BNode, BNode> blankNodes) {
        if (value instanceof BNode blank) {
            return blankNodes.computeIfAbsent(blank, ignored -> Values.factory().createBNode());
        }
        return value;
    }
}
