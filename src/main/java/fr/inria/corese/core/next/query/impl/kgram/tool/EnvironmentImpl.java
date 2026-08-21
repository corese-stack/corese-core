package fr.inria.corese.core.next.query.impl.kgram.tool;

import fr.inria.corese.core.next.query.impl.kgram.api.core.BindingContext;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.impl.kgram.api.query.ProcessVisitor;
import fr.inria.corese.core.next.query.impl.kgram.core.*;
import fr.inria.corese.core.next.query.impl.kgram.event.KgramEventDispatcher;
import fr.inria.corese.core.next.query.impl.kgram.path.Path;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.parser.ASTExtension;

import java.util.Map;

public class EnvironmentImpl implements Environment {
	protected Query query;

	public EnvironmentImpl(){
	}

        @Override
	public int count() {
		return 0;
	}

        @Override
	public Node getNode(Expr var){
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

	}

	@Override
	public Object getObject() {
		return null;
	}

	@Override
	public void setExp(Exp exp) {

	}

	@Override
	public Exp getExp() {
		return null;
	}

	public Map<String, IDatatype> getMap() {
		return null;
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
    public Node get(Expr var) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ASTExtension getExtension() {
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
    public void setReport(IDatatype dt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IDatatype getReport() {
        throw new UnsupportedOperationException("Not supported yet.");
    }



}
