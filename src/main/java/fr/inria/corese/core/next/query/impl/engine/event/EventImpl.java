package fr.inria.corese.core.next.query.impl.engine.event;


import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.eval.Stack;

/**
 * Event to trace KGRAM execution
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public class EventImpl implements Event {


	int type;
	boolean isSuccess = true;
	Object object;
	Object arg;
	Object arg2;

	EventImpl(int n, Object o){
		type = n;
		object = o;
	}

	EventImpl(int n, Object o, Object o2){
		type = n;
		object = o;
		arg = o2;
		if (o2 instanceof Boolean success){
			isSuccess = success;
		}
	}

	EventImpl(int n, Object o, Object o2, Object o3){
		type = n;
		object = o;
		arg = o2;
		arg2 = o3;
		if (o3 instanceof Boolean success){
			isSuccess = success;
		}
	}

	public String toString(){
		String str = getTitle();
		if (object != null){
			str += " ";
			if (object instanceof Exp exp && exp.isEdge()){
					str += "("+ exp.getEdge().getEdgeIndex() + ") ";
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


	String getTitle(){
		return switch (type) {
			case BEGIN -> "begin";
			case START -> "start";
			case ENUM -> "enum";
			case FILTER -> "filter";
			case BIND -> "bind";
			case MATCH -> "match";
			case GRAPH -> "graph";
			case PATH -> "path";
			case PATHSTEP, STEP -> "step";
			case FINISH -> "finish";
			case AGG -> "aggregate";
			case DISTINCT -> "distinct";
			case LIMIT -> "limit";
			case RESULT -> "result";
			case END -> "end";
			case COMPLETE -> "complete";
			case FORWARD -> "forward";
			case MAP -> "map";
			case NEXT -> "next";
			case QUIT -> "quit";
			case SUCCESS -> "success";
			case VERBOSE -> "verbose";
			case HELP -> "help";
			default -> "event(" + type + ')';
		};
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
		if (object instanceof Exp expression){
			return expression;
		}
		return null;
	}

	public boolean isSuccess(){
		return isSuccess;
	}

}
