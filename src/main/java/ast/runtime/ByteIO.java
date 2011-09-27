/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.protobuf.ast.runtime;

public class ByteIO extends AbstractIO
{

/* Should be exactly eight characters long */
static final char BYTEIO_UIDSTRING[] = new char[8]{"byteio  "};

static uint64_t BYTEIO_UID = 0;

// Read constructor
public
Byteio(Runtime rt, IOmode mode, Object stream)
    throws IOException
{
    super(rt,mode);    
    switch (mode) {
    case AST_READ:
        if(stream == null)
	    throw new IOException("Byteio: no InputStream specified");
	setStream((InputStream)stream);
	break;
    case AST_WRITE:    case AST_WRITE:
        if(stream == null)
	    stream = new StringBufferOutputStream();
	setStream((OutputStream)stream);
	break;
    }
}

}

