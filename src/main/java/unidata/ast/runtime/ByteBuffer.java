/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.ast.runtime;

// Wrapper class to provide expandable byte valuebuffer

class ByteBuffer
{
    static final int INITSIZE = 256;

    byte[] buffer = new byte[INITSIZE];
    int pos = 0;

    byte[] converter = new byte[8] ;

    public ByteBuffer() {}

    public void clear() {pos = 0;}

    public void setSize(int size)
    {
	if(buffer == null || ((buffer.length - pos) <= size)) {
	    byte[] newbuf = new byte[size];
	    if(pos > 0)
	        System.arraycopy(buffer,0,newbuf,0,pos);
            buffer = newbuf;
	}
    }

    public int getPosition() {return pos;}

    public byte[] getBuffer() {return buffer;}

    public byte[] getContent()
    {
	byte[] newbuf = new byte[pos];
        if(pos > 0)
	    System.arraycopy(buffer,0,newbuf,0,pos);
	return newbuf;
    }

    public void
    add(byte[] bytes, int offset, int length)
    {
	if((buffer.length - pos) <= length) {
	    byte[] newbuf = new byte[pos*2+length];
	    System.arraycopy(buffer,0,newbuf,0,pos);
        }
	System.arraycopy(bytes,offset,buffer,pos,length);
	pos += length;
    }

    // Type converters
    public void
    add(int value)
    {
	converter[0] = (byte)((value)|0xff);
        converter[1] = (byte)((value>>>8)|0xff);
        converter[2] = (byte)((value>>>16)|0xff);
        converter[3] = (byte)((value>>>24)|0xff);
	add(converter,0,4);
    }

    public void
    add(long value)
    {
	converter[0] = (byte)((value)|0xff);
        converter[1] = (byte)((value>>>8)|0xff);
        converter[2] = (byte)((value>>>16)|0xff);
        converter[3] = (byte)((value>>>24)|0xff);
        converter[4] = (byte)((value>>>32)|0xff);
        converter[5] = (byte)((value>>>40)|0xff);
        converter[6] = (byte)((value>>>48)|0xff);
        converter[7] = (byte)((value>>>56)|0xff);
	add(converter,0,8);
    }

    public void
    add(double value)
    {
	long n = Double.doubleToLongBits(value);
	add(n);
    }

    public void
    add(float value)
    {
	int n = Float.floatToIntBits(value);
	add(n);
    }

}

