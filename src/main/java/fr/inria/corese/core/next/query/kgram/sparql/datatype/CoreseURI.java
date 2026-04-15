package fr.inria.corese.core.next.query.kgram.sparql.datatype;

import fr.inria.corese.core.next.query.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.kgram.sparql.api.IDatatype;
import fr.inria.corese.core.next.query.kgram.sparql.exceptions.CoreseDatatypeException;


/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br>
 * An implementation of the xsd:anyURI datatype used by Corese
 * <br>
 * @author Olivier Savoie
 * @deprecated @TODO replace by fr.inria.corese.core.next.data class
 */

public class CoreseURI extends CoreseResource {
    static Datatype  code= Datatype.URI;

  public CoreseURI(String value) {
      super(value);
  }

    @Override
  public boolean isURI() {
	return true;
  }
  
    @Override
  public boolean isSkolem() {
	return getLabel().startsWith(ExpType.SKOLEM);
  }

    @Override
  public Datatype getCode() {
    return code;
  }
  
  @Override
  public fr.inria.corese.core.sparql.api.IDatatype.NodeKind getNodeKind() {
      return NodeKind.URI;
  }
  
    @Override
  public int compare(IDatatype iod) throws CoreseDatatypeException {
	  switch (iod.getCode()){
	  case URI: return getLabel().compareTo(iod.getLabel());
	  }
        throw new CoreseDatatypeException("Comparison evaluation could not be done");
  }

    @Override
  public boolean less(IDatatype iod) throws CoreseDatatypeException {
	  switch (iod.getCode()){
	  case URI: return getLabel().compareTo(iod.getLabel()) < 0;
	  }
        throw new CoreseDatatypeException("Comparison evaluation could not be done");
    }

    @Override
  public boolean lessOrEqual(IDatatype iod) throws CoreseDatatypeException{
	  switch (iod.getCode()){
	  case URI: return getLabel().compareTo(iod.getLabel()) <= 0;
	  }
        throw new CoreseDatatypeException("Comparison evaluation could not be done");
  }

    @Override
  public boolean greater(IDatatype iod) throws CoreseDatatypeException {
	  switch (iod.getCode()){
	  case URI: return getLabel().compareTo(iod.getLabel()) > 0;
	  }
        throw new CoreseDatatypeException("Comparison evaluation could not be done");
  }

    @Override
  public boolean greaterOrEqual(IDatatype iod) throws CoreseDatatypeException {
	  switch (iod.getCode()){
	  case URI: return getLabel().compareTo(iod.getLabel()) >= 0;
	  }
        throw new CoreseDatatypeException("Comparison evaluation could not be done");
  }

    @Override
  public boolean equalsWE(IDatatype iod) throws CoreseDatatypeException{
	  switch (iod.getCode()){
	  case URI: return getLabel().equals(iod.getLabel());
	  }
	  return false;
  }

}
