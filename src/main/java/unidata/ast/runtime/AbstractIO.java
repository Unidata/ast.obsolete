/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.ast.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;

abstract public class AbstractIO
{

public enum IOmode {Ast_read, Ast_write;}

/* Max depth of the message tree */
static final int MAX_STACK_SIZE = 1024;
static final int MAXJTYPESIZE = 16; //bytes

//////////////////////////////////////////////////
// Instance fields

IOmode mode = null;

InputStream istream = null;
OutputStream ostream = null;

int avail = Integer.MAX_VALUE; // ~ infinite

Stack<Integer> marks = new Stack<Integer>(); // track values of avail for reading only

//////////////////////////////////////////////////
// Constructor(s) 

public AbstractIO(IOmode mode)
{
    this.mode = mode;
}

//////////////////////////////////////////////////
// set/get

public void setStream(InputStream s) {istream = s;}
public InputStream getInputStream() {return istream;}

public void setStream(OutputStream s) {ostream = s;}
public OutputStream getOutputStream() {return ostream;}

public IOmode getMode() {return mode;}
public void setMode(IOmode mode) {this.mode = mode;}

//////////////////////////////////////////////////
// Subclass overrideable ; typically if input/output streams
// do not provide sufficient semantics.

public void
write(int len, byte[] buf) /* writes stream n bytes at a time */
    throws IOException
{
    if(ostream != null)
        ostream.write(buf,0,len);    
}

// This does not throw exception so we can
// programmatically determine eof.
public boolean
read(int len, byte[] buf) // reads stream n bytes at a time;
                           // return false if not enough bytes avail
    throws IOException
{
    return read(0,len,buf);
}

public boolean
read(int offset, int len, byte[] buf) // reads stream n bytes at a time;
                           // return false if not enough bytes avail
    throws IOException
{
    boolean ok = false;
    int count = -1;
    if(istream != null && len <= avail) {
	int left = len;
	int pos = offset;
	while(left > 0) {
	    count = istream.read(buf,pos,left);
	    if(count < 0) break;
	    left -= count;	    
	    pos += count;
	}
    }
    return ok;
}

public void
mark(int  n)
    throws IOException /* limit reads to n bytes */
{
    if(n <= 0)
	throw new IOException("AbstractIO.mark: illegal argument "+n);
    marks.push(avail);
    avail = n;
}

public void
unmark() /* restore previous markn limit */
    throws IOException 
{
    if(marks.empty())
	throw new IOException("AbstractIO.unmark: empty stack");
    avail = marks.pop();
}

public void
close()  /* reclaim this runtime instance */
    throws IOException
{
    // do nothing
}

} /*class AbstractIO*/


