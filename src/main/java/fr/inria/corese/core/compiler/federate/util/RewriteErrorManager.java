package fr.inria.corese.core.compiler.federate.util;

import fr.inria.corese.core.sparql.triple.parser.Exp;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class RewriteErrorManager {
    final static String NL = "\n";
    private List<RewriteError> errorList;


    public RewriteErrorManager() {
        errorList = new ArrayList<>();
    }
    
    public void add(String mes, Exp exp) {
        getErrorList().add(new RewriteError(exp, mes));
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (RewriteError err : getErrorList()) {
            sb.append(err).append(NL);
        }
        return sb.toString();
    }

    public List<RewriteError> getErrorList() {
        return errorList;
    }

    public void setErrorList(List<RewriteError> errorList) {
        this.errorList = errorList;
    }
    
    
}
