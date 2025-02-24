package fr.inria.corese.core.kgram.tool;

import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Message {

    static Logger logger = LoggerFactory.getLogger(Message.class);

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

        private final String prefix;
        Prefix(String p) {
            prefix = p;
        }
        public String getString() {
            return prefix;
        }
    }

}
