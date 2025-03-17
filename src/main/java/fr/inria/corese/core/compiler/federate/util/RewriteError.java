package fr.inria.corese.core.compiler.federate.util;

import fr.inria.corese.core.sparql.triple.parser.Exp;

/**
 *
 */
public class RewriteError {

    private Exp exp;
    private String message;

    RewriteError(Exp e, String mes) {
        exp = e;
        message = mes;
    }

    @Override
    public String toString() {
        return String.format("%s:\n%s", getMessage(), getExp());
    }

    public Exp getExp() {
        return exp;
    }

    public void setExp(Exp exp) {
        this.exp = exp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
