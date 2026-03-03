package fr.inria.corese.core.transform;

import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.Context;
import fr.inria.corese.core.sparql.triple.parser.Dataset;
import fr.inria.corese.core.sparql.triple.parser.NSManager;

public class ContextManager {
    private Context context;
    private Binding binding;
    private Mappings mappings;
    private Dataset dataset;
    private final TransformerMapping transformerMapping;
    private final NSManager namespaceManager;


    public ContextManager(TransformerMapping mapping, NSManager nsm)  {
        this.transformerMapping = mapping;
        this.context = new Context();
        this.namespaceManager = nsm;
    }

    // Getters and setters
    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
        initContext();
    }

    /**
     * Define prefix from Context slot st:prefix = ((ns uri))
     */
    void initContext() {
        if (getContext() != null) {
            if (getContext().hasValue(TransformerUtils.STL_PREFIX)) {
                definePrefix();
            }
        }
    }

    void definePrefix() {
        for (IDatatype def : context.get(TransformerUtils.STL_PREFIX).getValueList()) {
            if (def.isList() && def.size() >= 2) {
                getNSM().definePrefix(def.get(0).getLabel(), def.get(1).getLabel());
            }
        }
    }

    public Binding getBinding() {
        return binding;
    }

    public void setBinding(Binding binding) {
        this.binding = binding;
    }

    public Mappings getMappings() {
        return mappings;
    }

    public void setMappings(Mappings mappings) {
        this.mappings = mappings;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public NSManager getNSM() {
        return namespaceManager;
    }

}
