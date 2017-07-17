package fr.inria.acacia.corese.triple.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br>
 * This small class is used to manage function�s parameters list in the Sparql parser.
 * <br>
 * @author Virginie Bottollier
 */

public class ExpressionList extends ArrayList<Expression> {
   	
	boolean isDistinct = false;
        private boolean nested = false;
	String separator;
        Expression eseparator;
        HashMap<String, Constant> table;
        private List<ExpressionList> list;

	/** Use to keep the class version, to be consistent with the interface Serializable.java */
	private static final long serialVersionUID = 1L;

	public ExpressionList() {
		super();
                list = new ArrayList<>();
	}
        
        public ExpressionList(Expression e) {
		this();
                add(e);
	}
        
        public ExpressionList(List<Expression> l) {
		this();
                addAll(l);
	}
	
	public void setDistinct(boolean b){
		isDistinct = b;
	}
	
	public boolean isDistinct(){
		return isDistinct;
	}
	
	public void setSeparator(String s){
		separator = s;
	}
               	
	public String getSeparator(){
		return separator;
	}
        
        public void setExpSeparator(Expression e){
            if (e.isConstant()){
                setSeparator(e.getLabel());
            }
            else {
		eseparator = e;
            }
	}
               	
	public Expression getExpSeparator(){
		return eseparator;
	}
        
        HashMap<String, Constant> table(){
            if (table == null){
                table = new HashMap<>();
            }
            return table;
        }
        
        public void defType(Variable var, Constant type){
            if (type != null){
                table().put(var.getLabel(), type);
            }
        }
        
        public HashMap<String, Constant> getTable(){
            return table;
        }

    public boolean isNested() {
        return ! list.isEmpty() || nested;
    }
    
    public void setNested(boolean b){
        nested = b;
    }
   
     /**
     * @return the list
     */
    public List<ExpressionList> getList() {
        return list;
    }

    /**
     * @param list the list to set
     */
    public void setList(List<ExpressionList> list) {
        this.list = list;
    }
    
    public void add(ExpressionList l){
        list.add(l);
    }

}
