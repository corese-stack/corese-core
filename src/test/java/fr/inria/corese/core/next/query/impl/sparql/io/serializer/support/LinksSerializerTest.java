package fr.inria.corese.core.next.query.impl.sparql.io.serializer.support;

import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.query.api.io.serializer.LinksOptions;

import java.util.Collection;
import java.util.List;

public interface LinksSerializerTest {

    default IOOptions getOptionsWithLinks() {
        return new LinksOptions() {
            @Override
            public Collection<String> links() {
                return List.of("http://google.com", "mailto:bob@corese-test.com");
            }
        };
    }
}
