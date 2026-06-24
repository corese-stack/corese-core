package fr.inria.corese.core.sparql.triple.parser;

public class Join extends And {
	
	
	
        @Override
	public boolean isJoin(){
		return true;
	}
	
        @Override
	public ASTBuffer toString(ASTBuffer sb){
		sb.append(get(0));
		//sb.append(" " + KeywordPP.JOIN + " ");
		sb.append(get(1));
		return sb;
	}
	
	
}
