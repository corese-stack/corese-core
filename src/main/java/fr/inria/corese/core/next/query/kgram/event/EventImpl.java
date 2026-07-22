package fr.inria.corese.core.next.query.kgram.event;


import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Stack;

import java.util.Hashtable;


/**
 * Event to trace KGRAM execution
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public class EventImpl implements Event {

	
	static Hashtable<Integer, String> titles;
	
	int type;
	boolean isSuccess = true;
	Object object, arg, arg2;
	
	EventImpl(int n, Object o){
		type = n;
		object = o;
	}
	
	EventImpl(int n, Object o, Object o2){
		type = n;
		object = o;
		arg = o2;
		if (o2 instanceof Boolean){
			isSuccess = (Boolean) o2;
		}
	}
	
	EventImpl(int n, Object o, Object o2, Object o3){
		type = n;
		object = o;
		arg = o2;
		arg2 = o3;
		if (o3 instanceof Boolean){
			isSuccess = (Boolean) o3;
		}
	}
	
	public String toString(){
		String str = getTitle();
		if (object != null){
			str += " ";
			if (object instanceof Exp exp){
                if (exp.isEdge()){
					str += "("+ exp.getEdge().getEdgeIndex() + ") ";
				}
			}
			str += object;
		}
		if (arg != null){
			str +=  "\n";
			str += arg;
		}
		if (arg2 != null && ! (arg2 instanceof Stack)) str +=  "\n" + arg2;

		return str;
	}
	
	
	static void deftitle(int type, String title){
		titles.put(type, title);
	}
	
	String getTitle(){
		if (titles == null){
			init();
		}
		return titles.get(type);
	}
	
	static void init(){
		titles = new Hashtable<>();
		deftitle(BEGIN, "begin");
		deftitle(START, "start");
		deftitle(ENUM, 	"enum");
		deftitle(FILTER, "filter");
		deftitle(BIND, 	"bind");
		deftitle(MATCH, "match");
		deftitle(GRAPH, "graph");
		deftitle(PATH, 	"path");
		deftitle(PATHSTEP, "step");
		deftitle(FINISH, "finish");
		

		deftitle(AGG, "aggregate");
		deftitle(DISTINCT, "distinct");
		deftitle(LIMIT, "limit");

		deftitle(RESULT, "result");
		deftitle(END, "end");
		
		// User Event
		
		deftitle(COMPLETE, 	"complete");
		deftitle(FORWARD, 	"forward");
		deftitle(MAP, 		"map");
		deftitle(NEXT, 		"next");
		deftitle(QUIT, 		"quit");
		deftitle(STEP, 		"step");
		deftitle(SUCCESS, 	"success");

		deftitle(VERBOSE, "verbose");
		deftitle(HELP, "help");
	}
	
	

	public static EventImpl create(int type, Object obj){
        return new EventImpl(type, obj);
	}
	
	public static EventImpl create(int type, Object obj, Object arg){
        return new EventImpl(type, obj, arg);
	}

	public static EventImpl create(int type, Object obj, Object arg, Object arg2){
        return new EventImpl(type, obj, arg, arg2);
	}
	
	public int getSort(){
		return type;
	}
	
	public Object getObject(){
		return object;
	}
	
	public Object getArg(int n){
        return switch (n) {
            case 0 -> object;
            case 1 -> arg;
            case 2 -> arg2;
            default -> null;
        };
    }
	
	public Exp getExp(){
		if (object instanceof Exp){
			return (Exp) object;
		}
		return null;
	}
	
	public boolean isSuccess(){
		return isSuccess;
	}

}
