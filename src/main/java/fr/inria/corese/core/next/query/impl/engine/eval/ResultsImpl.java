package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.spi.Result;
import fr.inria.corese.core.next.query.impl.engine.spi.Results;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;

import java.util.Iterator;
import java.util.List;

public class ResultsImpl implements Results {

	Mappings maps;

	ResultsImpl(Mappings ms){
		maps = ms;
	}

	public List<Node> getSelect() {
		return maps.getSelect();
	}

	@Override
	public Iterator<Result> iterator() {
		Iterator<Mapping> it = maps.iterator();
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Result next() {
				return it.next();
			}
		};
	}

	public int size() {
		return maps.size();
	}



}
