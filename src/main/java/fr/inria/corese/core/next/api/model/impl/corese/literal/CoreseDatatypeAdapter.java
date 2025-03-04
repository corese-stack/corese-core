package fr.inria.corese.core.next.api.model.impl.corese.literal;

import fr.inria.corese.core.next.api.model.impl.corese.CoreseNodeAdapter;
import fr.inria.corese.core.sparql.api.IDatatype;

public interface CoreseDatatypeAdapter extends CoreseNodeAdapter {

    IDatatype getIDatatype();
}
