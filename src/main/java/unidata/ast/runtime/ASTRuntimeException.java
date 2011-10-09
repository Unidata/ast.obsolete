/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.ast.runtime;

public class ASTRuntimeException extends java.lang.RuntimeException {

    public ASTRuntimeException() {
        super();
    }

    public ASTRuntimeException(java.lang.String message) {
        super(message);
    }

    public ASTRuntimeException(java.lang.String message, java.lang.Throwable cause) {
        super(message, cause);
    }

    public ASTRuntimeException(java.lang.Throwable cause) {
        super(cause);
    }
}
