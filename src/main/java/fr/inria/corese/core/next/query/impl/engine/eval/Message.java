package fr.inria.corese.core.next.query.impl.engine.eval;


public class Message {

    private Message() {
        // Utility class
    }

    public enum Prefix {
        UNDEF_VAR("Undefined variable: ");

        private final String text;
        Prefix(String text) {
            this.text = text;
        }
        public String getString() {
            return text;
        }
    }

}
