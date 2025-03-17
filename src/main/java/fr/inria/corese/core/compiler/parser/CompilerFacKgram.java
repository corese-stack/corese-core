package fr.inria.corese.core.compiler.parser;


/**
 * Compiler Factory for Transformer.
 * Generate target Edge/Node/Filter
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public class CompilerFacKgram implements CompilerFactory {


    public CompilerFacKgram() {
    }

    @Override
    public Compiler newInstance() {
        return new CompilerKgram();
    }


}
