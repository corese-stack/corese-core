package fr.inria.corese.sandbox;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.compiler.api.QueryVisitor;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.*;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.api.IDatatype;
import org.junit.Test;

import java.util.List;

public class ElasticSearchSandbox extends ProcessVisitorDefault {

    @Test
    public void test() throws Exception {

        System.out.println("START");
        Graph graph = Graph.create();
        graph.init();
        Load loader = Load.create(graph);

        loader.parse("src/test/java/fr/inria/corese/sandbox/data.ttl");

        QueryProcess exec = QueryProcess.create(graph);
        String query = "INSERT DATA { <http://example.com/7> <http://example.com/ate> <http://example.com/9> }";
        System.out.println(query);
        Mappings mappings = exec.query(query, this);
        System.out.println("END");

    }

    @Override
    public IDatatype graph(Eval eval, Node g, Exp e, Mappings m1) {
        System.out.println("graph");
        return defaultValue();
    }

    @Override
    public IDatatype query(Eval eval, Node g, Exp e, Mappings m1) {
        System.out.println("query");
        return defaultValue();
    }

    @Override
    public IDatatype service(Eval eval, Node g, Exp e, Mappings m1) {
        System.out.println("service");
        return defaultValue();
    }

    @Override
    public IDatatype join(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        System.out.println("join");
        return defaultValue();
    }

    @Override
    public IDatatype after(Mappings map) {
        System.out.println("after");
        return defaultValue();
    }

    @Override
    public IDatatype beforeUpdate(Query q) {
        System.out.println("beforeUpdate");
        return defaultValue();
}

    @Override
    public IDatatype afterUpdate(Mappings map) {
        System.out.println("afterUpdate");
        return defaultValue();
    }

    @Override
    public IDatatype finish(Mappings map) {
        System.out.println("finish");
        return defaultValue();
    }

    @Override
    public IDatatype insert(IDatatype path, Edge edge) {
        System.out.println("insert");
        return defaultValue();
    }

    @Override
    public IDatatype delete(Edge edge) {
        System.out.println("delete");
        return defaultValue();
    }

    @Override
    public IDatatype update(Query q, List<Edge> delete, List<Edge> insert) {
        System.out.println(String.format("Update delete:{} insert:{}", delete.size(), insert.size()));
        return defaultValue();
    }
}
