package fr.inria.corese.core.next.data;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.impl.model.LinkedHashModel;

import java.util.Objects;

/** Public entry point for creating standalone RDF models. */
public final class Models {

    private Models() {
    }

    /**
     * Creates an empty, mutable, insertion-ordered in-memory model.
     *
     * @return a new model
     */
    public static Model create() {
        return new LinkedHashModel(Values.factory());
    }

    /**
     * Creates a mutable, insertion-ordered in-memory model initialized from a
     * statement source. The source is consumed once and is not retained.
     *
     * @param statements initial statements
     * @return a new model containing the supplied statements
     * @throws NullPointerException if the source or one of its statements is {@code null}
     */
    public static Model create(Iterable<? extends Statement> statements) {
        return createFromStatements(statements);
    }

    private static Model createFromStatements(Iterable<? extends Statement> statements) {
        Model model = create();
        for (Statement statement : Objects.requireNonNull(statements, "statements")) {
            model.add(Objects.requireNonNull(statement, "statement"));
        }
        return model;
    }

    /**
     * Creates an independent mutable copy of a model, including its prefix
     * metadata.
     *
     * @param source source model
     * @return an independent copy
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public static Model create(Model source) {
        Model checkedSource = Objects.requireNonNull(source, "source");
        Model copy = createFromStatements(checkedSource);
        checkedSource.getNamespaces().forEach(copy::setNamespace);
        return copy;
    }
}
