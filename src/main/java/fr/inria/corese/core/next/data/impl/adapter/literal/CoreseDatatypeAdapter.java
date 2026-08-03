package fr.inria.corese.core.next.data.impl.adapter.literal;

import fr.inria.corese.core.next.data.impl.adapter.CoreseNodeAdapter;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * Interface for Corese datatype adapter.
 * This interface is to be used to apply the adapter design pattern around classes representing literals.
 *
 */
public interface CoreseDatatypeAdapter extends CoreseNodeAdapter {

    /**
     * Returns the adapted object.
     *
     * @return the adapted object
     */
    IDatatype getIDatatype();
}
