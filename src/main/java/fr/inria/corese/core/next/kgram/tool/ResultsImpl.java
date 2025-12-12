package fr.inria.corese.core.next.kgram.tool;

import fr.inria.corese.core.next.kgram.api.core.Node;
import fr.inria.corese.core.next.kgram.api.query.Result;
import fr.inria.corese.core.next.kgram.api.query.Results;
import fr.inria.corese.core.next.kgram.core.Mapping;
import fr.inria.corese.core.next.kgram.core.Mappings;

import java.util.Iterator;
import java.util.List;

public class ResultsImpl implements Results {

	Mappings maps;
	
	ResultsImpl(Mappings ms){
		maps = ms;
	}
	
	public static ResultsImpl create(Mappings ms){
		ResultsImpl res = new ResultsImpl(ms);
		return res;
	}
	
	public List<Node> getSelect() {
		return maps.getSelect();
	}
	
	public Iterator<Result> iterator() {
		final Iterator<Mapping> it = maps.iterator();
		
		return new Iterator<Result>(){

			public boolean hasNext() {
				return it.hasNext();
			}

			public Result next() {
				return it.next();
			}

			public void remove() {				
			}
			
		};
	}

	public int size() {
		return maps.size();
	}
	
	

}
