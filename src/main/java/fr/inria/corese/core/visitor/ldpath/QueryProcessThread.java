package fr.inria.corese.core.visitor.ldpath;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.Constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2019
 *
 */
public class QueryProcessThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(QueryProcessThread.class);
    QueryProcess exec;
    ASTQuery ast;
    Mappings map;
    Constant predicate;
    private boolean join = false;
    
    
    QueryProcessThread(Graph g, ASTQuery ast, Constant p) {
        exec = QueryProcess.create(g);
        this.ast = ast;
        predicate = p;
    }
    
    void setVerbose(boolean b) {
        exec.getGraph().setVerbose(b);
    }
    
    @Override
    public void run() {
        process();
    }
    
    void process() {
        try {
            map = exec.query(ast);
        } catch (EngineException ex) {
            logger.error("An unexpected error has occurred", ex);
        }
    }
    
    Mappings getMappings() {
        return map;
    }
    
    ASTQuery getAST() {
        return ast;
    }
    
    Constant getPredicate() {
        return predicate;
    }
    
    
    /**
     * @return the join
     */
    public boolean isJoin() {
        return join;
    }

    /**
     * @param join the join to set
     */
    public void setJoin(boolean join) {
        this.join = join;
    }

}
