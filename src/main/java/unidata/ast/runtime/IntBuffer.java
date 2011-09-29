/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.ast.runtime;

// Wrapper class to provide expandable byte buffer

class IntBuffer
{
    static final int INITSIZE = 4;

    int[] buffer = null;
    int pos;

    public IntBuffer() {buffer = new int[INITSIZE]; pos = 0;}

    public int[] getContent()
    {
	int[] newbuf = new int[pos];
        if(pos > 0)
	    System.arraycopy(buffer,0,newbuf,0,pos);
	return newbuf;
    }

    public void
    add(int i)
    {
	if((buffer.length - pos) <= 0) {
	    int[] newbuf = new int[pos*2];
	    System.arraycopy(buffer,0,newbuf,0,pos);
        }
	buffer[pos++] = i;	
    }

}

