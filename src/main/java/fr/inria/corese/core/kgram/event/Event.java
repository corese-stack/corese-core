package fr.inria.corese.core.kgram.event;

import fr.inria.corese.core.kgram.core.Exp;

/**
 * Event to trace KGRAM execution
 * 
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public interface Event {
	
	int BEGIN 	= 0;
	int START 	= 1;
	int ENUM 	= 2;
	int MATCH 	= 3;
	int FILTER 	= 4;
	int BIND 	= 5;
	int GRAPH 	= 6;
	int PATH 	= 7;
	int PATHSTEP 	= 8;
	
	
	int AGG 	= 13;

	int FINISH 	= 14;
	int DISTINCT = 15;
	int LIMIT 	= 16;
	
	int RESULT 	= 19;
	int END 	= 20;
	

	// warning events
	
	
	int UNDEF_PROPERTY 	= 30;
	int UNDEF_CLASS 	= 31;
	int UNDEF_GRAPH 	= 32;

	
	
	
	
	int ALL 	= 50;
	
	
	
	// events from user to listener
	
	// next step
    int STEP 	= 101;
	
	// next edge/node/path 
    int NEXT 	= 102;
	
	// skip trace until next expression in stack
    int FORWARD 	= 103;

	// eval current expression silently 
    int COMPLETE 	= 104;
	
	// eval until current expression succeed 
    int SUCCESS 	= 105;
	
	// resume execution silently
    int QUIT 	= 109;
	
	
	
	
	// pprint current Mapping
    int MAP 	= 110;
	int VERBOSE = 111;
	int NONVERBOSE = 113;
	int HELP = 112;


	
	int getSort();
	
	Object getObject();
	
	Object getArg(int n);

	Exp getExp();
	
	boolean isSuccess();


}
