package fr.inria.corese.core.kgram.api.query;

import java.util.List;

import fr.inria.corese.core.kgram.api.core.Node;

/**
 * Interface to KGRAM  results (mappings)
 * 
 * @author corby
 *
 */
public interface Results extends Iterable<Result>
{
	
	List<Node> getSelect();
	
	int size();
	
}
