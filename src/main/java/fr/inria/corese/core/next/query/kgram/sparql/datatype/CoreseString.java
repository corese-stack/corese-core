package fr.inria.corese.core.next.query.kgram.sparql.datatype;

import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.query.kgram.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;

/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br>
 * An implementation of the xsd:string datatype used by Corese
 * <br>
 * @author Olivier Savoie
 * @deprecated @TODO replace by fr.inria.corese.core.next.data class
 */

public class CoreseString extends CoreseStringLiteral {
  static Datatype code= Datatype.STRING;
  static final CoreseURI datatype=new CoreseURI(XSD.xsdString.getIRI().stringValue());


  public CoreseString() {}

  public CoreseString(String value) {
      super(value);

  }
  
  public static CoreseString create(String str){
	  return new CoreseString(str);
  }

  @Override
  public IDatatype getDatatype(){
       return datatype;
     }

  @Override
  public Datatype getCode() {
    return code;
  }

 
  @Override
  public boolean equalsWE(IDatatype iod) throws CoreseDatatypeException {
	  switch (iod.getCode()){
	  case STRING:  return getLabel().equals(iod.getLabel());
	  case LITERAL: return iod.equalsWE(this);
              
          //case UNDEF: 
	  case URI:
	  case BLANK: case TRIPLE: return false;
	  }
	  throw new CoreseDatatypeException("Equality evaluation could not be done");
  }
  

}
