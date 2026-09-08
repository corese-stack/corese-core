package fr.inria.corese.core.next.query.impl.engine.path;


import fr.inria.corese.core.next.query.impl.engine.spi.Environment;

/**
 * Draft to compute path in the graph
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public class GraphPath extends Thread {


    private final Environment mem;
	private final PathFinder finder;



	/**
	 * ?x %path ?y
	 * case:
	 * ?x or ?y is bound/unbound
	 * filter on ?x ?y
	 * Relation type on %path : ?x c:related::%path ?y
	 *
	 */

	public GraphPath(PathFinder pc, Environment mem){
		this.finder  = pc;
		this.mem = mem;
    }

    @Override
    public void run() {
        finder.process(finder.get(mem, finder.getIndex()), mem);
    }
}
