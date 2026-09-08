package fr.inria.corese.core.next.query.impl.engine.event;

import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.spi.Result;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Regex;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.path.Path;

/**
 * Result Listener to process KGRAM result on the fly
 *
 * @author Olivier Corby, Edelweiss, INRIA 2011
 *
 */
public interface ResultListener {

	/**
	 * For each solution, kgram call process(env)
	 * If return true:  Mapping created as usual
	 * If return false: Mapping not created
	 */
	boolean process(Environment env);


	/**
	 * For each path, kgram call process(path)
	 * If return true:  Mapping created as usual
	 * If return false: Mapping not created
	 */
	boolean process(Path path);

	boolean enter(Edge ent, Regex exp, int size);

	boolean leave(Edge ent, Regex exp, int size);

        boolean listen(Exp exp, Edge query, Edge target);

        Exp listen(Exp exp, int n);

        void listen(Expr exp);
}
