package fr.inria.corese.core.next.impl.temp.literal;

import fr.inria.corese.core.next.impl.temp.CoreseNodeAdapter;
import fr.inria.corese.core.sparql.api.IDatatype;

public interface CoreseDatatypeAdapter extends CoreseNodeAdapter {

    IDatatype getIDatatype();
}
