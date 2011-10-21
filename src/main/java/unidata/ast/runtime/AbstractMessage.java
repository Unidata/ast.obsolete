/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.ast.runtime;

import java.io.IOException;


abstract public class AbstractMessage
{

public ASTRuntime rt = null;

public AbstractMessage() {this(null);}

public AbstractMessage(ASTRuntime rt) {this.rt = rt;}

// provide wrappers around rt methods

public boolean read(byte[] buf, int offset, int len) throws IOException
{return rt.read(buf, offset, len);}

public void write(int len, byte[] buf) throws IOException
{rt.write(len, buf);}

public void mark(int avail) throws IOException
{rt.mark(avail);}

public void unmark() throws IOException
{rt.unmark();}

public void skip_field(int wiretype, int fieldno) throws IOException
{rt.skip_field(wiretype, fieldno);}

public int getSize(int sort, double val) throws ASTException
{return rt.getSize(sort,val);}

public int getSize(int sort, float val) throws ASTException
{return rt.getSize(sort,val);}

public int getSize(int sort, boolean val) throws ASTException
{return rt.getSize(sort,val);}

public int getSize(int sort, String val) throws ASTException
{return rt.getSize(sort,val);}

public int getSize(int sort, byte[] val) throws ASTException
{return rt.getSize(sort,val);}

public int getSize(int sort, int val) throws ASTException
{return rt.getSize(sort,val);}

public int getSize(int sort, long val) throws ASTException
{return rt.getSize(sort,val);}

public int
getSizePacked(int sort, int[] val)
{return rt.getSizePacked(sort,val);}

public int
getSizePacked(int sort, long[] val)
{return rt.getSizePacked(sort,val);}

public int
getSizePacked(int sort, boolean[] val)
{return rt.getSizePacked(sort,val);}

public int
getSizePacked(int sort, float[] val)
{return rt.getSizePacked(sort,val);}

public int
getSizePacked(int sort, double[] val)
{return rt.getSizePacked(sort,val);}

public int getMessageSize(int size) throws ASTException
{return rt.getMessageSize(size);}

public int getTagSize(int sort, int fieldno) throws ASTException
{return rt.getTagSize(sort, fieldno);}

public void write_tag(int wiretype, int fieldno) throws IOException
{rt.write_tag(wiretype, fieldno);}

public boolean read_tag(int[] wiretype, int[] fieldno) throws IOException
{return rt.read_tag(wiretype, fieldno);}

public void write_size(int size) throws IOException
{rt.write_size(size);}

public int read_size() throws IOException
{return rt.read_size();}

public double read_primitive_double(int sort) throws IOException
{return rt.read_primitive_double(sort);}

public float read_primitive_float(int sort) throws IOException
{return rt.read_primitive_float(sort);}

public boolean read_primitive_boolean(int sort) throws IOException
{return rt.read_primitive_boolean(sort);}

public String read_primitive_string(int sort) throws IOException
{return rt.read_primitive_string(sort);}

public byte[] read_primitive_bytes(int sort) throws IOException
{return rt.read_primitive_bytes(sort);}

public int read_primitive_int(int sort) throws IOException
{return rt.read_primitive_int(sort);}

public long read_primitive_long(int sort) throws IOException
{return rt.read_primitive_long(sort);}

public double[] read_primitive_packed_double(int sort) throws IOException
{return rt.read_primitive_packed_double(sort);}

public float[] read_primitive_packed_float(int sort) throws IOException
{return rt.read_primitive_packed_float(sort);}

public boolean[] read_primitive_packed_bool(int sort) throws IOException
{return rt.read_primitive_packed_bool(sort);}

public int[] read_primitive_packed_int(int sort) throws IOException
{return rt.read_primitive_packed_int(sort);}

public long[] read_primitive_packed_long(int sort) throws IOException
{return rt.read_primitive_packed_long(sort);}

public void write_primitive(int sort, double val) throws IOException
{rt.write_primitive(sort, val);}

public void write_primitive(int sort, float val) throws IOException
{rt.write_primitive(sort, val);}

public void write_primitive(int sort, boolean val) throws IOException
{rt.write_primitive(sort, val);}

public void write_primitive(int sort, String val) throws IOException
{rt.write_primitive(sort, val);}

public void write_primitive(int sort, byte[] bval) throws IOException
{rt.write_primitive(sort, bval);}

public void write_primitive(int sort, long val) throws IOException
{rt.write_primitive(sort, val);}

public void write_primitive_packed(int sort, double[] val) throws IOException
{rt.write_primitive_packed(sort, val);}

public void write_primitive_packed(int sort, float[] val) throws IOException
{rt.write_primitive_packed(sort, val);}

public void write_primitive_packed(int sort, boolean[] val) throws IOException
{rt.write_primitive_packed(sort, val);}

public void write_primitive_packed(int sort, int[] val) throws IOException
{rt.write_primitive_packed(sort, val);}

public void write_primitive_packed(int sort, long[] val) throws IOException
{rt.write_primitive_packed(sort, val);}

public double[] repeat_append(int sort, double newval, double[] list)
{return rt.repeat_append(sort,newval,list);}

public float[] repeat_append(int sort, float newval, float[] list)
{return rt.repeat_append(sort,newval,list);}

public boolean[] repeat_append(int sort, boolean newval, boolean[] list)
{return rt.repeat_append(sort, newval, list);}

public int[] repeat_append(int sort, int newval, int[] list)
{return rt.repeat_append(sort, newval, list);}

public long[] repeat_append(int sort, long newval, long[] list)
{return rt.repeat_append(sort, newval, list);}

public String[] repeat_append(int sort, String newval, String[] list)
{return rt.repeat_append(sort, newval, list);}

public byte[][] repeat_append(int sort, byte[] newval, byte[][] list)
{return rt.repeat_append(sort, newval, list);}

public Object repeat_extend(Object list, java.lang.Class klass)
{return rt.repeat_extend( list, klass);}

//////////////////////////////////////////////////
// Miscellaneous utilities

static protected int
hex(char c)
{
    assert("0123456789ABCDEFabcdef".indexOf(c) >= 0);
    if(c >= 'a' && c <= 'f') return (int)((c - 'a')+10);
    if(c >= 'A' && c <= 'F') return (int)((c - 'F')+10);
    return (int)(c - '0'); // (c >= '0' && c <= '9')
}

public byte[]
makeByteString(String s)
    throws ASTException
{
    if(s.startsWith("0x")) s = s.substring(2);
    int len = s.length();
    if(len == 0) {s = "00"; len=2;}
    else if((len % 2) == 1) {s = s+"0"; len++;}
    byte[] bs = new byte[len/2];
    for(int i=0,j=0;i<len;i+=2,j++) {
	char c1 = s.charAt(i);
	char c2 = s.charAt(i+1);
	if("0123456789ABCDEFabcdef".indexOf(c1) < 0
	   || "0123456789ABCDEFabcdef".indexOf(c2) < 0)
	    throw new ASTException("Malformed byte constant: '"+s+"'");
	bs[j] = (byte)((hex(c1)&0xf)<<4|(hex(c2)&0xf));
    }
    return bs;
}

} // AbstractMessage

