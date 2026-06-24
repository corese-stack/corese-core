package fr.inria.corese.core.sparql.datatype;

public class CoreseStringBuilder extends CoreseString {
	
	StringBuilder sb;
	
	CoreseStringBuilder(StringBuilder s){
		sb = s;
		value = null;
	}
	
	
        @Override
	public String getLabel(){
		if (value == null){
			value = sb.toString();
		}
		return value;
	}
	
        @Override
	public StringBuilder getStringBuilder(){
		return sb;
	}
        
        

}
