package fr.inria.corese.core.next.storage;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.impl.model.StorageModel;

import java.util.Objects;

/** Public entry point for exposing a storage backend as a mutable RDF model. */
public final class StorageModels {

    private StorageModels() {
    }

    /**
     * Creates a mutable live model backed by an open storage manager.
     *
     * <p>The returned model does not own {@code storage}; the caller remains
     * responsible for its lifecycle. This makes it possible to share one backend
     * between RDF I/O and a repository.</p>
     *
     * @param storage storage backend to expose as a model
     * @return a model backed by {@code storage}
     * @throws NullPointerException if {@code storage} is {@code null}
     */
    public static Model create(StorageManager storage) {
        return StorageModel.builder()
                .storage(Objects.requireNonNull(storage, "storage"))
                .valueFactory(Values.factory())
                .build();
    }
}
