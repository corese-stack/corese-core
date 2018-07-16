package fr.inria.corese.compiler.eval;

import fr.inria.corese.kgram.api.core.DatatypeValue;
import fr.inria.corese.kgram.api.core.Expr;
import fr.inria.corese.kgram.api.core.ExprType;
import fr.inria.corese.kgram.core.Eval;
import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.datatype.DatatypeMap;
import fr.inria.corese.sparql.triple.parser.NSManager;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 *
 */
public class QuerySolverOverload {

    public static final String ERROR    = "@error"; 
    public static final String EQ = "@eq";
    public static final String NE = "@ne";
    public static final String LE = "@le";
    public static final String LT = "@lt";
    public static final String GE = "@ge";
    public static final String GT = "@gt";

    public static final String US = NSManager.USER;
    public static final String MEQ = US + "eq";
    public static final String MNE = US + "ne";
    public static final String MLE = US + "le";
    public static final String MLT = US + "lt";
    public static final String MGE = US + "ge";
    public static final String MGT = US + "gt";

    public static final String MPLUS = US + "plus";
    public static final String MMINUS = US + "minus";
    public static final String MMULT = US + "mult";
    public static final String MDIVIS = US + "divis";

    private boolean overload = true;
    QuerySolverVisitor visitor;
    
    
    QuerySolverOverload(QuerySolverVisitor vis) {
        visitor = vis;
    }
    
    public void setOverload(boolean b) {
        overload = b;
    }
    
    public boolean isOverload() {
        return overload;
    }
    

    public boolean overload(Expr exp, DatatypeValue res, DatatypeValue dt1, DatatypeValue dt2) {
        return overload && isOverload(dt1, dt2);
    }

    boolean isOverload(DatatypeValue dt1, DatatypeValue dt2) {
        return datatypeOverload(dt1, dt2) || bnodeOverload(dt1, dt2);
    }
    
    boolean bnodeOverload(DatatypeValue dt1, DatatypeValue dt2) {
        return dt1.isBlank() && dt2.isBlank();
    }
    
    boolean datatypeOverload(DatatypeValue dt1, DatatypeValue dt2) {
        return dt1.isUndefined() && dt2.isUndefined()
                && dt1.getDatatypeURI().equals(dt2.getDatatypeURI());
    }


    public IDatatype error(Eval eval, Expr exp, DatatypeValue[] args) {
        return overloadError(eval, exp, (IDatatype[]) args);
    }

    /**
     * Return error() if res == null && overload == null
     */
    public IDatatype overload(Eval eval, Expr exp, IDatatype res, IDatatype[] param) {
        // 1) @type us:length function us:eq(?e, ?a, ?b) where datatype(?a) == us:lengtj
        // 2) function us:eq(?e, ?a, ?b)
        IDatatype dt = overloadMethod(eval, exp, param);
        if (dt == null) {
            if (res == null) {
                return overloadError(eval, exp, param);
            }
            // std result
            return  res;
        }
        return dt;
    }

    /**
     * a = b return null 1) try @eq function us:feq(?e, ?a, ?b) 2) otherwise try
     * @error function us:error(?e, a, ?b)
     */
    IDatatype overloadError(Eval eval, Expr exp, IDatatype[] param) {
        String name = getName(exp, param);
        IDatatype[] values = toArray(param, exp);
        if (name == null) {
            return visitor.callback(eval, ERROR, values);
        }
        IDatatype val = visitor.callback(eval, name, values);
        if (val == null) {
            val = visitor.callback(eval, ERROR, values);
        }
        return val;
    }

    IDatatype[] toArray(IDatatype[] args, Object... lobj) {
        IDatatype[] param = new IDatatype[args.length + lobj.length];
        int i = 0;
        for (Object obj : lobj) {
            param[i++] = DatatypeMap.getValue(obj);
        }
        for (IDatatype val : args) {
            param[i++] = val;
        }
        return param;
    }

    IDatatype overloadFunction(Eval eval, Expr exp, IDatatype[] param) {
        String name = getName(exp, param);
        if (name == null) {
            return null;
        }
        return visitor.callback(eval, name, param);
    }

    IDatatype overloadMethod(Eval eval, Expr exp, IDatatype[] param) {
        String name = getMethodName(exp);
        if (name == null) {
            return null;
        }
        return visitor.method(eval, name, kind(param[0]), param);
    }
    
    IDatatype kind(IDatatype dt) {
       return DatatypeMap.kind(dt);
    }

    String getMethodName(Expr exp) {
        switch (exp.oper()) {
            case ExprType.EQ:
                return MEQ;
            case ExprType.NEQ:
                return MNE;

            case ExprType.LE:
                return MLE;
            case ExprType.LT:
                return MLT;
            case ExprType.GE:
                return MGE;
            case ExprType.GT:
                return MGT;

            case ExprType.PLUS:
                return MPLUS;
            case ExprType.MINUS:
                return MMINUS;
            case ExprType.MULT:
                return MMULT;
            case ExprType.DIV:
                return MDIVIS;
        }
        return null;
    }
   
    String getName(Expr exp, IDatatype[] param) {
        switch (exp.oper()) {
            case ExprType.EQ:
                return EQ;
            case ExprType.NEQ:
                return NE;
            case ExprType.LE:
                return LE;
            case ExprType.LT:
                return LT;
            case ExprType.GE:
                return GE;
            case ExprType.GT:
                return GT;
        }
        return null;
    }

}
