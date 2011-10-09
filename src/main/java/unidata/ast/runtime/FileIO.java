/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.ast.runtime;

import java.io.*;

public class FileIO extends AbstractIO
{

/* Should be exactly eight characters long */
static final char BYTEIO_UIDSTRING[] = new char[]{'f','i','l','e','i','o',' ',' '};

// Constructor(s)

public
FileIO(IOmode mode, File file)
    throws IOException
{
    super();    
    if(file == null)
	throw new IOException("FileIO: null file argument");
    switch (mode) {
    case Ast_read:
	InputStream istream = new FileInputStream(file);
	construct(istream);
	break;
    case Ast_write:
	OutputStream ostream = new FileOutputStream(file);
	construct(ostream);	
	break;
    }
}

public
FileIO(Object stream)
    throws IOException
{
    super();
    if(stream == null)
	throw new IOException("FileIO: null file argument");
    construct(stream);
}

void
construct(Object stream)
    throws IOException
{
    if(stream instanceof InputStream)
	setStream((InputStream)stream);
    else if(stream instanceof OutputStream)
	setStream((OutputStream)stream);
}

}

