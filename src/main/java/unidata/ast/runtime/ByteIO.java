/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.ast.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ByteIO extends AbstractIO
{

/* Should be exactly eight characters long */
static final char BYTEIO_UIDSTRING[] = new char[]{'b','y','t','e','i','o',' ',' '};

// Read constructor(s)

public
ByteIO() // only if writing
    throws IOException
{
    this(IOmode.Ast_write,new ByteArrayOutputStream());
}

public
ByteIO(IOmode mode, Object stream)
    throws IOException
{
    super(mode);    
    switch (mode) {
    case Ast_read:
        if(stream == null || !(stream instanceof InputStream))
	    throw new IOException("Byteio: no InputStream specified");
	setStream((InputStream)stream);
	break;
    case Ast_write:
        if(stream == null || !(stream instanceof OutputStream))
	    throw new IOException("Byteio: no OutputStream specified");
	setStream((OutputStream)stream);
	break;
    }
}

}

