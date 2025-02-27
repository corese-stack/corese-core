package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.impl.CoreseNodeAdapter;
import fr.inria.corese.core.sparql.api.IDatatype;

public interface CoreseDatatypeAdapter extends CoreseNodeAdapter {

    IDatatype getIDatatype();
}
