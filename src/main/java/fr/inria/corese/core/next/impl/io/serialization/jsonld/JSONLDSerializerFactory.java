package fr.inria.corese.core.next.impl.io.serialization.jsonld;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.base.io.RdfFormat;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.impl.io.TitaniumJSONLDProcessorOptions;

/**
 * Placeholder class while waiting for the general serializer factory to be implemented
 */
public class JSONLDSerializerFactory {

    public JSONLDSerializerFactory() {
    }

    public JSONLDSerializer createSerializer(Model model) {
        return createSerializer(RdfFormat.JSONLD, model, null);
    }

    public JSONLDSerializer createSerializer(RdfFormat format, Model model, IOOptions config) {
        if(format == RdfFormat.JSONLD) {
            if(config == null) {
                return new JSONLDSerializer(model);
            } else if(config instanceof TitaniumJSONLDProcessorOptions) {
                return new JSONLDSerializer(model, (TitaniumJSONLDProcessorOptions) config);
            }  else {
                throw new IllegalArgumentException("Unsupported config for JSONLD serialization: " + config);
            }
        } else {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }

    }
}
