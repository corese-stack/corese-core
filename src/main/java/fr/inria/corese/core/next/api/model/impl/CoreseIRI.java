package fr.inria.corese.core.next.api.model.impl;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseURI;

/**
 * Wrapper for Corese Node that represents an IRI
 */
public class CoreseIRI extends AbstractIRI implements CoreseNodeWrapper {

    private final Node node;

    public CoreseIRI(String fullIRI) {
        super(fullIRI);
        this.node = new CoreseURI(fullIRI);
    }

    public CoreseIRI(IRI iri) {
        super(iri.stringValue());
        this.node = new CoreseURI(iri.stringValue());
    }

    public CoreseIRI(Node node) {
        super(node.getLabel());
        if(node.isConstant() && ! node.isBlank() && node.getNodeKind() == IDatatype.NodeKind.URI ) {
            this.node = node;
        } else {
            throw new IllegalArgumentException("Node must be an URI");
        }
    }

    @Override
    public Node getCoreseNode() {
        return node;
    }
}
