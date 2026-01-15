package fr.inria.corese.core.next.kgram.tool;

import fr.inria.corese.core.next.kgram.api.core.BindingContext;
import fr.inria.corese.core.next.kgram.api.core.Edge;
import fr.inria.corese.core.next.kgram.api.core.Expr;
import fr.inria.corese.core.next.kgram.api.core.Node;
import fr.inria.corese.core.next.kgram.api.query.Environment;
import fr.inria.corese.core.next.kgram.api.query.ProcessVisitor;
import fr.inria.corese.core.next.kgram.core.*;
import fr.inria.corese.core.next.kgram.event.EventManager;
import fr.inria.corese.core.next.kgram.path.Path;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.ASTExtension;

import java.util.Map;

public class EnvironmentImpl implements Environment {
	protected Query query;
	
	public EnvironmentImpl(){
	}
	
	EnvironmentImpl(Query q){
		query = q;
	}
	
	public static EnvironmentImpl create(Query q){
		return new EnvironmentImpl(q);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node getNode(Node node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node getQueryNode(int n) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node getQueryNode(String label) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isBound(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int pathLength(Node node) {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public Query getQuery() {
		// TODO Auto-generated method stub
		return query;
	}

	@Override
	public EventManager getEventManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node getGraphNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setObject(Object o) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object getObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setExp(Exp exp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Exp getExp() {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, IDatatype> getMap() {
		return null;
	}

    @Override
    public Edge[] getEdges() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node[] getNodes() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node[] getQueryNodes() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Path getPath(Node qNode) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Mappings getMappings() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node get(Expr var) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ASTExtension getExtension() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BindingContext  getBind() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    @Override
    public boolean hasBind() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ApproximateSearchEnv getAppxSearchEnv() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setBind(BindingContext b) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterable<Mapping> getAggregate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void aggregate(Mapping m, int n) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Mapping getMapping() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Eval getEval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setEval(Eval e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ProcessVisitor getVisitor() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setReport(IDatatype dt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IDatatype getReport() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

	

}
