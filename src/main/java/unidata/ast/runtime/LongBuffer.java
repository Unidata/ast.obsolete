/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.ast.runtime;

// Wrapper class to provide expandable byte buffer

class LongBuffer
{
    static final int INITSIZE = 4;

    long[] buffer = null;
    int pos;

    public LongBuffer() {buffer = new long[INITSIZE]; pos = 0;}

    public long[] getContent()
    {
	long[] newbuf = new long[pos];
        if(pos > 0)
	    System.arraycopy(buffer,0,newbuf,0,pos);
	return newbuf;
    }

    public void
    add(long i)
    {
	if((buffer.length - pos) <= 0) {
	    long[] newbuf = new long[pos*2];
	    System.arraycopy(buffer,0,newbuf,0,pos);
        }
	buffer[pos++] = i;	
    }

}

