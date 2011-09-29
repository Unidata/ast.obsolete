/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.ast.runtime;

import java.io.IOException;

public class ASTException extends IOException {

    public ASTException() {
        super();
    }

    public ASTException(java.lang.String message) {
        super(message);
    }

    public ASTException(java.lang.String message, java.lang.Throwable cause) {
        super(message, cause);
    }

    public ASTException(java.lang.Throwable cause) {
        super(cause);
    }
}
