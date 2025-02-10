package fr.inria.corese.core.api;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.sparql.exceptions.EngineException;

/**
 * 
 * @author Olivier Corby, Wimmics INRIA 2012
 */
public interface Engine {
	
	int UNDEF = -1;
	int RDFS_ENGINE = 0;
	int RULE_ENGINE = 1;
	int QUERY_ENGINE = 2;
	int WORKFLOW_ENGINE = 3;

	
	// temporarily desactivate 
	void setActivate(boolean b);
	
	boolean isActivate();
	
	void init();

	// return true if some new entailment have been performed 
	boolean process() throws EngineException ;
	
	// remove entailments
	void remove();
	
	// some edges have been deleted
	void onDelete();
	
	// edge inserted
	void onInsert(Node gNode, Edge edge);
	
	// graph have been cleared
	void onClear();
	
	int type();


}
