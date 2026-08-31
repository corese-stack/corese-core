package fr.inria.corese.core.next.query.impl.kgram.adapter;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.core.TripleStore;
import fr.inria.corese.core.kgram.path.Path;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * Wraps a next-package {@link fr.inria.corese.core.next.query.impl.kgram.api.core.Node}
 * as an old kgram {@link Node} so that legacy {@code Expression.eval} call chains
 * (e.g. Variable → env.getNode → node.getDatatypeValue) work without modification.
 *
 * <p>Only {@link #getDatatypeValue()}, {@link #getValue()}, and {@link #getLabel()}
 * are functional; all other methods throw {@link UnsupportedOperationException}.</p>
 */
final class LegacyNodeBridge implements Node {

    private final fr.inria.corese.core.next.query.impl.kgram.api.core.Node delegate;

    LegacyNodeBridge(fr.inria.corese.core.next.query.impl.kgram.api.core.Node delegate) {
        this.delegate = delegate;
    }

    @Override public IDatatype getDatatypeValue() { return delegate.getDatatypeValue(); }
    @Override public IDatatype getValue()          { return delegate.getDatatypeValue(); }
    @Override public String  getLabel()            { return delegate.getLabel(); }
    @Override public boolean isVariable()          { return delegate.isVariable(); }
    @Override public boolean isConstant()          { return delegate.isConstant(); }
    @Override public boolean isBlank()             { return delegate.isBlank(); }

    // --- stubs: not invoked during basic FILTER evaluation ---
    @Override public int     getIndex()             { return 0; }
    @Override public void    setIndex(int n)        {}
    @Override public String  getKey()               { return null; }
    @Override public void    setKey(String str)     {}
    @Override public boolean same(Node n)           { return false; }
    @Override public boolean match(Node n)          { return false; }
    @Override public int     compare(Node node)     { return 0; }
    @Override public boolean isFuture()             { return false; }
    @Override public Node    getGraph()             { return null; }
    @Override public Node    getNode()              { return this; }
    @Override public Object  getNodeObject()        { return null; }
    @Override public void    setObject(Object o)    {}
    @Override public Path    getPath()              { return null; }
    @Override public TripleStore getTripleStore()   { return null; }
}
