package fr.inria.corese.core.sparql.triple.function.script;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 *
 */
public class JavaFunction extends LDScript {
    static final String GLOBAL_VALUE = "ds:getPublicDatatypeValue";
    private boolean reject = false;

    JavaFunction() {}

    JavaFunction(String name) {
        super(name);
//        switch (name) {
//            case GLOBAL_VALUE: break;
//            default: setReject(Access.reject(Feature.JAVA_FUNCTION));
//        }
    }
    
    /**
     * @param reject the reject to set
     */
    public void setReject(boolean reject) {
        this.reject = reject;
    }
    
}
