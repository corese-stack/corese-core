package fr.inria.corese.core.next.query.impl.sparql;

import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.sparql.datatype.CoreseLiteral;
import fr.inria.corese.core.next.query.kgram.sparql.datatype.CoreseURI;
import fr.inria.corese.core.next.query.kgram.sparql.triple.parser.Variable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoreseQueryArtifactAdapterTest {

    @Test
    void queryAstToQueryArtifact() {
    }

    @Test
    void termAstToNode() {
        IriAst iri = new IriAst("<http://ns.inria.fr/test/iri");
        Node iriNode = CoreseQueryArtifactAdapter.termAstToNode(iri);
        assertNotNull(iriNode);
        assertInstanceOf(CoreseURI.class, iriNode);

        LiteralAst lit = new LiteralAst("1234", "fr", null);
        Node litNode = CoreseQueryArtifactAdapter.termAstToNode(lit);
        assertNotNull(litNode);
        assertInstanceOf(CoreseLiteral.class, litNode);

        VarAst var = new VarAst("var1");
        Node varNode = CoreseQueryArtifactAdapter.termAstToNode(var);
        assertNotNull(varNode);
        assertInstanceOf(Variable.class, varNode);
    }

    @Test
    void triplePatternAstToEdge() {
        IriAst iri = new IriAst("<http://ns.inria.fr/test/iri>");
        LiteralAst lit = new LiteralAst("1234", "fr", null);
        VarAst var = new VarAst("var1");

        TriplePatternAst iriiriiri = new TriplePatternAst(iri, iri, iri);
        Edge iriiriiriEdge = CoreseQueryArtifactAdapter.triplePatternAstToEdge(iriiriiri);
        assertNotNull(iriiriiriEdge);
        assertInstanceOf(Edge.class, iriiriiriEdge);
        assertInstanceOf(CoreseURI.class, iriiriiriEdge.getSubjectNode());
        assertInstanceOf(CoreseURI.class, iriiriiriEdge.getPropertyNode());
        assertInstanceOf(CoreseURI.class, iriiriiriEdge.getObjectNode());

        TriplePatternAst iriirilit = new TriplePatternAst(iri, iri, lit);
        Edge iriirilitEdge = CoreseQueryArtifactAdapter.triplePatternAstToEdge(iriirilit);
        assertNotNull(iriiriiriEdge);
        assertInstanceOf(Edge.class, iriirilitEdge);
        assertInstanceOf(CoreseURI.class, iriirilitEdge.getSubjectNode());
        assertInstanceOf(CoreseURI.class, iriirilitEdge.getPropertyNode());
        assertInstanceOf(CoreseLiteral.class, iriirilitEdge.getObjectNode());

        TriplePatternAst varirilit = new TriplePatternAst(var, iri, lit);
        Edge varirilitEdge = CoreseQueryArtifactAdapter.triplePatternAstToEdge(varirilit);
        assertNotNull(varirilitEdge);
        assertInstanceOf(Edge.class, varirilitEdge);
        assertInstanceOf(Variable.class, varirilitEdge.getSubjectNode());
        assertInstanceOf(CoreseURI.class, varirilitEdge.getPropertyNode());
        assertInstanceOf(CoreseLiteral.class, varirilitEdge.getObjectNode());
    }
}