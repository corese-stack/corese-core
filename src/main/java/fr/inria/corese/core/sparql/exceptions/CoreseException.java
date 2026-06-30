package fr.inria.corese.core.sparql.exceptions;

/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br>
 * This class subsumes CoreseDatatypeException, CoreseNullMarkerException, ExistException, QueryException, 
 * SizeException, TypeException and Unbound.<br>
 * It deals with all the exceptions that can be thrown by Corese.<br>
 * <br>
 * @author Olivier Corby
 */

public class CoreseException extends Exception {

	/** Use to keep the class version, to be consistent with the interface Serializable.java */
	private static final long serialVersionUID = 1L;

	public Object object = null;//modif Olivier Savoie pour le repackaging

	public CoreseException() {
	}


	public CoreseException(String msg) {
		super(msg);
	}


}
