package fr.inria.corese.core.kgram.tool;

import java.util.Iterator;
import java.util.List;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.query.Result;
import fr.inria.corese.core.kgram.api.query.Results;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;

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
