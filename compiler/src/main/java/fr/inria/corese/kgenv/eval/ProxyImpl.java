package fr.inria.corese.kgenv.eval;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.cg.datatype.DatatypeMap;
import fr.inria.corese.sparql.cg.datatype.RDF;
import fr.inria.corese.sparql.exceptions.CoreseDatatypeException;
import fr.inria.corese.sparql.triple.parser.ASTQuery;
import fr.inria.corese.sparql.triple.parser.Dataset;
import fr.inria.corese.sparql.triple.parser.NSManager;
import fr.inria.corese.sparql.triple.parser.Processor;
import fr.inria.corese.sparql.triple.parser.Term;
import fr.inria.corese.kgenv.api.ProxyPlugin;
import fr.inria.corese.kgram.api.core.Edge;
import fr.inria.corese.kgram.api.core.Entity;
import fr.inria.corese.kgram.api.core.Expr;
import fr.inria.corese.kgram.api.core.ExprLabel;
import fr.inria.corese.kgram.api.core.ExprType;
import fr.inria.corese.kgram.api.core.Node;
import fr.inria.corese.kgram.api.core.Pointerable;
import fr.inria.corese.kgram.api.query.Environment;
import fr.inria.corese.kgram.api.query.Evaluator;
import fr.inria.corese.kgram.api.query.Producer;
import fr.inria.corese.kgram.core.Eval;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.kgram.core.Memory;
import fr.inria.corese.kgram.core.PointerObject;
import fr.inria.corese.kgram.core.Query;
import fr.inria.corese.kgram.event.EvalListener;
import fr.inria.corese.kgram.event.Event;
import fr.inria.corese.kgram.event.EventImpl;
import fr.inria.corese.kgram.filter.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implements evaluator of operators & functions of filter language with
 * IDatatype values
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public class ProxyImpl implements Proxy, ExprType {

    private static final String URN_UUID = "urn:uuid:";
    private static Logger logger = LogManager.getLogger(ProxyImpl.class);
    public static final IDatatype TRUE = DatatypeMap.TRUE;
    public static final IDatatype FALSE = DatatypeMap.FALSE;
    public static final IDatatype UNDEF = DatatypeMap.UNBOUND;
    
    static final String UTF8 = "UTF-8";
    public static final String RDFNS = NSManager.RDF; //"http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDFTYPE = RDFNS + "type";
    private static final String USER_DISPLAY = NSManager.USER+"display";
    static final String alpha = "abcdefghijklmnoprstuvwxyz";
    static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static int count = 0;
    ProxyPlugin plugin;
    Custom custom;
    SQLFun sql;
    Evaluator eval;
    private Producer producer;
    EvalListener el;
    // for LDScript java compiling only
    private Environment environment;
    int number = 0;
    // KGRAM is relax wrt to string vs literal vs uri input arg of functions
    // eg regex() concat() strdt()
    // setMode(SPARQL_MODE) 
    boolean SPARQLCompliant = false;
    protected IDatatype EMPTY = DatatypeMap.newStringBuilder("");
    

    public ProxyImpl() {
        sql = new SQLFun();
        custom = new Custom();
    }

    @Override
    public void setEvaluator(Evaluator ev) {
        eval = ev;
        if (plugin != null) {
            plugin.setEvaluator(ev);
        }
    }

    @Override
    public Evaluator getEvaluator() {
        return eval;
    }
    
    public Eval getEval(){
        return (Eval) getEvaluator().getEval();
    }
        
    public void setPlugin(ProxyPlugin p) {
        plugin = p;
        plugin.setEvaluator(eval);
    }
    
    @Override
    public void setPlugin(Proxy p) {
        
    }

    @Override
    public Proxy getPlugin() {
        return plugin;
    }

    @Override
    public void setMode(int mode) {
        switch (mode) {

            case Evaluator.SPARQL_MODE:
                SPARQLCompliant = true;
                break;

            case Evaluator.KGRAM_MODE:
                SPARQLCompliant = false;
                break;
        }
        plugin.setMode(mode);
    }

    public void start() {
        number = 0;
    }
    
     @Override 
     public IDatatype cast(Object obj, Environment env, Producer p){
         return DatatypeMap.cast(obj);
     }
     
    @Override
     public IDatatype[] createParam(int n){
         return new IDatatype[n];
     }
     
     String label(int ope){
         switch (ope){
             case ExprType.EQ:  return ExprLabel.EQUAL;
             case ExprType.NEQ: return ExprLabel.DIFF;
             case ExprType.LT:  return ExprLabel.LESS;
             case ExprType.LE:  return ExprLabel.LESS_EQUAL;
             case ExprType.GT:  return ExprLabel.GREATER;
             case ExprType.GE:  return ExprLabel.GREATER_EQUAL;
             // Proxy implements IN with equal, lets use ext:equal as well
             case ExprType.IN:  return ExprLabel.EQUAL;
                 
             case ExprType.PLUS:    return ExprLabel.PLUS;
             case ExprType.MINUS:   return ExprLabel.MINUS;
             case ExprType.MULT:    return ExprLabel.MULT;
             case ExprType.DIV:     return ExprLabel.DIV;
         }
         return null;
     }
     
     /**
      * exp:      a = b
      * datatype: http://example.org/datatype
      * result:   http://example.org/datatype#equal
      */
     String label(Expr exp, String datatype){
         
         return datatype.concat(ExprLabel.SEPARATOR + label(exp.oper()));
     }
     
     IDatatype[] array(IDatatype o1, IDatatype o2){
          IDatatype[] args = new IDatatype[2];
          args[0] = o1;
          args[1] = o2;
          return args;
     }
     
      IDatatype[] array(IDatatype o1){
          IDatatype[] args = new IDatatype[1];
          args[0] = o1;
          return args;
     }
     
     // TODO: check ExprLabel.COMPARE
    @Override
     public int compare(Environment env, Producer p, Node o1, Node o2) {
        IDatatype dt1 = (IDatatype) o1.getValue();
        IDatatype dt2 = (IDatatype) o2.getValue();
        if (dt1.getCode() == IDatatype.UNDEF && dt2.getCode() == IDatatype.UNDEF){
            if (dt1.getDatatypeURI().equals(dt2.getDatatypeURI())){   
                // get an extension function that implements the operator for
                // the extended datatype
                Expr exp = eval.getDefine(env, ExprLabel.COMPARE, 2);
                if (exp != null){
                     IDatatype res = (IDatatype) eval.eval(exp.getFunction(), env, p, array(dt1, dt2), exp);
                     if (res == null){
                         return 0;
                     }
                     return res.intValue();
                }                
            }
        }
        return dt1.compareTo(dt2);
     }
     

    @Override
    public IDatatype term(Expr exp, Environment env, Producer p, Object o1, Object o2) {
        IDatatype dt1 = (IDatatype) o1;
        IDatatype dt2 = (IDatatype) o2;
        
        if (dt1.getCode() == IDatatype.UNDEF && dt2.getCode() == IDatatype.UNDEF){
            String d1 = dt1.getDatatypeURI();
            if (d1.equals(dt2.getDatatypeURI())){   
                // get an extension function that implements the operator for
                // the extended datatype
                Expr ee = eval.getDefine(env, label(exp, d1), exp.arity()); 
                if (ee != null){
                   return  (IDatatype) eval.eval(exp, env, p, array(dt1, dt2), ee);
                }                
            }
        }

        // TODO: this should depend on a boolean bnodeExtension
//        if (dt1.isBlank() && dt2.isBlank() && ! exp.isFuncall()) {
//            // exclude funcall kg:equal() to prevent a loop
//            Expr ee = eval.getDefine(env, label(exp, ExpType.BNODE), exp.arity());
//            if (ee != null) {
//                return eval.eval(exp, env, p, array(dt1, dt2), ee);
//            }
//        }
        
        boolean b = true;

        try {
            switch (exp.oper()) {
                                
                case IN:
                    return in(dt1, dt2);
                    
                case EQ:
                    b = dt1.equalsWE(dt2);
                    break;               
                case NEQ:
                    b = !dt1.equalsWE(dt2);
                    break;
                case LT:
                    b = dt1.less(dt2);
                    break;
                case LE:
                    b = dt1.lessOrEqual(dt2);
                    break;
                case GE:
                    b = dt1.greaterOrEqual(dt2);
                    break;
                case GT:
                    b = dt1.greater(dt2);
                    break;
                    
                case CONT:
                    b = dt1.contains(dt2);
                    break;
                case START:
                    b = dt1.startsWith(dt2);
                    break;

                case PLUS:
                    if (SPARQLCompliant) {
                        if (!(dt1.isNumber() && dt2.isNumber())) {
                            return null;
                        }
                    }
                    return dt1.plus(dt2);

                case MINUS:
                    return dt1.minus(dt2);
                case MULT:
                    return dt1.mult(dt2);
                case DIV:
                    try {
                        return dt1.div(dt2);
                    } catch (java.lang.ArithmeticException e) {
                        return null;
                    }

                default:
                    return null;

            }
        } catch (CoreseDatatypeException e) {
            return null;
        }

        return (b) ? TRUE : FALSE;
    }
    
    @Override
    public IDatatype function(Expr exp, Environment env, Producer p) {

        switch (exp.oper()) {

             case CONCAT:
             case STL_CONCAT:
             //case XT_CONCAT:
                return concat(exp, env, p);
                 
            case NUMBER:
                return getValue(env.count());
                           
            case RANDOM:
                return rand(); 

            case NOW:
                return now();
                
            case BNODE:
                return bnode();

            case PATHNODE:
                return pathNode(env);

            case FUUID:
                return uuid();

            case STRUUID:
                return struuid();
                
            case FOR:
                return loop(exp, env, p);
                
            case SEQUENCE:
                return sequence(exp, env, p);
                              
            case XT_MAPPING:
                // use case: aggregate(xt:mapping())
                return DatatypeMap.createObject(env);
                
            case XT_GRAPH:
                return DatatypeMap.createObject(p.getGraph());
                
            case XT_QUERY:
                return DatatypeMap.createObject(env.getQuery());
                
            case XT_METADATA:
                ASTQuery ast = (ASTQuery) env.getQuery().getAST();
                if (ast.getMetadata() == null){
                    return null;
                }
                return DatatypeMap.createObject(ast.getMetadata());
                
            case XT_FROM:
            case XT_NAMED:
                return dataset(exp, env, p);
                
            case XT_AST:
                return DatatypeMap.createObject(env.getQuery().getAST());

            default:
                if (plugin != null) {
                    return  plugin.function(exp, env, p);
                }
        }

        return null;
    }
    
    public IDatatype power(IDatatype dt1, IDatatype dt2){
        return getValue(Math.pow(dt1.doubleValue(), dt2.doubleValue()));
    }


    public IDatatype struuid() {
        UUID uuid = UUID.randomUUID();
        String str = uuid.toString();
        return DatatypeMap.newLiteral(str);
    }
    
    public IDatatype now(){
        return DatatypeMap.newDate();
    }

    
    public IDatatype rand(){
        return getValue(Math.random());
    }

    public IDatatype uuid() {
        UUID uuid = UUID.randomUUID();
        String str = URN_UUID + uuid;
        return DatatypeMap.createResource(str);
    }

    @Override
    public IDatatype function(Expr exp, Environment env, Producer p, Object o1) {

        IDatatype dt = (IDatatype) o1;

        switch (exp.oper()) {

            case ISURI:
                return isURI(dt); 

            case ISLITERAL:
                return isLiteral(dt);

            case ISBLANK:
                return isBlank(dt);

            case ISSKOLEM:
                return (dt.isSkolem()) ? TRUE : FALSE;

            case ISNUMERIC:
                return isNumeric(dt);
                
            case ISWELLFORMED:
                return isWellFormed(dt);
                
            case NOT:
                return not(dt);

            case URI:
                return uri(exp, dt);

            case STR:
                return str(dt);
                
            case XSDSTRING:
                return string(dt);    

            case STRLEN:
                return strlen(dt);

            case UCASE:
                return ucase(dt);

            case LCASE:
                return lcase(dt);

            case ENCODE:
                return encode_for_uri(dt);

            case ABS:
                return abs(dt);

            case FLOOR:
                return floor(dt);

            case ROUND:
                return round(dt);

            case CEILING:
                return ceil(dt);

            case TIMEZONE:
                return timezone(dt);

            case TZ:
                return tz(dt);

            case YEAR: return year(dt);
            case MONTH: return month(dt);
            case DAY:   return day(dt);
            case HOURS: return hours(dt);
            case MINUTES: return minutes(dt);
            case SECONDS: return seconds(dt);

            case HASH:
                return hash(exp, dt);

            case LANG:
                return lang(dt);

            case BNODE:
                return bnode(dt, env);

            case DATATYPE:
                return datatype(dt);
                
             case CAST: // xsd:string(?x)
                return cast(dt, exp.getLabel());        

            case DISPLAY:
               return display(exp, dt, null);
                                        
            case SLICE:
                return slice(env, dt);  
                
            case RETURN:
                return result(dt);
                
            case XT_COUNT:
                return size(dt);
                
            case ISLIST:
                return isList(dt);
                
            case XT_FIRST:
                return first(dt);
                
            case XT_REST:
                return rest(dt);
                               
            case XT_REVERSE:
                 return reverse(dt);
                 
            case XT_TOLIST:
                return dt.toList();
                
            case XT_SORT:
                return sort(dt);
                
            case XT_CONTENT: 
                return content(dt);
                
            case XT_DATATYPE:
                return xt_datatype(dt);
                
            case XT_KIND:
                return xt_kind(dt);
                                
            case XT_REJECT:
                return reject(env, dt); 
                                                                                                 
            default:
                if (plugin != null) {
                    return plugin.function(exp, env, p, dt);
                }

        }
        return null;
    }
    
  
    IDatatype xt_display(Environment env, Producer p, IDatatype[] param) {
       return xt_display(env, p, param, true, true);
    }
    
    IDatatype xt_display(Environment env, Producer p, IDatatype[] param, boolean turtle, boolean content) {
        for (IDatatype dt : param) {
            xt_display(env, p, dt, turtle, content);
            System.out.print(" ");
        }
        System.out.println();
        return TRUE;
    }
    
    IDatatype xt_display(Environment env, Producer p, IDatatype dt){  
        return xt_display(env, p, dt, true, true);
    }      

    IDatatype xt_display(Environment env, Producer p, IDatatype dt, boolean turtle, boolean content) {
        IDatatype res = method(USER_DISPLAY, null, array(dt), env, p);
        if (res == null) {
            res = dt.display();
        } 
        if (turtle){
            System.out.println(res);
        }
        else {
            System.out.println(res.stringValue());
        }
        return dt;
    }
    
   
    // skip n first elements
    IDatatype[] copy(IDatatype [] param, int n){
        IDatatype [] arg = new IDatatype [param.length - n];
        System.arraycopy(param, n, arg, 0, arg.length);
        return arg;
    }
    
         
    IDatatype method(IDatatype name, IDatatype[] param, Environment env, Producer p) {
        return method(name.stringValue(), null, param, env, p);
    }
          
     /**
     * Try to execute a method name in the namespace of the generalized datatype URI
     * http://ns.inria.fr/sparql-datatype/triple#display(?x)
     * URI:   dt:uri#name
     * bnode: dt:bnode#name
     * literal: dt:datatype#name or dt:literal#name
     */   
    IDatatype method(String name, IDatatype type, IDatatype[] param, Environment env, Producer p) {   
        if (env == null){
            return null;
        }       
        Expr exp = eval.getDefineMethod(env, name, type, param);
        if (exp == null) {
            return null;
        }
        else {
           return (IDatatype) eval.eval(exp.getFunction(), env, p, param, exp); 
        }       
    }
    
              
    IDatatype xt_datatype(IDatatype dt){
        if (dt.isLiteral()) return dt.getDatatype();
        return xt_kind(dt);
    }
          
    IDatatype xt_kind(IDatatype dt){
        if (dt.isLiteral()) return DatatypeMap.LITERAL_DATATYPE;
        if (dt.isURI()) return DatatypeMap.URI_DATATYPE;
        return DatatypeMap.BNODE_DATATYPE;
    }
        
    IDatatype content(IDatatype dt){
        if (dt.getObject() != null){
            return DatatypeMap.newInstance(dt.getContent());
        }
        return dt;
    }
        
    public IDatatype display(IDatatype dt1, IDatatype dt2) {
        System.out.println(dt1 + " " + dt2);
        return TRUE;
    }
      
    public IDatatype display(IDatatype dt) {
        return xt_display(null, null, dt);
    }
       
    IDatatype display(Expr exp, IDatatype dt, IDatatype arg) {
        if (dt.getObject() != null) {
            System.out.println(exp.getExp(0) + " = " + dt.getObject());
        }
        else  if (arg == null){
            System.out.println(exp.getExp(0) + " = " + dt);
        }
        else {
            if (! arg.equals(FALSE)){
                System.out.println(arg.stringValue());
            }
            System.out.println(dt.stringValue());
            System.out.println();
        }       
        return TRUE;
    }

    @Override
    public IDatatype function(Expr exp, Environment env, Producer p, Object o1, Object o2) {
        IDatatype dt1 = (IDatatype) o1;
        IDatatype dt2 = (IDatatype) o2;
        boolean b;

        switch (exp.oper()) {
            
            case POWER:
               return power(dt1, dt2); 
                
            case PLUS:
            case MINUS:
            case MULT:
            case DIV:
            case EQ:
            case NEQ:
            case LT:
            case LE:
            case GT:
            case GE:
                return term(exp, env, p, o1, o2);
                
            case OR:
                return or(dt1, dt2);
            case AND:
                return and(dt1, dt2);
            
             case DISPLAY:
               return display(exp, dt1, dt2);

            case CONT:
                return getValue(dt1.contains(dt2));
                
            case SAMETERM:
                return sameTerm(dt1, dt2);

            case CONTAINS:
               return contains(dt1, dt2);

            case STARTS:
              return strstarts(dt1, dt2);

            case ENDS:
                return strends(dt1, dt2);
                
            case SUBSTR:
                return substr(dt1, dt2);

            case STRBEFORE:
                return strbefore(dt1, dt2);

            case STRAFTER:
                return strafter(dt1, dt2);

            case LANGMATCH:
                return langMatches(dt1, dt2);

            case STRDT:
                return strdt(dt1, dt2);

            case STRLANG:
                return strlang(dt1, dt2);
                
            case CAST: // cast(?x, xsd:string)
                //return cast(dt1, dt2);    
                return cast(dt1, exp.getLabel());    
                
            case REGEX: 
               return regex(exp, dt1, dt2, null);
            
            case XPATH: {
                // xpath(?g, '/book/title')
                Processor proc = getProcessor(exp);
                proc.setResolver(new VariableResolverImpl(env));
                IDatatype res = proc.xpath(dt1, dt2);
                return res;
            }

            case EXTEQUAL: {
                boolean bb = StringHelper.equalsIgnoreCaseAndAccent(dt1.getLabel(), dt2.getLabel());
                return (bb) ? TRUE : FALSE;
            }

            case EXTCONT: {
                boolean bb = StringHelper.containsWordIgnoreCaseAndAccent(dt1.getLabel(), dt2.getLabel());
                return (bb) ? TRUE : FALSE;
            }
                
            case XT_MEMBER: 
                return member(dt1, dt2);
                
            case XT_CONS:
                return cons(dt1, dt2);
                
            case XT_ADD:
                return add(dt1, dt2);    
                
            case XT_APPEND:
                return append(dt1, dt2);
                
            case XT_MERGE:
                return merge(dt1, dt2);    
                
            case XT_GET:
                return get(dt1, dt2);
                                               
            default:
                if (plugin != null) {
                    return  plugin.function(exp, env, p, dt1, dt2);
                }
        }

        return null;
    }

    @Override
    public IDatatype eval(Expr exp, Environment env, Producer p, Object[] args) {
        IDatatype[] param =  (IDatatype[]) args;
        switch (exp.oper()) {

            case EXTERNAL:
                return external(exp, env, p, param);                
                
            case CUSTOM:
                return custom.eval(exp, env, p, args);

            case KGRAM:
            case EXTERN:
            case PROCESS:
                return (IDatatype) plugin.eval(exp, env, p, param);

            case DEBUG:
                if (el == null) {
                    el = EvalListener.create();
                    env.getEventManager().addEventListener(el);
                }
                int i = 0;
                for (IDatatype arg : param) {
                    Event e = EventImpl.create(Event.FILTER, exp.getExp(i++), arg);
                    env.getEventManager().send(e);
                }
                return TRUE;
                           
            case STL_AND:
                return and(param); 
                
            case XT_SET: 
                return set(param[0], param[1], param[2]);
                
            case XT_ADD: 
                return add(param[0], param[1], param[2]); 
                
            case XT_SWAP: 
                return swap(param[0], param[1], param[2]); 
                
            case IOTA:
                return iota(param);
                
            case XT_ITERATE:
                return iterate(param);    
                
            case XT_GEN_GET:
                return gget(param[0], param[1], param[2]);
                                               
            case LIST:
                return DatatypeMap.list(param);  
                                           
            case CONCAT:
            case STL_CONCAT:
            //case XT_CONCAT:            
                return concat(exp, env, p, param);
                
            case XT_DISPLAY:
                // turtle + content if gdisplay
               return xt_display(env, p, param, true, (!exp.getLabel().equals(Processor.XT_DISPLAY))); 
                            
            case XT_PRINT:
                // string + content if gprint
                return xt_display(env, p, param, false, (!exp.getLabel().equals(Processor.XT_PRINT)));
                
            case XT_METHOD:
                return method(param[0].stringValue(), null, copy(param, 1), env, p) ;  
                
            case XT_METHOD_TYPE:
                return method(param[0].stringValue(), param[1], copy(param, 2), env, p) ;  
                    
            
        }


        boolean b = true;
        IDatatype dt = null, dt1 = null;
        if (param.length > 0) {
            dt =  param[0];
        }
        if (param.length > 1) {
            dt1 =  param[1];
        }

        switch (exp.oper()) {

            case REGEX:
                return regex(exp, dt, dt1, param[2]);               

            case SUBSTR:               
                return substr(dt, dt1, (param.length > 2) ? param[2] : null);

            case STRREPLACE:

                if (param.length < 3) {
                    return null;
                }
                
                if (! isStringLiteral(dt)) {
                    return null;
                }
                
                return strreplace(exp, dt, dt1, param[2], (param.length == 4) ? param[3] : null);

                //TODO: fix it, return IDatatype and check
//            case SQL:
//                // return ResultSet
//                return sql(exp, env, param);

            default:
                if (plugin != null) {
                    return  plugin.eval(exp, env, p, param);
                }
        }

        return null;
    }
    
    IDatatype external(Expr exp, Environment env, Producer p, IDatatype[] param) {
        // user defined function with prefix/namespace
        // function://package.className
        Processor proc = getProcessor(exp);
        if (! proc.isCorrect()){
            return null;
        }       
        proc.compile();
        return eval(proc, env, param, p);
        //return proc.eval(param);
    }
    
    Processor getProcessor(Expr exp) {
        return ((Term) exp).getProcessor();
    }
     
     /**
	 * Eval external method
	 */
    public IDatatype eval(Processor proc, Environment env, IDatatype[] args, Producer p) {
        String name = proc.getMethod().getName();
        try {
            Object obj = proc.getProcessor();
            if (obj instanceof ProxyImpl){
                ProxyImpl pi = (ProxyImpl) obj;
                pi.setEvaluator(eval);
                pi.setPlugin(plugin);
                pi.setProducer(p);
                pi.setEnvironment(env);
                plugin.setProducer(p);
            }
            return (IDatatype) proc.getMethod().invoke(obj, args);
        } catch (IllegalArgumentException e) {
           trace(e, "eval", name, args);
        } catch (IllegalAccessException e) {
            trace(e, "eval", name, args);
        } catch (InvocationTargetException e) {
           trace(e, "eval", name, args);
        } catch (NullPointerException e) {
           trace(e, "eval", name, args); 
        }
        return null;
    }
    
    String javaName(IDatatype dt){
        return NSManager.nstrip(dt.getLabel());
    }
    
    /**
     * LDScript Java compiler
     */
    public IDatatype funcall(IDatatype fun, IDatatype... ldt) {
        String name = javaName(fun);
        try {          
            Class<IDatatype>[] aclasses = new Class[ldt.length];
            for (int i = 0; i < aclasses.length; i++) {
                aclasses[i] = IDatatype.class;
            }
            Method m = this.getClass().getMethod(name, aclasses);
            return (IDatatype) m.invoke(this, ldt);
        } catch (SecurityException e) {
           
        } catch (NoSuchMethodException e) {
            trace(e, "funcall", name, ldt);
        } catch (IllegalArgumentException e) {
            trace(e, "funcall", name, ldt);       
        } catch (IllegalAccessException e) {
            trace(e, "funcall", name, ldt);     
        } catch (InvocationTargetException e) {
            trace(e, "funcall", name, ldt);  
        }
        return null;
    }
    
    void trace(Exception e, String title, String name, IDatatype[] ldt){
        String str = "";
        for (IDatatype dt : ldt) {
            str += dt + " ";
        }
        logger.warn(e);
        logger.warn(title + " "+ name + " " + str);  
    }
    
    /**
     * LDScript Java compiler
     * ldt[0] is a list
     */
    public IDatatype map(IDatatype fun, IDatatype... ldt) {
        String name = javaName(fun);
        try {          
            Class<IDatatype>[] aclasses = new Class[ldt.length];
            for (int i = 0; i < aclasses.length; i++) {
                aclasses[i] = IDatatype.class;
            }
            Method m = this.getClass().getMethod(name, aclasses);
            IDatatype list = ldt[0];
            for (IDatatype dt : list.getValueList()){
                ldt[0] = dt;
                m.invoke(this, ldt);
            }
        } catch (SecurityException e) {
            trace(e, "map", name, ldt);
         } 
        catch (NoSuchMethodException e) {
            trace(e, "map", name, ldt);
        } catch (IllegalArgumentException e) {
            trace(e, "map", name, ldt);
        } catch (IllegalAccessException e) {
            trace(e, "map", name, ldt);
        } catch (InvocationTargetException e) {
            trace(e, "map", name, ldt);
        }
        return null;
    }
    
     public IDatatype maplist(IDatatype fun, IDatatype... ldt) {
        String name = javaName(fun);
        try {          
            Class<IDatatype>[] aclasses = new Class[ldt.length];
            for (int i = 0; i < aclasses.length; i++) {
                aclasses[i] = IDatatype.class;
            }
            Method m = this.getClass().getMethod(name, aclasses);
            IDatatype list = ldt[0];
            ArrayList<IDatatype> res = new ArrayList<IDatatype>();
            for (IDatatype dt : list.getValues()){
                ldt[0] = dt;
                IDatatype obj = (IDatatype) m.invoke(this, ldt);
                if (obj != null){
                  res.add(obj);
                }
            }
            return DatatypeMap.newInstance(res);
        } catch (SecurityException e) {
            trace(e, "maplist", name, ldt);
        } catch (NoSuchMethodException e) {
           trace(e, "maplist", name, ldt);
        } catch (IllegalArgumentException e) {
           trace(e, "maplist", name, ldt);
        } catch (IllegalAccessException e) {
            trace(e, "maplist", name, ldt);
        } catch (InvocationTargetException e) {
            trace(e, "maplist", name, ldt);
        }
        return null;
    }
    
     /**
     * @return the producer
     */
    public Producer getProducer() {
        return producer;
    }

    /**
     * @param producer the producer to set
     */
    @Override
    public void setProducer(Producer producer) {
        this.producer = producer;
    }



    boolean isStringLiteral(IDatatype dt) {
        return !SPARQLCompliant || DatatypeMap.isStringLiteral(dt);
    }
    
    public IDatatype encode_for_uri(IDatatype dt) {
        String str = encodeForUri(dt.getLabel());
        return DatatypeMap.newLiteral(str);
    }

    public String encodeForUri(String str) {

        StringBuilder sb = new StringBuilder(2 * str.length());

        for (int i = 0; i < str.length(); i++) {
            
            char c = str.charAt(i);

            if (stdChar(c)) {
                sb.append(c);
            } else {
                try {
                    byte[] bytes = Character.toString(c).getBytes("UTF-8");

                    for (byte b : bytes) {
                        sb.append("%");

                        char cc = (char) (b & 0xFF);

                        String hexa = Integer.toHexString(cc).toUpperCase();

                        if (hexa.length() == 1) {
                            sb.append("0");
                        }

                        sb.append(hexa);
                    }

                } catch (UnsupportedEncodingException e) {
                }
            }
        }

        return sb.toString();
    }

    boolean stdChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' 
                || c == '-' || c == '.' || c == '_' || c == '~';
    }
    
    public IDatatype strstarts(IDatatype dt1, IDatatype dt2) {
        if (!compatible(dt1, dt2)) {
            return null;
        }
        boolean b = dt1.startsWith(dt2);
        return (b) ? TRUE : FALSE;
    }
    
    public IDatatype strends(IDatatype dt1, IDatatype dt2) {
        if (!compatible(dt1, dt2)) {
            return null;
        }
        boolean b = dt1.getLabel().endsWith(dt2.getLabel());
        return (b) ? TRUE : FALSE;
    }
    
    public IDatatype contains(IDatatype dt1, IDatatype dt2) {
        if (!compatible(dt1, dt2)) {
            return null;
        }
        boolean b = dt1.getLabel().contains(dt2.getLabel());
        return (b) ? TRUE : FALSE;
    }
    
    public IDatatype sameTerm(IDatatype dt1, IDatatype dt2){
        return getValue(dt1.sameTerm(dt2));
    }
    
    IDatatype cast(IDatatype dt, IDatatype dt1, IDatatype dt2){
        //return dt.cast(dt1, dt2);
        return dt.cast(dt1);
    }
    
    IDatatype cast(IDatatype dt, IDatatype dt1){
        return dt.cast(dt1);
    }
    
    IDatatype cast(IDatatype dt, String dt1){
        return dt.cast(dt1);
    }
    
    public IDatatype substr(IDatatype dt, IDatatype ind) {
        return substr(dt, ind, null);
    }
    
    // first index is 1
    public IDatatype substr(IDatatype dt, IDatatype ind, IDatatype len) {
        String str = dt.getLabel();
        int start = ind.intValue();
        start = Math.max(start - 1, 0);
        int end = str.length();
        if (len != null) {
            end = len.intValue();
        }
        end = start + end;
        if (end > str.length()) {
            end = str.length();
        }
        str = str.substring(start, end);
        return getValue(dt, str);
    }

    // return a Literal (not a xsd:string)
    public IDatatype str(IDatatype dt) {
        return DatatypeMap.newLiteral(dt.getLabel());
    }
    
    public IDatatype string(IDatatype dt) {
        return DatatypeMap.newInstance(dt.getLabel());
    }
          
    public IDatatype strlen(IDatatype dt) {
        return getValue(dt.getLabel().length());
    }

    public IDatatype ucase(IDatatype dt) {
        String str = dt.getLabel().toUpperCase();
        return getValue(dt, str);
    }

    public IDatatype lcase(IDatatype dt) {
        String str = dt.getLabel().toLowerCase();
        return getValue(dt, str);
    }
    
    public IDatatype isBlank(IDatatype dt){
        return getValue(dt.isBlank());
    }
    
    public IDatatype isURI(IDatatype dt){
        return getValue(dt.isURI());
    }
    
    public IDatatype isIRI(IDatatype dt){
        return isURI(dt);
    }
    
    public IDatatype isLiteral(IDatatype dt){
        return getValue(dt.isLiteral());
    }
    
    public IDatatype isNumeric(IDatatype dt){
        return getValue(dt.isNumber());
    }
    
   public IDatatype iri(IDatatype dt) {
       return uri(null, dt);
   }
   
   public IDatatype uri(IDatatype dt) {
       return uri(null, dt);
   }
    
     IDatatype uri(Expr exp, IDatatype dt) {
        if (dt.isURI()) {
            return dt;
        }
        String label = dt.getLabel();
        if (exp != null && exp.getModality() != null && !isURI(label)) {
            // with base
            return DatatypeMap.newResource(exp.getModality() + label);
        } else {
            return DatatypeMap.newResource(label);
        }
    }

    boolean isURI(String str) {
        return str.matches("[a-zA-Z0-9]+://.*");
    }

    /**
     * Compatibility for strbefore (no lang or same lang)
     */
    boolean compatible(IDatatype dt1, IDatatype dt2) {
        if (!dt1.hasLang()) {
            return !dt2.hasLang();
        } else if (!dt2.hasLang()) {
            return true;
        } else {
            return dt1.getLang().equals(dt2.getLang());
        }
    }

    public IDatatype strbefore(IDatatype dt1, IDatatype dt2) {

        if (!isStringLiteral(dt1) || !compatible(dt1, dt2)) {
            return null;
        }

        int index = dt1.getLabel().indexOf(dt2.getLabel());
        String str = "";
        if (index != -1) {
            str = dt1.getLabel().substring(0, index);
        }
        return result(str, dt1, dt2);
    }

    public IDatatype strafter(IDatatype dt1, IDatatype dt2) {
        if (!isStringLiteral(dt1) || !compatible(dt1, dt2)) {
            return null;
        }

        int index = dt1.getLabel().indexOf(dt2.getLabel());
        String str = "";
        if (index != -1) {
            str = dt1.getLabel().substring(index + dt2.getLabel().length());
        }
        return result(str, dt1, dt2);
    }
    
    IDatatype strreplace(Expr exp, IDatatype dt1, IDatatype dt2, IDatatype dt3, IDatatype dt4) {
        Processor p = getProcessor(exp);
        String str = p.replace(dt1.stringValue(), dt2.stringValue(), dt3.stringValue(), (dt4 == null) ? null : dt4.stringValue());
        return result(str, dt1, dt3);
    }
    
    public IDatatype replace(IDatatype dt1, IDatatype dt2, IDatatype dt3) {
        return replace(dt1, dt2, dt3, null);
    }
    
   public IDatatype replace(IDatatype dt1, IDatatype dt2, IDatatype dt3, IDatatype dt4) {
        Processor p = Processor.create();
        String str = p.replace(dt1.stringValue(), dt2.stringValue(), dt3.stringValue(), (dt4 == null) ? null : dt4.stringValue());
        return result(str, dt1, dt3);
    }

    IDatatype result(String str, IDatatype dt1, IDatatype dt2) {
        if (dt1.hasLang() && str != "") {
            return DatatypeMap.createLiteral(str, null, dt1.getLang());
        } else if (DatatypeMap.isString(dt1)) {
            return getValue(str);
        }
        return DatatypeMap.newLiteral(str);
    }
    
    IDatatype slice (Environment env, IDatatype dt){
        env.getQuery().setSlice(dt.intValue());
        return TRUE;
    }
    
    /**
     * TODO: lang 
     */
    public IDatatype concat(IDatatype... dts){
        if (dts.length == 0){
            return getValue("");
        }
        StringBuilder sb = new StringBuilder();
        boolean hasLang = false;
        String lang = null;
        if (dts[0].hasLang()){
            lang = dts[0].getLang();
            hasLang = true;
        }
        
        for (IDatatype dt : dts){   
            if (hasLang){
                if (!(dt.hasLang() && dt.getLang().equals(lang))){
                    hasLang = false;
                }
            }          
            sb.append(dt.stringValue());
        }
        return (hasLang) ? DatatypeMap.newInstance(sb.toString(), null, lang) : getValue(sb.toString()) ;
    }
    
    public IDatatype bound(IDatatype dt){
        return getValue(dt != null && DatatypeMap.isBound(dt));
    }
    
    public IDatatype coalesce(IDatatype... dts){
        for (IDatatype dt : dts){
            if (dt != null){
                return dt;
            }
        }
        return null;
    }

    /**
     * literals with same lang return literal@lang all strings return string
     * else return literal error if not literal or string
     */
      IDatatype concat(Expr exp, Environment env, Producer p) {
            return concat(exp, env, p, null);
      }

    /**
     * std usage: lval is null, evaluate exp
     * lval = list of values in this use case:
     * apply(concat(), maplist(st:fun(?x) , xt:list(...)))
     * TODO: lval is deprecated ?
     * 
     */
    IDatatype concat(Expr exp, Environment env, Producer p, IDatatype[] lval) {
        String str = "";
        String lang = null;

        if (exp.arity() == 0 && lval == null) {
            return EMPTY;
        }
        int length = 0;
        if (lval != null){
            length = lval.length;
        }
        
        // when template st:concat()
        // st:number() is not evaluated now
        // it will be evaluated by template group_concat aggregate
        // return future(concat(str, st:number(), str))
        boolean isSTLConcat = exp.oper() == STL_CONCAT;

        StringBuilder sb = new StringBuilder();
        ArrayList<Object> list = null;
        boolean ok = true, hasLang = false, isString = true;
        IDatatype dt = null;
        int i = 0;
        List<Expr> argList = exp.getExpList();
        for (int j = 0; j < ((length > 0) ? length : argList.size()); ) {

            if (lval == null) {
                Expr ee = argList.get(j);

                if (isSTLConcat && isFuture(ee)) {
                    // create a future
                    if (list == null) {
                        list = new ArrayList<Object>();
                    }
                    if (sb.length() > 0) {
                        list.add(result(env, sb, isString, (ok && lang != null) ? lang : null, isSTLConcat));
                        sb = new StringBuilder();
                    }
                    list.add(ee);
                    // Do not touch to j++ (see below int k = j;)   
                    j++;
                    continue;
                }
                dt = (IDatatype) eval.eval(ee, env, p);
            } else {
                dt =  lval[j];
            }
            // Do not touch to j++ (see below int k = j;)             
            j++;
            
            if (dt == null){
                return null;
            }

            if (isSTLConcat && dt.isFuture()){
                // result of ee is a Future
                // use case: ee = box { e1 st:number() e2 }
                // ee = st:concat(e1, st:number(), e2) 
                // dt = Future(concat(e1, st:number(), e2))
                // insert Future arg list (e1, st:number(), e2) into current argList
                // arg list is inserted after ee (indice j is already  set to j++)
                ArrayList<Expr> el = new ArrayList(argList.size());
                el.addAll(argList);
                Expr future = (Expr) dt.getObject();
                int k = j;

                for (Expr arg : future.getExpList()){
                    el.add(k++, arg);
                }
                argList = el;
                continue; 
            }
            
            if (i == 0 && dt.hasLang()) {
                hasLang = true;
                lang = dt.getLang();
            }
            i++;
            
            if (!isStringLiteral(dt)) {
                return null;
            }

            if (dt.getStringBuilder() != null) {
                sb.append(dt.getStringBuilder());
            } else {
                sb.append(dt.getLabel());
            }
          
            if (ok) {
                if (hasLang) {
                    if (!(dt.hasLang() && dt.getLang().equals(lang))) {
                        ok = false;
                    }
                } else if (dt.hasLang()) {
                    ok = false;
                }

                if (!DatatypeMap.isString(dt)) {
                    isString = false;
                }
            }
            
        }
            
        if (list != null){
            // return ?out = future(concat(str, st:number(), str)
            // will be evaluated by template group_concat(?out) aggregate
            if (sb.length()>0){
                list.add(result(env, sb, isString, (ok && lang != null)?lang:null, isSTLConcat));
            }  
            Expr e = plugin.createFunction(Processor.CONCAT, list, env);
            IDatatype res = DatatypeMap.createFuture(e);
            return res;
        }
        
        return result(env, sb, isString, (ok && lang != null)?lang:null, isSTLConcat);
    }
     
    boolean isFuture(Expr e){
        if  (e.oper() == STL_NUMBER 
                //|| e.oper() == STL_FUTURE
                ){
            return true;
        }
        if  (e.oper() == CONCAT || e.oper() == STL_CONCAT){
            // use case:  group { st:number() } box { st:number() }
            return false;
        }
        if (e.arity() > 0){
            for (Expr a : e.getExpList()){
                if (isFuture(a)){
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     *     
     */
    IDatatype result(Environment env, StringBuilder sb, boolean isString, String lang, boolean isSTL){
        if (lang != null) {
            return DatatypeMap.createLiteral(sb.toString(), null, lang);
        } else if (isString) {
            if (isSTL){
                return  plugin.getBufferedValue(sb, env);
            }
            else {
                return DatatypeMap.newStringBuilder(sb);
            }
        } else {
            return DatatypeMap.newLiteral(sb.toString());
        }
    }
    
    IDatatype and(IDatatype[] val){
        for (IDatatype dt : val){
            if (dt == null){
                return null;
            }
            
            try {
                if (! dt.isTrue()){
                    return DatatypeMap.FALSE;
                }
            } catch (CoreseDatatypeException ex) {
                return null;
            }
            
        }
        return DatatypeMap.TRUE;
    }
    
    public IDatatype datatype(IDatatype dt){
        return dt.getDatatype();
    }


    /**
     * same bnode for same label in same solution, different otherwise
     */
    IDatatype bnode(IDatatype dt, Environment env) {
        Map map = env.getMap();
        IDatatype bn = (IDatatype) map.get(dt.getLabel());
        if (bn == null) {
            bn = bnode();
            map.put(dt.getLabel(), bn);
        } else {
        }

        return bn;
    }
    
    public IDatatype lang(IDatatype dt) {
        return dt.getDataLang();
    }

    public IDatatype bnode() {
        return DatatypeMap.createBlank();
    }
    
    public IDatatype bnode(IDatatype dt) {
        return DatatypeMap.createBlank();
    }
       
    public IDatatype floor(IDatatype dt) {
        return getValue(Math.floor(dt.doubleValue()), dt.getDatatypeURI());
    }

    public IDatatype round(IDatatype dt) {
        return getValue(Math.round(dt.doubleValue()), dt.getDatatypeURI());
    }

    public IDatatype ceil(IDatatype dt) {
        return getValue(Math.ceil(dt.doubleValue()), dt.getDatatypeURI());
    }

    public IDatatype timezone(IDatatype dt) {
        return DatatypeMap.getTimezone(dt);
    }        

    public IDatatype tz(IDatatype dt) {
        return DatatypeMap.getTZ(dt);
    }         
           
//    IDatatype time(Expr exp, IDatatype dt) {
//        if (dt.getDatatypeURI().equals(RDF.xsddate)
//                || dt.getDatatypeURI().equals(RDF.xsddateTime)) {
//
//            switch (exp.oper()) {
//
//                case YEAR:
//                    return DatatypeMap.getYear(dt);
//                case MONTH:
//                    return DatatypeMap.getMonth(dt);
//                case DAY:
//                    return DatatypeMap.getDay(dt);
//
//                case HOURS:
//                    return DatatypeMap.getHour(dt);
//                case MINUTES:
//                    return DatatypeMap.getMinute(dt);
//                case SECONDS:
//                    return DatatypeMap.getSecond(dt);
//            }
//        }
//
//        return null;
//    }
    
    boolean isDate(IDatatype dt){
        return dt.isDate();
    }
    
    public IDatatype year(IDatatype dt){
        if (! isDate(dt)){
            return null;
        }
        return DatatypeMap.getYear(dt);
    } 
    
    public IDatatype month(IDatatype dt){
        if (! isDate(dt)){
            return null;
        }
        return DatatypeMap.getMonth(dt);
    }
    
    public IDatatype day(IDatatype dt){
        if (! isDate(dt)){
            return null;
        }
        return DatatypeMap.getDay(dt);
    }
    
    public IDatatype hours(IDatatype dt){
        if (! isDate(dt)){
            return null;
        }
        return DatatypeMap.getHour(dt);
    } 
    
    public IDatatype minutes(IDatatype dt){
        if (! isDate(dt)){
            return null;
        }
        return DatatypeMap.getMinute(dt);
    } 
    
    public IDatatype seconds(IDatatype dt){
        if (! isDate(dt)){
            return null;
        }
        return DatatypeMap.getSecond(dt);
    }

    IDatatype hash(Expr exp, IDatatype dt) {
        String name = exp.getModality();
        String str = dt.getLabel();
        String res = new Hash(name).hash(str);
        if (res == null) {
            return null;
        }
        return DatatypeMap.newLiteral(res);
    }
    
    public IDatatype hash(IDatatype name, IDatatype dt) {
        String res = new Hash(name.stringValue()).hash(dt.stringValue());
        if (res == null) {
            return null;
        }
        return DatatypeMap.newLiteral(res);
    }


    public IDatatype abs(IDatatype dt) {
        switch (dt.getCode()){
            case IDatatype.INTEGER: return DatatypeMap.newInteger(Math.abs(dt.longValue()));
            default:                return getValue(Math.abs(dt.doubleValue()));
        }
    }

    /**
     * sum(?x)
     */
    @Override
    public IDatatype aggregate(Expr exp, Environment env, Producer p, Node qNode) {
        //exp = decode(exp, env, p);
        Walker walk = new Walker(exp, qNode, this, env, p);

        // apply the aggregate on current group Mapping, 
        env.aggregate(walk, p, exp.getFilter());

        IDatatype res = (IDatatype) walk.getResult(env, p);
        return res;
    }
    
    @Override
    public Expr decode (Expr exp, Environment env, Producer p){
        switch (exp.oper()){
            case STL_AGGREGATE:
                return plugin.decode(exp, env, p);
        }
        return exp;       
    }

    /**
     * return null if value is UNDEF 
     * use case: ?y in not bound in let (?y) = select where  
     * */
    @Override
    public IDatatype getConstantValue(Node value) {
        if (value == null){
            // xt:gget() argument evaluation fail ->  do not fail, return UNDEF 
            return UNDEF;
        }
        if (value == UNDEF){
            // let (var = UNDEF) body : do not fail, do not bind var, 
            // eval body with var unbound, can be trapped with if(bound(var)
            return null;
        }
        return (IDatatype) value.getDatatypeValue();
    }

    IDatatype pathNode(Environment env) {
        Query q = env.getQuery();
        int num = q.getGlobalQuery().nbPath();
        IDatatype dt = DatatypeMap.createBlank(Query.BPATH + Integer.toString(num));
        return dt;
    }

    @Override
    public boolean isTrue(Object value) {
        IDatatype dt = (IDatatype) value;
        //if (! dt.isTrueAble()) return false;
        try {
            return dt.isTrue();
        } catch (CoreseDatatypeException e) {
            return false;
        }
    }

    @Override
    public boolean isTrueAble(Object value) {
        IDatatype dt = (IDatatype) value;
        return dt.isTrueAble();
    }

    protected IDatatype datatypeValue(Object o) {
        return (IDatatype) o;
    }

    @Override
    public IDatatype getValue(boolean b) {
        // TODO Auto-generated method stub
        if (b) {
            return TRUE;
        } else {
            return FALSE;
        }
    }
    
    @Override
    public IDatatype getValue(Object val, Object obj){
       if (val instanceof Boolean){
           Boolean b = (Boolean) val;
           IDatatype dt = DatatypeMap.newInstance(b);
          // dt.setObject(obj);
           return dt;
       }
       return null;
    }

    @Override
    public IDatatype getValue(int value) {       
        return DatatypeMap.newInstance(value);
    }
    
  
    @Override
    public IDatatype getValue(long value) {
        return DatatypeMap.newInstance(value);
    }

    @Override
    public IDatatype getValue(float value) {
        return DatatypeMap.newInstance(value);
    }

    @Override
    public IDatatype getValue(double value) {
        return DatatypeMap.newInstance(value);
    }

    @Override
    public IDatatype getValue(double value, String datatype) {
        return DatatypeMap.newInstance(value, datatype);
    }

    // return xsd:string
    @Override
    public IDatatype getValue(String value) {
        return DatatypeMap.newInstance(value);
    }

    // return rdfs:Literal or xsd:string wrt dt
    public IDatatype getValue(IDatatype dt, String value) {
        if (dt.hasLang()) {
            return DatatypeMap.createLiteral(value, null, dt.getLang());
        } else if (dt.isLiteral() && dt.getDatatype() == null) {
            return DatatypeMap.newLiteral(value);
        }
        return DatatypeMap.newInstance(value);
    }
    
    
   public IDatatype regex(IDatatype dt1, IDatatype dt2) {
       return regex(dt1, dt2, null);
    }
   
    public IDatatype regex(IDatatype dt1, IDatatype dt2, IDatatype dt3) {
        if (!isStringLiteral(dt1)) {
            return null;
        }
        Processor proc = Processor.create();
        boolean b = proc.regex(dt1.stringValue(), dt2.stringValue(), (dt3 == null) ? null : dt3.stringValue());
        return (b) ? TRUE : FALSE;
    }
    
    IDatatype regex(Expr exp, IDatatype dt1, IDatatype dt2, IDatatype dt3) {
        if (!isStringLiteral(dt1)) {
            return null;
        }
        Processor proc = getProcessor(exp);
        boolean b = proc.regex(dt1.stringValue(), dt2.stringValue(), (dt3 == null) ? null : dt3.stringValue());
        return (b) ? TRUE : FALSE;
    }
    
    public IDatatype strlang(IDatatype dt1, IDatatype dt2) {
        if (SPARQLCompliant && !DatatypeMap.isSimpleLiteral(dt1)) {
            return null;
        }
        return DatatypeMap.createLiteral(dt1.getLabel(), null, dt2.getLabel());
    }

    
    public IDatatype strdt(IDatatype dt1, IDatatype dt2) {
        if (SPARQLCompliant && !DatatypeMap.isSimpleLiteral(dt1)) {
            return null;
        }
        return DatatypeMap.newInstance(dt1.getLabel(), dt2.getLabel());
    }

    public IDatatype langMatches(IDatatype ln1, IDatatype ln2) {
        String l1 = ln1.getLabel();
        String l2 = ln2.getLabel();

        if (l2.equals("*")) {
            return getValue(l1.length() > 0);
        }
        if (l2.indexOf("-") != -1) {
            // en-us need exact match
            return getValue(l1.toLowerCase().equals(l2.toLowerCase()));
        }
        return getValue(l1.regionMatches(true, 0, l2, 0, 2));
    }

    public IDatatype self(IDatatype dt) {
        return dt;
    }

    public Object similarity(Environment env) {
        if (!(env instanceof Memory)) {
            return getValue(0);
        }
        Memory memory = (Memory) env;
        int count = 0, total = 0;

        for (Edge qEdge : memory.getQueryEdges()) {

            if (qEdge != null && qEdge.getLabel().equals(RDFTYPE)) {
                Entity edge = memory.getEdge(qEdge);
                if (edge != null) {
                    Node type = qEdge.getNode(1);
                    if (type.isConstant()) {
                        total += 1;
                        if (type.same(edge.getNode(1))) {
                            count += 1;
                        }
                    }
                }
            }
        }

        if (total == 0) {
            return getValue(1);
        } else {
            return getValue(count / total);
        }

    }

    /**
     * sql('db', 'login', 'passwd', 'query') sql('db', 'driver', 'login',
     * 'passwd', 'query') sql('db', 'driver', 'login', 'passwd', 'query', true)
     *
     * sort means list of sparql variables (sql() as (var)) must be sorted
     * according to sql variables in result
     */
    Object sql(Expr exp, Environment env, IDatatype[] args) {
        ResultSet rs;
        boolean isSort = false;
        if (args.length == 4) {
            // no driver
            rs = sql.sql(args[0], args[1], args[2], args[3]);
        } else {
            if (args.length == 6) {
                try {
                    isSort = args[5].isTrue();
                } catch (CoreseDatatypeException e) {
                }
            }
            // with driver
            rs = sql.sql(args[0], args[1], args[2], args[3], args[4]);
        }

        return new SQLResult(rs, isSort);
    }

    /**
     * ?x in (a b) ?x in (xpath())
     */
     public IDatatype in(IDatatype dt1, IDatatype dt2) {

        boolean error = false;

        if (dt2.isList()) {

            for (IDatatype dt : dt2.getValues()) {
                try {
                    if (dt1.equalsWE(dt)) {
                        return TRUE;
                    }
                } catch (CoreseDatatypeException e) {
                    error = true;
                }
            }

            if (error) {
                return null;
            }
            return FALSE;
        } else {
            try {
                if (dt1.equalsWE(dt2)) {
                    return TRUE;
                }
            } catch (CoreseDatatypeException e) {
                return null;
            }
        }

        return FALSE;
    }

    @Override
    public Expr createFunction(String name, List<Object> args, Environment env) {
        return null;    
    }
    
    @Override
    public Expr getDefine(Expr exp, Environment env, String name, int n){
        return plugin.getDefine(exp, env, name, n);
    }

    @Override
    public void start(Producer p, Environment env) {
        plugin.start(p, env);
    }
    
    @Override
     public void finish(Producer p, Environment env) {
        plugin.finish(p, env);
    }
    
    IDatatype result(IDatatype dt){
        return DatatypeMap.result(dt);
    }
    
    boolean isReturn(IDatatype dt){
        return dt == null || DatatypeMap.isResult(dt);
    }
    
     IDatatype getResultValue(IDatatype dt){
        return DatatypeMap.getResultValue(dt);
    }
     
    @Override
     public IDatatype getResultValue(Object obj){       
        return getResultValue((IDatatype) obj);
    }
     
    private IDatatype sequence(Expr exp, Environment env, Producer p) {
        IDatatype res = TRUE;
        for (Expr e : exp.getExpList()){
            res = (IDatatype) eval.eval(e, env, p);
            if (isReturn(res)){
                return res;
            }
        }
        return res;
    }

     /**
      * loop: for (var in list) { exp }
      */
    IDatatype loop(Expr loop, Environment env, Producer p){
        IDatatype list = (IDatatype) eval.eval(loop.getDefinition(), env, p);
        if (list == null){ 
            return null;
        }
        if (list.isList()){
            env.set(loop, loop.getVariable(), TRUE);
            for (IDatatype dt : list.getValues()){           
                IDatatype res = step(loop, env, p, dt);
                if (isReturn(res)){
                    env.unset(loop, loop.getVariable(), dt);            
                    return res;
                }
            }
            env.unset(loop, loop.getVariable(), TRUE);            
        }
        else { 
            env.set(loop, loop.getVariable(), TRUE);
            for (IDatatype dt : list) { 
                IDatatype res = step(loop, env, p, dt);               
                if (isReturn(res)){
                    env.unset(loop, loop.getVariable(), dt);            
                    return res;
                }
            }
            env.unset(loop, loop.getVariable(), TRUE);            
        }
        return TRUE;
    }
    
     /**
     * for (var = value) { body }
     * + see above
     */
    IDatatype step(Expr loop, Environment env, Producer p, IDatatype value){
        //env.set(loop, loop.getVariable(), value);
        env.bind(loop, loop.getVariable(), value);
        return (IDatatype) eval.eval(loop.getBody(), env, p);
        //env.unset(loop, loop.getVariable());
    }
    
    
    public IDatatype list(IDatatype... ldt){
        return DatatypeMap.newList(ldt);
    } 
   
      
     /**
      * map (xt:fun, ?x, ?l)
      * maplist (xt:fun, ?l1, ?l2)
      * maplist return list of results
      * mapselect return sublist of elements that match a boolean predicate
      * map return true
      * map on List or Loopable (IDatatype that contains e.g Mappings)
      * TODO: when getLoop() it works with only one loop
      * @return 
      */
    
    
    @Override
    public IDatatype eval(Expr exp, Environment env, Producer p, Object[] oparam, Expr function) {
        IDatatype[] param = (IDatatype[]) oparam;
        switch (exp.oper()){
            case ExprType.REDUCE: 
                if (param.length == 0) return null;
                return reduce(exp, env, p, param[0], function);
            case ExprType.MAPEVERY:
            case ExprType.MAPANY:
                return anyevery(exp, env, p, param, function);
                
            default:
                return map(exp, env, p, param, function);
        }
    }
    
    
    IDatatype map(Expr exp, Environment env, Producer p, IDatatype[] param, Expr function) {
        boolean maplist     = exp.oper() == MAPLIST; 
        boolean mapmerge    = exp.oper() == MAPMERGE; 
        boolean mapappend   = exp.oper() == MAPAPPEND; 
        boolean mapfindelem = exp.oper() == MAPFIND;
        boolean mapfindlist = exp.oper() == MAPFINDLIST;
        boolean mapfind     = mapfindelem || mapfindlist;
        boolean hasList     = maplist || mapmerge || mapappend;

        IDatatype list = null;
        IDatatype ldt = null;
        Iterator<IDatatype> loop = null ;
        boolean isList = false, isLoop = false;
        
        int k = 0;
        for (IDatatype dt : param){  
            if (dt.isList() && ! isList && ! isLoop){
                isList = true;
                list = dt;
            }
            else if (dt.isLoop()) {
                if (! isList && ! isLoop) {
                    isLoop = true;
                    ldt = dt;
                    loop = ldt.iterator();
                }
                else {
                    // list + loop || loop + loop
                    // loop.toList()
                    param[k] = dt.toList();
                }
            }
            
            k++;
        }               
        if (list == null && ldt == null){
            return null;
        }
        IDatatype[] value = new IDatatype[param.length];
        ArrayList<IDatatype> res = (hasList)     ? new ArrayList<IDatatype>() : null;
        ArrayList<IDatatype> sub = (mapfindlist) ? new ArrayList<IDatatype>() : null;
        int size = 0; 
        
        for (int i = 0;  (isList) ? i< list.size() : loop.hasNext(); i++){ 
            IDatatype elem = null;
            
            for (int j = 0; j<value.length; j++){
                IDatatype dt = param[j];
                if (dt.isList()){                   
                    /**
                     * if list size is <= i,  focus on last element of the list
                     * use case: maplist(?fun, ?list, xt:list(?lst))
                     * The second ?lst argument is itself a list and we do not want to iterate this one
                     */  
                    value[j] = (i < dt.size()) ? dt.get(i) : dt.get(dt.size()-1);
                    if (mapfind && elem == null){
                        elem = value[j];
                    }
                }
                else if (isLoop && dt.isLoop()){
                    // TODO: track several dt Loop
                    if (loop.hasNext()){
                       value[j] = loop.next(); 
                       if (mapfind && elem == null){
                         elem = value[j];
                       }
                    }
                    else {
                        return null;
                    }
                }
                else {
                    value[j] = dt;
                }
            }

            IDatatype val =  call(function, env, p, value);

            if (val == null){
                return null;
            }
            else if (hasList) {
                if (val.isList()){
                    size += val.size();
                }
                else {
                    size += 1;
                }
               res.add(val);
            }
            else if (mapfindelem && val.booleanValue()){
                return elem;
            }
            else if (mapfindlist && val.booleanValue()){
                    // select elem whose predicate is true
                    // mapselect (xt:prime, xt:iota(1, 100))
                    sub.add(elem);
            }
            
        }
        
        if (mapmerge || mapappend){
            int i = 0;
            ArrayList<IDatatype> mlist = new ArrayList<IDatatype>();
            for (IDatatype dt : res){
                if (dt.isList()){
                    for (IDatatype v : dt.getValues()){
                        add(mlist, v, mapmerge);
                    }
                }
                else {
                    add(mlist, dt, mapmerge);
                }
            }
            return DatatypeMap.createList(mlist);
        }
        else if (maplist){
            return DatatypeMap.createList(res); 
        }
        else if (mapfindlist){
            return DatatypeMap.createList(sub);
        }
        else if (mapfindelem){
            return null;
        }
        return TRUE;
    }
    
    void add(List<IDatatype> list, IDatatype dt, boolean merge){
        if (merge){
            if (! list.contains(dt)){
                list.add(dt);
            }
        }
        else {
            list.add(dt);
        }
    }
      
  
    /**
     * every (xt:fun, ?list)   
     * every (xt:fun, ?x, ?list) 
     * every (xt:fun, ?l1, ?l2) 
     * TODO: when getLoop() it works with only one loop
     * error follow SPARQ semantics of OR (any) AND (every)
     * @return 
     */
    private IDatatype anyevery(Expr exp, Environment env, Producer p, IDatatype[] param, Expr function) {
        boolean every = exp.oper() == MAPEVERY;       
        boolean any   = exp.oper() == MAPANY;       
        IDatatype list = null; 
        IDatatype ldt = null;
        Iterator<IDatatype> loop = null ;
        boolean isList = false, isLoop = false;
        
        
        int k = 0;
        for (IDatatype dt : param){  
            if (dt.isList() && ! isList && ! isLoop){
                isList = true;
                list = dt;
            }
            else if (dt.isLoop()) {
                if (! isList && ! isLoop) {
                    isLoop = true;
                    ldt = dt;
                    loop = ldt.iterator();
                }
                else {
                    // list + loop || loop + loop
                    // snd_loop.toList()
                    param[k] = dt.toList();
                }
            }
            
            k++;
        }         
        if (list == null && ldt == null){
            return null;
        }
        IDatatype[] value = new IDatatype[param.length];
        boolean error = false;      
        for (int i = 0; (isList) ? i < list.size() : loop.hasNext(); i++){ 

            for (int j = 0; j<value.length; j++){
                IDatatype dt = param[j];
                if (dt.isList()){
                    value[j] = (i < dt.size()) ? dt.get(i) : dt.get(dt.size()-1);  
                }
                else if (isLoop && dt.isLoop()){
                    if (loop.hasNext()){
                       // TODO:  track the case with several dt loop
                       value[j] = loop.next(); //(IDatatype) p.getValue(loop.next());
                    }
                    else {
                        return null;
                    }
                }
                else {
                    value[j] = dt;
                }
            }
            
            IDatatype res =  call(function, env, p, value);                   
            if (res == null){
                error = true;                
            }
            else {
                if (every) {
                    if (! res.booleanValue()){
                        return FALSE;
                    }
                }
                else if (any) {
                    // any
                    if (res.booleanValue()){
                        return TRUE;
                    }
                }
            }
        }
        if (error){
            return null;
        }
        return getValue(every);
    }
       
    /**
     * reduce(kg:plus, ?list)   
     * @return 
     */
    private IDatatype reduce(Expr exp, Environment env, Producer p, IDatatype dt, Expr function) {
        if (! dt.isList()) {
            return null;
        }
        List<IDatatype> list = dt.getValues();
        if (list.isEmpty()){
            return neutral(function, dt);
        }
        IDatatype[] value = new IDatatype[2];
        IDatatype res = list.get(0);
        value[0] = res;
        
        for (int i = 1; i < list.size(); i++) {            
            value[1] = list.get(i);  
            res =  call(function, env, p, value);   
            if (res == null) {
               return error();
            }
            value[0] = res;
        }
        return res;
    }
    
    IDatatype neutral(Expr exp, IDatatype dt){
        switch (exp.oper()){
            case OR:
                return FALSE;
                
            case AND:
                return TRUE;
                
            case CONCAT:
                return DatatypeMap.EMPTY_STRING;
                
            case PLUS:
                return DatatypeMap.ZERO;
                
            case MULT:
                return DatatypeMap.ONE; 
                
            case XT_APPEND:
            case XT_MERGE:
                return DatatypeMap.EMPTY_LIST;
                
            default: return dt;
        }
    }
         
    
    /**
     * map(fun, list)
     * reduce(fun, list)
     */
    private IDatatype call(Expr function, Environment env, Producer p, IDatatype[] values) {
       return (IDatatype) eval.eval(function.getFunction(), env, p, values, function);
    }
         
     public IDatatype or(IDatatype dt1, IDatatype dt2) {
        boolean e1 = error(dt1);
        boolean e2 = error(dt2);
        if (e1 && e2) {
            return error();
        } else if (e1) {
            return errorOr(dt2);
        } else if (e2) {
            return errorOr(dt1);
        } else {
            return getValue(dt1.booleanValue() || dt2.booleanValue());
        }
    }
     
     boolean error(IDatatype dt){
         return dt == null || ! dt.isTrueAble();
     }
    
     public IDatatype error() {
         return null;
     }
     
    IDatatype errorOr(IDatatype dt){
         if (dt.booleanValue()){
             return TRUE;
         }
         return error();
     }
   
     IDatatype errorAnd(IDatatype dt){
         if (dt.booleanValue()){
             return error();
         }
         return FALSE;
     }
     
    public IDatatype and(IDatatype dt1, IDatatype dt2) {
        boolean e1 = error(dt1);
        boolean e2 = error(dt2);
        if (e1 && e2) {
            return error();
        } else if (e1) {
            return errorAnd(dt2);
        } else if (e2) {
            return errorAnd(dt1);
        } else {
            return getValue(dt1.booleanValue() && dt2.booleanValue());
        }
    }
    
    public IDatatype not(IDatatype dt){
        if (error(dt)){
            return error();
        }
        return getValue(! dt.booleanValue());
    }
    
    public IDatatype isWellFormed(IDatatype dt){
        if (dt.isLiteral() && dt.isUndefined()){
            if (dt.getDatatypeURI().startsWith(RDF.XSD)){ 
                return FALSE;
            }
            else if (dt.getDatatypeURI().startsWith(RDF.RDF) && ! dt.getDatatypeURI().equals(RDF.RDF_HTML)){
                return FALSE;
            }
        }
        return TRUE;
    }
              
    public IDatatype isList(IDatatype dt){
        return getValue(dt.isList());
    }
    
    public IDatatype first(IDatatype dt){
        return DatatypeMap.first(dt);
    }
    
    public IDatatype rest(IDatatype dt){
        return DatatypeMap.rest(dt);
    }
    
    public IDatatype reverse(IDatatype dt){
        return DatatypeMap.reverse(dt);
    }
    
    public IDatatype sort(IDatatype dt){
        return DatatypeMap.sort(dt);
    }
    
    public IDatatype member(IDatatype dt1, IDatatype dt2){
        return DatatypeMap.member(dt1, dt2);
    }
    
    public IDatatype cons(IDatatype dt1, IDatatype dt2){
        return DatatypeMap.cons(dt1, dt2);
    }
    
    public IDatatype add(IDatatype dt, IDatatype dtlist){
        return DatatypeMap.add(dt, dtlist);
    }
    
    public IDatatype add(IDatatype dt, IDatatype dtlist, IDatatype dtind){
        return DatatypeMap.add(dt, dtlist, dtind);
    }
    
    public IDatatype swap(IDatatype dt, IDatatype dtlist, IDatatype dtind){
        return DatatypeMap.swap(dt, dtlist, dtind);
    }
    
    public IDatatype set(IDatatype dt1, IDatatype dtind, IDatatype dtval){
        return DatatypeMap.set(dt1, dtind, dtval);
    }
    
    public IDatatype append(IDatatype dt1, IDatatype dt2){
        return DatatypeMap.append(dt1, dt2);
    }
    
    public IDatatype merge(IDatatype dt1, IDatatype dt2){
        return DatatypeMap.merge(dt1, dt2);
    }
    
    public IDatatype merge(IDatatype dt){
        return DatatypeMap.merge(dt);
    }
    
    public IDatatype iota(IDatatype... args){
        IDatatype dt = args[0];
        if (dt.isNumber()){
            return iotaNumber(args);
        }
        return iotaString(args);
    }
    
    IDatatype iotaNumber(IDatatype[] args){
        int start = 1;
        int end = 1;
        
        if (args.length > 1){
            start = args[0].intValue();
            end =   args[1].intValue();
        }
        else {
            end =    args[0].intValue();
        }
        if (end < start){
            return DatatypeMap.createList();
        }
        
        int step = 1;
        if (args.length == 3){
            step = args[2].intValue();
        }
        int length = (end - start + step) / step;
        ArrayList<IDatatype> ldt = new ArrayList<IDatatype>(length);
        
        for (int i=0; i<length; i++){
            ldt.add(DatatypeMap.newInstance(start));
            start += step;
        }
        IDatatype dt = DatatypeMap.createList(ldt);
        return dt;
    }
    
    IDatatype iotaString(IDatatype[] args){
        String fst =  args[0].stringValue();
        String snd = args[1].stringValue();
        int step = 1;
        if (args.length == 3){
            step =  args[2].intValue();
        }               
        String str = alpha;
        int start = str.indexOf(fst);
        int end   = str.indexOf(snd);
        if (start == -1){
            str = ALPHA;
            start = str.indexOf(fst);
            end   = str.indexOf(snd);
        }
        if (start == -1 || end == -1){
            return null;
        }
       
        
        int length = (end - start + step) / step;
        ArrayList<IDatatype> ldt = new ArrayList<IDatatype>(length);
        
        for (int i=0; i<length; i++){
            ldt.add(DatatypeMap.newInstance(str.substring(start, start+1)));
            start += step;
        }
        IDatatype dt = DatatypeMap.createList(ldt);
        return dt;
    }
    
    
      IDatatype iterate(IDatatype... args){
        int start = 0;
        int end = 1;
        
        if (args.length > 1){
            start = args[0].intValue();
            end =   args[1].intValue();
        }
        else {
            end =   args[0].intValue();
        }
        
        int step = 1;
        
        if (end < start){
            step = -1;
        }
        
        if (args.length == 3){
            step = args[2].intValue();
        }
                      
        IDatatype dt = DatatypeMap.newIterate(start, end, step);
        return dt;
    }

    @Override
    public Object getBufferedValue(StringBuilder sb, Environment env) {
        return plugin.getBufferedValue(sb, env);
    }
    
    public IDatatype get(IDatatype dt1, IDatatype dt2){
        return gget(dt1, null, dt2);
    }
    
    /*
     * Java compiler
     * dtmap = SPARQL Query solution Mappings
     * dtvar = variable name
     * return value of variable in first Mapping
     */
    public IDatatype gget(IDatatype dtmap, IDatatype dtvar){
        if (! dtmap.isPointer() || dtmap.pointerType() != PointerObject.MAPPINGS_POINTER){
            return null;
        }
        Mappings map = dtmap.getPointerObject().getMappings();
        return (IDatatype) map.getValue(dtvar.stringValue());
    }
    
    /**
     * Generic get with variable name and index
     * may be unbound, return specific UNDEF value because null would be considered an error  
     * embedding let will let the variable unbound, see getConstantValue()
     * it can be catched with bound(var) or coalesce(var)
     */
    public IDatatype gget(IDatatype dt, IDatatype var, IDatatype ind){
        if (dt.isList()) {
            return getResult(DatatypeMap.get(dt, ind));           
        }
        if (dt.isPointer()){
            Object res = dt.getPointerObject().getValue((var == null) ? null : var.getLabel(), ind.intValue());
            if (res == null) {                
                return UNDEF;
            }                 
            return  DatatypeMap.getValue(res);           
        } 
        return getResult(dt.get(ind.intValue()));
    }
    
    IDatatype getResult(IDatatype dt){
        if (dt == null){
            return UNDEF;
        }
        return dt;
    }
            
    IDatatype nodeValue(Node n){
        return (IDatatype) n.getValue();
    }
     
    
    IDatatype reject(Environment env, IDatatype dtm){
        if (dtm.pointerType() == Pointerable.MAPPING_POINTER){
            env.getMappings().reject(dtm.getPointerObject().getMapping()); 
        }
        return TRUE;
    }
    
    public IDatatype size(IDatatype dt){
        if (dt.isList()){
            return DatatypeMap.size(dt);
        }
        if (dt.isPointer()){
            return getValue(dt.getPointerObject().size());                    
        }
        return null;
    }
    
    IDatatype dataset(Expr exp, Environment env, Producer p){
        ASTQuery ast = (ASTQuery) env.getQuery().getAST();
        Dataset ds = ast.getDataset();
        
        switch (exp.oper()){
            case XT_FROM:
                return ds.getFromList();
            case XT_NAMED:
                return ds.getNamedList();
        }
        return null;
    }

    /**
     * @return the environment
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * @param environment the environment to set
     */
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

   
}
