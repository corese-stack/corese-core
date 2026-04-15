package fr.inria.corese.core.next.query.kgram.sparql.datatype;

import fr.inria.corese.core.next.query.kgram.api.core.DatatypeValue;
import fr.inria.corese.core.next.query.kgram.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;

/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br>
 * Root of URI and BlankNode
 * <br>
 */

public class CoreseResource extends CoreseStringableImpl {
    static Datatype  code= Datatype.URI;

    public CoreseResource(String value) {
      super(value);

  }

// URI and Blank have no lang, hence return null
    @Override
  public IDatatype getDataLang() {
       return null;
     }

    @Override
     public boolean isTrue() throws CoreseDatatypeException {
         throw new CoreseDatatypeException("isTrue not implemented");
       }

    @Override
    public boolean equalsWE(DatatypeValue other) throws CoreseDatatypeException {
        return false;
    }

    @Override
    public int compare(DatatypeValue other) throws CoreseDatatypeException {
        return 0;
    }

    @Override
       public boolean isTrueAble() {
         return false;
       }
       
    @Override
    public boolean booleanValue() {
        return false;
    }

    @Override
    public IDatatype toList() {
        return null;
    }

    @Override
    public Iterable getLoop() {
        return null;
    }

    @Override
       public boolean isLiteral() {
    	   return false;
       }
       
       /**
        * SPARQL fails because URI have no datatype
        */
    @Override
       public IDatatype getDatatype(){
    	   return null;
       }
       
    @Override
       public  Datatype getCode(){
    	   return code;
       }
       



}
