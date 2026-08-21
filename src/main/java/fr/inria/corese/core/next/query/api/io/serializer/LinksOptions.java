package fr.inria.corese.core.next.query.api.io.serializer;

import fr.inria.corese.core.next.data.api.io.option.IOOptions;

import java.util.Collection;

public interface LinksOptions extends IOOptions {

    Collection<String> links();
}
