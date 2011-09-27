/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.protobuf.ast.runtime;

abstract public class AbstractIO
{

public enum IOmode {AST_READ, AST_WRITE;}

/* Max depth of the message tree */
static final int MAX_STACK_SIZE = 1024;
static final int MAXJTYPESIZE = 16; //bytes

//////////////////////////////////////////////////
// Instance fields

Runtime rt = null;
IOmode mode = IOmode.AST_READ; /* Write/Read/Free (WRF) */

InputStream istream = null;
OutputStream ostream = null;

int avail = Integer.MAX_VALUE; // ~ infinite

Stack<int> marks = new Stack<int>(); // track values of avail for reading only

//////////////////////////////////////////////////
// Constructor(s) 

public AbstractIO(Runtime rt, IOmode mode)
{
    this.rt = rt;
    this.mode = mode;
}

//////////////////////////////////////////////////
// set/get

void setStream(InputStream s) {istream = s;}
void setStream(OutputStream s) {ostream = s;}

IOmode getMode() {return mode;}

//////////////////////////////////////////////////
// Subclass overrideable ; typically if input/output streams
// do not provide sufficient semantics.

public void
write(long len, byte[] buf) /* writes stream n bytes at a time */
    throws IOException
{
    if(ostream != null)
	ostream.write(buf,0,len);    
}

public boolean
read(long len, byte[] buf) // reads stream n bytes at a time;
                           // return false if not enough bytes avail
    throws IOException
{
    return read(0,len,buf);
}

public boolean
read(long offset, long len, byte[] buf) // reads stream n bytes at a time;
                           // return false if not enough bytes avail
    throws IOException
{
    boolean ok = false;
    int count = -1;
    if(istream != null) {
	if(len > avail) break;
	int left = len;
	int pos = offset;
	while(left > 0) {
	    int count = istream.read(buf,pos,left);
	    if(count < 0) break;
	    left -= count;	    
	    pos += count;
	}
    }
    return ok;
}

public void
mark(long n)
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


