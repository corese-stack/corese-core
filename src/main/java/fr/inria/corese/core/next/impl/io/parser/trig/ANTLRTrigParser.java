package fr.inria.corese.core.next.impl.io.parser.trig;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.base.io.parser.AbstractRDFParser;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.exception.ParsingErrorException;
import fr.inria.corese.core.next.impl.parser.antlr.TriGLexer;
import fr.inria.corese.core.next.impl.parser.antlr.TriGParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Un parseur pour le format Trig basé sur ANTLR4.
 * Il utilise une grammaire ANTLR pour tokeniser et parser les documents Trig,
 * puis un listener pour construire le modèle RDF.
 */
public class ANTLRTrigParser extends AbstractRDFParser {

    /**
     * Constructeur pour le parseur ANTLRTrigParser.
     *
     * @param model   Le modèle RDF à peupler.
     * @param factory La fabrique de valeurs pour créer des ressources RDF.
     */
    public ANTLRTrigParser(Model model, ValueFactory factory) {
        super(model, factory);
    }

    /**
     * Constructeur pour le parseur ANTLRTrigParser avec des options de configuration.
     *
     * @param model   Le modèle RDF à peupler.
     * @param factory La fabrique de valeurs pour créer des ressources RDF.
     * @param config  Les options de configuration pour le parsing.
     */
    public ANTLRTrigParser(Model model, ValueFactory factory, IOOptions config) {
        super(model, factory, config);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.TRIG;
    }

    @Override
    public void setConfig(IOOptions config) {
        // Cette méthode est requise par l'interface mais n'est pas utilisée dans cette implémentation.
    }

    @Override
    public void parse(InputStream in) throws ParsingErrorException {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), null);
    }

    @Override
    public void parse(InputStream in, String baseURI) throws ParsingErrorException {
        parse(new InputStreamReader(in, StandardCharsets.UTF_8), baseURI);
    }

    @Override
    public void parse(Reader reader) throws ParsingErrorException {
        parse(reader, null);
    }

    /**
     * Parse les données Trig à partir d'un {@link Reader} en utilisant ANTLR4.
     *
     * @param reader  Le {@link Reader} pour lire les données RDF.
     * @param baseURI L'URI de base.
     * @throws ParsingErrorException si une erreur de parsing ou d'E/S se produit.
     */
    @Override
    public void parse(Reader reader, String baseURI) throws ParsingErrorException {
        try {
            CharStream charStream = CharStreams.fromReader(reader);
            TriGLexer triGLexer = new TriGLexer(charStream);

            TrigErrorListener trigErrorListener = new TrigErrorListener();
            triGLexer.removeErrorListeners();
            triGLexer.addErrorListener(trigErrorListener);

            CommonTokenStream tokens = new CommonTokenStream(triGLexer);
            TriGParser triGParser = new TriGParser(tokens);


            triGParser.removeErrorListeners();
            triGParser.addErrorListener(trigErrorListener);

            ParseTreeWalker walker = new ParseTreeWalker();
            ParseTree tree = triGParser.trigDoc();

            if (trigErrorListener.hasErrors()) {
                throw new ParsingErrorException("Syntax error in TriG document: " + trigErrorListener.getErrorMessage());
            }

            TriGListerner listerner = new TriGListerner(getModel(), getValueFactory(), this.getConfig(), baseURI);
            walker.walk((ParseTreeListener) listerner, tree);

        } catch (ParsingErrorException e) {
            throw e;
        } catch (IOException e) {
            throw new ParsingErrorException("Failed to parse TriG RDF: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParsingErrorException("Unexpected error during TriG parsing: " + e.getMessage(), e);
        }
    }


    /**
     * Un écouteur d'erreurs personnalisé pour collecter les erreurs du lexer et du parseur.
     */
    private static class TrigErrorListener extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();

        /**
         * Enregistre les erreurs de syntaxe générées par ANTLR.
         *
         * @param recognizer       Le recognizer qui a détecté l'erreur.
         * @param offendingSymbol  Le symbole qui a causé l'erreur.
         * @param line             Le numéro de ligne où l'erreur s'est produite.
         * @param charPositionInLine La position du caractère sur la ligne.
         * @param msg              Le message d'erreur.
         * @param e                L'exception de reconnaissance.
         */
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            if (msg != null && (msg.contains("token recognition error") || msg.contains("mismatched input"))) {
                if (offendingSymbol instanceof Token) {
                    Token token = (Token) offendingSymbol;
                    String tokenText = token.getText();
                    if (msg.contains("token recognition error") && tokenText != null && tokenText.contains("\"")) {
                        msg = "Invalid string literal - possibly unterminated or contains invalid escape sequence: " + msg;
                    }
                }
            }

            String error = "line " + line + ":" + charPositionInLine + " " + msg;
            errors.add(error);
        }

        /**
         * Vérifie si des erreurs de parsing ont été trouvées.
         * @return `true` si la liste d'erreurs n'est pas vide, sinon `false`.
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        /**
         * Retourne un message d'erreur formaté contenant toutes les erreurs trouvées.
         * @return Un {@link String} contenant les messages d'erreur.
         */
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
