package fr.inria.corese.core.kgram.api.core;

import java.util.List;

public interface ExpPattern {
	
	void getVariables(List<String> list);
        
	void getVariables(List<String> list, boolean excludeLocal);

}
