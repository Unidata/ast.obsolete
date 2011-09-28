/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.protobuf.ast.runtime;

// Wrapper class to provide expandable byte buffer

class ASTBuffer
{
    byte[] buffer = new byte[Runtime.Sort.MAXJTYPESIZE];
    int pos = 0;

    public ASTBuffer()
    {
    }

    public ASTBuffer require(int space)
    {
	return setLength(space + buffer.length);
    }

    public byte[] getBuffer() {return buffer;}

    public ASTBuffer setLength(int len)
    {
	if(buffer.length < len) {
	    byte[] newbuf = new byte[len];
	    if(buffer.length > 0 && pos > 0)
		System.arraycopy(buffer,0,newbuf,0,pos);
	    buffer = newbuf;
        }
	return this;
    }

    public int  getLength() {return pos;};
    public int getAvail() {return buffer.length - pos;};

    public buf[] getContent()
    {
	byte[] newbuf = new byte[pos];
        if(pos > 0)
	    System.arraycopy(newbuf,0,buf,0,pos);
        }
	return newbuf;
    }

    public void write(byte[] b) {write(b,0,b.length);}

    public void
    write(byte[] b, int off, int len)
    {
	require(len);
	System.arraycopy(b,off,buffer,pos,len);
	pos += len;
    }

    public void
    write(int b)
    {
	require(1);
	buffer[pos++] = (byte)b;
	avail--;
    }

    public void rewind() {pos=0;}
}

