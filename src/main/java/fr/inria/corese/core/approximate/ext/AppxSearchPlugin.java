package fr.inria.corese.core.approximate.ext;

import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseStringLiteral;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.kgram.api.core.Expr;
import fr.inria.corese.core.kgram.api.core.ExprType;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.kgram.tool.ApproximateSearchEnv;
import fr.inria.corese.core.approximate.algorithm.ISimAlgorithm;
import fr.inria.corese.core.approximate.algorithm.SimAlgorithmFactory;
import fr.inria.corese.core.approximate.algorithm.impl.BaseAlgorithm;
import fr.inria.corese.core.query.PluginImpl;

/**
 * Plugin implementation for approximate search
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @date 1 oct. 2015
 */
public class AppxSearchPlugin implements ExprType {

    static final IDatatype TRUE = DatatypeMap.TRUE;
    static final IDatatype FALSE = DatatypeMap.FALSE;
    private final PluginImpl plugin;

    public AppxSearchPlugin(PluginImpl p) {
        this.plugin = p;
    }

    public IDatatype evaluate(Expr exp, Environment env, Producer p) {
        if (exp.oper() == APP_SIM) {
            ApproximateSearchEnv appxEnv = env.getAppxSearchEnv();
            double d = appxEnv.aggregate(env);
            return plugin.getValue(d);
        }
        return null;
    }

    public IDatatype evaluate(Expr exp, Environment env, Producer p, Object[] args) {
        IDatatype[] param = (IDatatype[]) args;
        if (exp.oper() == APPROXIMATE) {//0. check parameters
            return evaluate(exp, env, param);
        }
        return null;
    }

    // Use approximate as a filter function
    private IDatatype match(IDatatype dt1, IDatatype dt2, String parameter, String algs, double threshold) {
        String s1 = stringValue(dt1);
        String s2 = stringValue(dt2);    
        if (s1.equalsIgnoreCase(s2)) {
            return TRUE;
        }
        ISimAlgorithm alg = SimAlgorithmFactory.createCombined(algs, true);
        double sim = alg.calculate(s1, s2, parameter);

        return (sim > threshold) ? TRUE : FALSE;
    }
    
    String stringValue(IDatatype dt){
        if (dt.hasLang()){
            return dt.stringValue().concat("@").concat(dt.getLang());
        }
        return dt.stringValue();
    }

    // Use 'approximate' as appx search and calculate similarity
    private IDatatype approximate(IDatatype dt1, IDatatype dt2, String parameter, String algs, double threshold, Environment env, Expr exp) {
        //0. initialize
        String s1 = stringValue(dt1);
        String s2 = stringValue(dt2);   
        Expr variableExpr = exp.getExp(0);
        ApproximateSearchEnv appxEnv = env.getAppxSearchEnv();

        Double combinedSim;
        Double singleSim = appxEnv.getSimilarity(variableExpr, dt1, algs);//check appx env to see if already computed

        boolean notExisted = (singleSim == null);

        //1 calculation to get current similarity and overall similarity
        if (s1.equalsIgnoreCase(s2)) {
            singleSim   = ISimAlgorithm.MAX;
            combinedSim = ISimAlgorithm.MAX;
        } else {
            if (notExisted) { //2.1.2 otherwise, re-calculate
                ISimAlgorithm alg = SimAlgorithmFactory.createCombined(algs, false);
                singleSim = alg.calculate(s1, s2, parameter);
            }
            combinedSim = appxEnv.aggregate(env, variableExpr, singleSim);
        }

        //3 finalize
        boolean filtered = combinedSim > threshold;

        if (notExisted) {
            appxEnv.add(variableExpr, dt2, dt1, algs, singleSim);
        }
        return filtered ? TRUE : FALSE;
    }

    
    /** 
     * filter approximate(var1, var2, 'ng-jw-wn-eq-mult', 0.2, true)
        args[0] = var 1
        args[1] = uri
        args[2] = alg list
        args[3] = threshold
        args[4] = false|true.
    */
    private IDatatype evaluate(Expr exp, Environment env, IDatatype[] args) {
        if (args.length != 4) {
            return FALSE;
        }

        IDatatype dt1 = args[0];
        IDatatype dt2 = args[1];
        if (dt1.stringValue() == null || dt2.stringValue() == null) {
            return null;
        }
        //IF the types are different, then return FALSE directly
        boolean match = match(dt1.getCode(), dt2.getCode());
        if (! match){
            return FALSE;
        }

        if (!(args[2] instanceof CoreseStringLiteral)
                || !args[3].isNumber()) {
            return FALSE;
        }

        String algs = args[2].stringValue();
        double threshold = args[3].doubleValue();

        String parameter = null;
        if (dt1.isURI()) {
            parameter = BaseAlgorithm.OPTION_URI;
        }

        ASTQuery ast =  env.getQuery().getAST();
        //is approximate search
        //TRUE: only calculate once, does not compute the value of similarity
        //FALSE: need to compute the value of similarity
        if (ast.isRelax()) {
            return this.approximate(dt1, dt2, parameter, algs, threshold, env, exp);
        } else {
            return this.match(dt1, dt2, parameter, algs, threshold);
        }

    }
    
    boolean match(IDatatype.Datatype c1, IDatatype.Datatype c2){
        if (c1 == c2){
            return true;
        }
        return (c1 == IDatatype.Datatype.STRING && c2 == IDatatype.Datatype.LITERAL) ||
                (c2 == IDatatype.Datatype.STRING && c1 == IDatatype.Datatype.LITERAL);
    }
  
}
