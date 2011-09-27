/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.protobuf.ast.runtime;

// Wrapper class to hide buffer details

class ByteBuffer
{
    byte[] buffer = new byte[Runtime.MAXJTYPESIZE];    
    int pos = 0
    int avail = buffer.length;

    public ByteBuffer()
    {
    }

    public ByteBuffer require(int space)
    {
	return setRawBuffer(space + buffer.length);
    }

    public buf[] getRawBuffer() {return buffer};

    public ByteBuffer setRawBuffer(int len)
    {
	if(buffer.length < len) {
	    byte[] newbuf = new byte[len];
	    if(buffer.length > 0 && pos > 0)
		System.arraycopy(buffer,0,newbuf,0,pos);
	    buffer = newbuf;
        }
	avail = buffer.length - pos; // reset
	return this;
    }

    public int getAvail() {return avail;};

    public int  getLength() {return pos;};

    public ByteBuffer setLength(int len)
    {
	if(len > buffer.length) setRawBuffer(len);
	pos = len;
	avail = buffer.length - pos;			
	return this;
    }

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
	avail -= len;
    }

    public void
    write(int b)
    {
	require(1);
	buffer[pos++] = (byte)b;
	avail--;
    }

}

