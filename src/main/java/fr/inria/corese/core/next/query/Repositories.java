package fr.inria.corese.core.next.query;

import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.impl.repository.CoreseRepository;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.plugin.PluginException;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Public entry point for creating ready-to-use Corese repositories.
 *
 * <p>The common path requires no implementation imports or explicit
 * initialization:</p>
 *
 * <pre>{@code
 * try (Repository repository = Repositories.create()) {
 *     repository.update("INSERT DATA { <urn:s> <urn:p> <urn:o> }");
 *     boolean present = repository.ask("ASK { <urn:s> <urn:p> <urn:o> }");
 * }
 * }</pre>
 *
 * <p>A repository owns the storage manager passed to it. Closing the repository
 * closes that storage manager. Instances returned by this class are open and
 * immediately usable.</p>
 */
public final class Repositories {

    private Repositories() {
    }

    /**
     * Creates an open repository backed by the default in-memory storage.
     *
     * @return a ready-to-use repository
     */
    public static Repository create() {
        return create(StorageConfig.memory());
    }

    /**
     * Creates an open repository using the storage plugin selected by the
     * supplied configuration.
     *
     * @param config storage plugin configuration
     * @return a ready-to-use repository owning the selected storage
     */
    public static Repository create(StorageConfig config) {
        StorageConfig checkedConfig = Objects.requireNonNull(config, "config");
        try {
            return new CoreseRepository(Storages.create(checkedConfig), checkedConfig);
        } catch (PluginException e) {
            throw new RepositoryException("Could not create repository storage", e);
        }
    }

    /**
     * Creates an open repository that takes ownership of a storage manager.
     * An uninitialized manager is initialized with an empty configuration; an
     * already-running manager is adopted as-is.
     *
     * @param storage storage manager whose ownership is transferred
     * @return a ready-to-use repository
     */
    public static Repository create(StorageManager storage) {
        return create(storage, StorageConfig.builder().build());
    }

    /**
     * Creates an open repository that takes ownership of a storage manager.
     * The configuration is used only when the manager is not initialized yet.
     *
     * @param storage storage manager whose ownership is transferred
     * @param config initialization configuration
     * @return a ready-to-use repository
     */
    public static Repository create(StorageManager storage, StorageConfig config) {
        return new CoreseRepository(
                Objects.requireNonNull(storage, "storage"),
                Objects.requireNonNull(config, "config"));
    }

    /**
     * Creates an open in-memory repository pre-loaded with RDF statements from the specified file.
     * The file format is inferred from the file extension.
     *
     * @param source RDF source file
     * @return a ready-to-use repository containing the loaded data
     * @throws RepositoryException if repository creation or RDF loading fails
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public static Repository createFrom(Path source) {
        Objects.requireNonNull(source, "source");
        Repository repository = create();
        try {
            repository.load(source);
            return repository;
        } catch (RuntimeException | Error e) {
            try {
                repository.close();
            } catch (Exception closeEx) {
                e.addSuppressed(closeEx);
            }
            throw e;
        }
    }

    /**
     * Creates an open in-memory repository pre-loaded with RDF statements from the specified input stream.
     *
     * @param source RDF input stream
     * @param format explicit RDF serialization format
     * @return a ready-to-use repository containing the loaded data
     * @throws RepositoryException if repository creation or RDF loading fails
     * @throws NullPointerException if {@code source} or {@code format} is {@code null}
     */
    public static Repository createFrom(InputStream source, RDFFormat format) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(format, "format");
        Repository repository = create();
        try {
            repository.load(source, format);
            return repository;
        } catch (RuntimeException | Error e) {
            try {
                repository.close();
            } catch (Exception closeEx) {
                e.addSuppressed(closeEx);
            }
            throw e;
        }
    }

    /**
     * Executes work against a temporary in-memory repository, ensuring deterministic closure.
     *
     * @param consumer repository callback
     * @throws RepositoryException if repository creation, execution, or closure fails
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    public static void withMemory(Consumer<? super Repository> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        try (Repository repository = create()) {
            consumer.accept(repository);
        }
    }

    /**
     * Computes a result against a temporary in-memory repository, ensuring deterministic closure.
     *
     * @param function computation callback
     * @param <T> result type
     * @return the computed value
     * @throws RepositoryException if repository creation, execution, or closure fails
     * @throws NullPointerException if {@code function} is {@code null}
     */
    public static <T> T computeWithMemory(Function<? super Repository, ? extends T> function) {
        Objects.requireNonNull(function, "function");
        try (Repository repository = create()) {
            return function.apply(repository);
        }
    }
}
