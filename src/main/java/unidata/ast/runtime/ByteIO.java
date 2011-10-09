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
    this(new ByteArrayOutputStream());
}

public
ByteIO(Object stream)
    throws IOException
{
    super();
    if(stream == null)
        throw new IOException("ByteIO: no stream specified");
    if(stream instanceof InputStream)
	setStream((InputStream)stream);
    else if(stream instanceof OutputStream)
	setStream((OutputStream)stream);
}


}

