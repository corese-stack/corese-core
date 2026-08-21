package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * Converts KGRAM {@link Node} constants to API {@link Value} instances.
 *
 * <p>This is the single point in the bridge layer that is allowed to inspect
 * {@link IDatatype} on behalf of callers outside {@code next.query.impl.kgram}.</p>
 */
public final class KgramNodeConverter {

    private KgramNodeConverter() {}

    /**
     * Converts a KGRAM constant {@link Node} to the corresponding API {@link Value}.
     *
     * @param node    the KGRAM node to convert (must not be null)
     * @param factory the value factory used to create API term instances
     * @return the API value, or {@code null} when the node kind is not supported
     */
    public static Value nodeToValue(Node node, CoreseValueFactory factory) {
        IDatatype dt = node.getDatatypeValue();
        if (dt.isURI()) {
            return factory.createIRI(dt.getLabel());
        }
        if (dt.isBlank()) {
            return factory.createBNode(dt.getLabel());
        }
        if (dt.isLiteral()) {
            String lang = dt.getLang();
            if (lang != null && !lang.isEmpty()) {
                return factory.createLiteral(dt.getLabel(), lang);
            }
            String datatypeUri = dt.getDatatypeURI();
            if (datatypeUri != null) {
                return factory.createLiteral(dt.getLabel(), factory.createIRI(datatypeUri));
            }
            return factory.createLiteral(dt.getLabel());
        }
        return null;
    }
}
