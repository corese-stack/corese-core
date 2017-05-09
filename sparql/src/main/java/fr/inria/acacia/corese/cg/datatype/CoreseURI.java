package fr.inria.acacia.corese.cg.datatype;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.exceptions.CoreseDatatypeException;
import fr.inria.edelweiss.kgram.api.core.ExpType;


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
 */

public class CoreseURI extends CoreseResource {
    static int  code=URI;

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
  public int getCode() {
    return code;
  }
  
    @Override
  public int compare(IDatatype iod) throws CoreseDatatypeException {
	  switch (iod.getCode()){
	  case URI: return getLabel().compareTo(iod.getLabel());
	  }
	  throw failure();
  }

    @Override
  public boolean less(IDatatype iod) throws CoreseDatatypeException {
	  switch (iod.getCode()){
	  case URI: return getLabel().compareTo(iod.getLabel()) < 0;
	  }
	  throw failure();
    }

    @Override
  public boolean lessOrEqual(IDatatype iod) throws CoreseDatatypeException{
	  switch (iod.getCode()){
	  case URI: return getLabel().compareTo(iod.getLabel()) <= 0;
	  }
	  throw failure();
  }

    @Override
  public boolean greater(IDatatype iod) throws CoreseDatatypeException {
	  switch (iod.getCode()){
	  case URI: return getLabel().compareTo(iod.getLabel()) > 0;
	  }
	  throw failure();
  }

    @Override
  public boolean greaterOrEqual(IDatatype iod) throws CoreseDatatypeException {
	  switch (iod.getCode()){
	  case URI: return getLabel().compareTo(iod.getLabel()) >= 0;
	  }
	  throw failure();
  }

    @Override
  public boolean equalsWE(IDatatype iod) throws CoreseDatatypeException{
	  switch (iod.getCode()){
	  case URI: return getLabel().equals(iod.getLabel());
	  }
	  return false;
  }



}
