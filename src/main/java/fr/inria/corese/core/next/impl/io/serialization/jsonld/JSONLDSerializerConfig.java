package fr.inria.corese.core.next.impl.io.serialization.jsonld;

import com.apicatalog.jsonld.JsonLdVersion;
import fr.inria.corese.core.next.api.io.IOConfig;

public class JSONLDSerializerConfig implements IOConfig {

    private boolean ordered = false;
    private JsonLdVersion version = JsonLdVersion.V1_1;
    private boolean useNativeTypes = false;
    private boolean useRdfType = false;
    public JSONLDSerializerConfig() {
    }

    public boolean isOrdered() {
        return ordered;
    }

    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }

    public void setVersionTo10() {
        version = JsonLdVersion.V1_0;
    }

    public void setVersionTo11() {
        version = JsonLdVersion.V1_1;
    }

    public JsonLdVersion getVersion() {
        return version;
    }

    public boolean usesNativeTypes() {
        return useNativeTypes;
    }

    public void setUseNativeTypes(boolean useNativeTypes) {
        this.useNativeTypes = useNativeTypes;
    }

    public boolean usesRdfType() {
        return useRdfType;
    }

    public void setUseRdfType(boolean useRdfType) {
        this.useRdfType = useRdfType;
    }
}
