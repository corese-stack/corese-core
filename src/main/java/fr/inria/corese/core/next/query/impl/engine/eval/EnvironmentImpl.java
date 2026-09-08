package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;

import fr.inria.corese.core.next.query.impl.engine.model.BindingContext;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitor;
import fr.inria.corese.core.next.query.impl.engine.event.KgramEventDispatcher;
import fr.inria.corese.core.next.query.impl.engine.path.Path;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;

import java.util.Map;

public class EnvironmentImpl implements Environment {
	protected Query query;

	public EnvironmentImpl(){
		// The base environment starts without a query or bindings.
	}

        @Override
	public int count() {
		return 0;
	}

        @Override
	public Node getNode(Expr variable){
		return null;
	}

        @Override
        public int size() {
            return 0;
        }

	@Override
	public Node getNode(String label) {
		return null;
	}

	@Override
	public Node getNode(Node node) {
		return null;
	}

	@Override
	public Node getQueryNode(int n) {
		return null;
	}

	@Override
	public Node getQueryNode(String label) {
		return null;
	}

	@Override
	public boolean isBound(Node node) {
		return false;
	}

	@Override
	public int pathLength(Node node) {
		return 0;
	}



	@Override
	public Query getQuery() {
		return query;
	}

	@Override
	public KgramEventDispatcher getEventManager() {
		return null;
	}

	@Override
	public Node getGraphNode() {
		return null;
	}

	@Override
	public void setObject(Object o) {
		// The base environment does not retain an attached object.
	}

	@Override
	public Object getObject() {
		return null;
	}

	@Override
	public void setExp(Exp exp) {
		// Expression storage is supplied by concrete execution environments.
	}

	@Override
	public Exp getExp() {
		return null;
	}

	public Map<String, DatatypeValue> getMap() {
		return Map.of();
	}

    @Override
    public Edge[] getEdges() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Node[] getNodes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Node[] getQueryNodes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Path getPath(Node qNode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Mappings getMappings() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Node get(Expr variable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BindingContext  getBind() {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public boolean hasBind() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ApproximateSearchEnv getAppxSearchEnv() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setBind(BindingContext b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterable<Mapping> getAggregate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void aggregate(Mapping m, int n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Mapping getMapping() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Eval getEval() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setEval(Eval e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ProcessVisitor getVisitor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setReport(DatatypeValue dt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DatatypeValue getReport() {
        throw new UnsupportedOperationException("Not supported yet.");
    }



}
