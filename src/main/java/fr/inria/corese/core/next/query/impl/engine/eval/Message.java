package fr.inria.corese.core.next.query.impl.engine.eval;


public class Message {

    private Message() {
        // Utility class
    }

    public enum Prefix {
        UNDEF_VAR("Undefined variable: "),
        UNDEF_FUN("Undefined function: "),
        FAIL("Corese fail at compile time"),
        FAIL_AT("Corese fail at: "),
        EVAL("Eval: "),
        FREE("Pattern is Free: "),
        CHECK("Check: "),
        REWRITE("Compiler rewrite error: "),
        PRAGMA("Pragma: "),
        LOOP("Loop: "),
        AGG("Aggregate limited to (defined) variable: ");

        private final String text;
        Prefix(String text) {
            this.text = text;
        }
        public String getString() {
            return text;
        }
    }

}
