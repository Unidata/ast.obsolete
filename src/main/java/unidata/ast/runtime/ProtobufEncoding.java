/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *********************************************************************/

// This code contains code taken from Google's protobuf implementation
// Google code copyright follows:
// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
// http://code.google.com/p/protobuf/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


package unidata.ast.runtime;

import java.io.IOException;
import java.lang.reflect.Array;

import static unidata.ast.runtime.ASTRuntime.*;
import static unidata.ast.runtime.ASTRuntime.Sort.*;
import static unidata.ast.runtime.ASTRuntime.Wiretype.*;

public class ProtobufEncoding extends Encoding
{

//////////////////////////////////////////////////

static public int wiretype(int sort)
{
	switch (sort) {
	case Ast_float: case Ast_fixed32: case Ast_sfixed32:
	    return Wiretype.Ast_32bit;
	case Ast_double: case Ast_fixed64: case Ast_sfixed64:
	    return Wiretype.Ast_64bit;
	case Ast_string: case Ast_bytes: case Ast_message:
	    return Wiretype.Ast_counted;
	default:
	}
	return Wiretype.Ast_varint;
}

static public int wiresize(int sort)
  {
      switch (sort) {
      case Ast_float: case Ast_fixed32: case Ast_sfixed32: case Ast_enum: return 4;
      case Ast_double: case Ast_fixed64: case Ast_sfixed64: return 8;
      case Ast_bool: return 1;
      default: break;
      }
      return 0;
  }

//////////////////////////////////////////////////
// Instance fields

byte[] valuebuffer = new byte[Sort.MAXTYPESIZE]; // size is max needed except for string|bytes|packed
                                                 // used to encode single primitive values
byte[] sizebuffer = new byte[Sort.MAXTYPESIZE]; // used to encode tags and sizes.

ByteBuffer varintbuffer = new ByteBuffer(); // for writing packed data that is encoded as varints

//////////////////////////////////////////////////
// Constructor(s)

public ProtobufEncoding()
    throws ASTException
{
    super();
}

//////////////////////////////////////////////////
/* Non overriding methods */

protected int
readvarint(byte[] buffer)
    throws IOException
{
    return readwirevalue(Wiretype.Ast_varint, buffer);
}

/* Based on the wiretype, extract the proper number of bytes
   for an integer base value; return the length
   and place the bytes into the valuebuffer.
   For ast counted, do this for the count,
   not the content.
*/

protected int
readwirevalue(int wiretype, byte[] buffer)
    throws IOException
{
    int count = 0;
    switch (wiretype) {
    case Ast_varint:
        for(int i=0;i<buffer.length;i++) {
	    if(!io.read(buffer, i, 1))  {
                count = -1; // eof
                break;
            }
	    if((0x80 & buffer[i]) == 0) {
                count = i+1;
                break;
            }
        }
	break;
    case Ast_32bit:
	count = 4;
        if(!io.read(buffer, 0,4)) count = -1;
	break;
    case Ast_64bit:
	count = 8;
        if(!io.read(buffer, 0,8)) count = -1;
	break;
    case Ast_counted: /* get the count */
        int len = readwirevalue(Ast_varint, buffer);
	count = uint32_decode(len,buffer);
	break;
    default:
	throw new ASTException("Unexpected wiretype: "+wiretype);
    }
    return count;
}

//////////////////////////////////////////////////
/* Given an unknown field, skip past it */
void
skip_field(int wiretype, int fieldno)
    throws IOException
{
    int len;
    switch (wiretype) {
    case Wiretype.Ast_varint:
        len = readvarint(valuebuffer);
	break;
    case Wiretype.Ast_32bit:
        read(valuebuffer, 0,(len=4));
	break;
    case Wiretype.Ast_64bit:
	len = 8;
        read(valuebuffer, 0,(len=8));
	break;
    case Wiretype.Ast_counted:
        len = readvarint(valuebuffer);
        /* get the count */
	len = uint32_decode(len, valuebuffer);
	/* Now skip "len" bytes */
	while(len > 0) {
	    int count = (len > valuebuffer.length? valuebuffer.length:len);
	    if(!read(valuebuffer, 0,count))
		throw new ASTException("skip_field: too few bytes");
	    len -= count;
	}
	break;
    default:
	throw new ASTException("skip_field: unexpected wiretype: "+wiretype);
    }
}

//////////////////////////////////////////////////
/* Procedure to calulate size of a value;
   note that this is the size of an actual
   value.
 */

int
getSize(int sort, float val)
{
    assert(sort == Sort.Ast_float);
    return 4;
}

int
getSize(int sort, double val)
{
    assert(sort == Sort.Ast_double);
    return 8;
}

int
getSize(int sort, boolean val)
{
    assert(sort == Sort.Ast_bool);
    return 1;
}

int
getSize(int sort, String val)
{
    assert(sort == Sort.Ast_string) ;
    /* string count is size for length counter + strlen(string) */
    int count = 0;
    if(val != null) {
	int slen = val.length();
        count = uint32_size(slen);
	count += slen;
    }
    return count;
}

int
getSize(int sort, byte[] val)
{
    assert(sort == Sort.Ast_bytes);
    int count = 0;
    if(val != null) {
	count = uint32_size(val.length);
        count += val.length;
    }
    return count;
}

int
getSize(int sort, long val)
        throws ASTException
{
    switch (sort) {
    case Sort.Ast_enum: /* fall thru */
    case Sort.Ast_int32:
	return int32_size((int)val);
    case Sort.Ast_int64:
	return int64_size(val);
    case Sort.Ast_uint32:
	return uint32_size((int)val);
    case Sort.Ast_uint64:
	return uint64_size(val);
    case Sort.Ast_sint32:
	return sint32_size((int)val);
    case Sort.Ast_sint64:
	return sint64_size(val);
    case Sort.Ast_fixed32:
	return 4;
    case Sort.Ast_sfixed32:
	return 4;
    case Sort.Ast_fixed64:
	return 8;
    case Sort.Ast_sfixed64:
	return 8;
    default:
	break;
    }
    throw new ASTException("Unexpected Sort: "+sort);
}

int
getSizePacked(int sort, int[] val)
{
    switch (wiretype(sort)) {
    case Wiretype.Ast_varint:
	// Sigh! we have to walk it
	int size = 0;
	for(int i=0;i<val.length;i++)
	    size += int64_size((long)val[i]);
	return size;
    case Wiretype.Ast_32bit:
	return 4*val.length;
    default: break;
    }
    throw new ASTRuntimeException("Illegal packed Sort");
}

int
getSizePacked(int sort, long[] val)
{
    switch (wiretype(sort)) {
    case Wiretype.Ast_varint:
	// Sigh! we have to walk it
	int size = 0;
	for(int i=0;i<val.length;i++)
	    size += int64_size(val[i]);
	return size;
    case Wiretype.Ast_64bit:
	return 8*val.length;
    default: break;
    }
    throw new ASTRuntimeException("Illegal packed Sort");
}

int
getSizePacked(int sort, float[] val)
{
    return 4*val.length;
}

int
getSizePacked(int sort, double[] val)
{
    return 8*val.length;
}

int
getSizePacked(int sort, boolean[] val)
{
    return 1*val.length;
}


//////////////////////////////////////////////////
/* Procedure to calculate the size a message
   including its prefix size, when given
   the unprefixed message size
 */
int
getMessageSize(int size)
{
    int count = uint32_encode(size,sizebuffer);
    return count+size;
}

/* Procedure to calulate size of a tag */
int
getTagSize(int sort, int fieldno)
{
    int wiretype = wiretype(sort);
    int count = encode_tag(wiretype,fieldno, sizebuffer);
    return count;
}

//////////////////////////////////////////////////
/* Procedures to read/write tags */
void
write_tag(int sort, int fieldno)
    throws IOException
{
    int wiretype = wiretype(sort);
    int count = encode_tag(wiretype,fieldno, sizebuffer);
    io.write(count, sizebuffer);
}

/* Procedure to extract tags; args simulate call by ref */
boolean
read_tag(int[] wiretype, int[] fieldno)
    throws IOException
{
    int count;
    int key;

    /* Extract the wiretype + index */
    count = readvarint(sizebuffer);
    if(count < 0) return false;

    /* convert from varint */
    key = uint32_decode(count, sizebuffer);

    /* Extract the wiretype and fieldno */
    wiretype[0] = (key & 0x7);
    fieldno[0] = (key >>> 3);

    return true;
}

/* Procedures to write out counts */
void
write_size(int size)
    throws IOException
{
    /* write size as varint */
    int len = uint32_encode(size, sizebuffer);
    io.write(len, sizebuffer);
}

/* Procedure to extract size */
int
read_size()
    throws IOException
{
    int len = readvarint(sizebuffer);
    if(len < 0) return len;
    return uint32_decode(len, sizebuffer);
}

//////////////////////////////////////////////////
//////////////////////////////////////////////////

double
read_primitive_double(int sort)
    throws IOException
{
    assert(sort == Sort.Ast_double);
    int wiretype = wiretype(sort);
    int len = readwirevalue(wiretype, valuebuffer);
    double value = float64_decode(len, valuebuffer);
    return value;
}

float
read_primitive_float(int sort)
    throws IOException
{
    assert(sort == Sort.Ast_float);
    int wiretype = wiretype(sort);
    int len = readwirevalue(wiretype, valuebuffer);
    float value = float32_decode(len, valuebuffer);
    return value;
}

boolean
read_primitive_boolean(int sort)
    throws IOException
{
    assert(sort == Sort.Ast_bool);
    int wiretype = wiretype(sort);
    int len = readwirevalue(wiretype, valuebuffer);
    boolean value = bool_decode(len, valuebuffer);
    return value;
}

String
read_primitive_string(int sort)
    throws IOException
{
    assert(sort == Sort.Ast_string);
    int wiretype = wiretype(sort);
    int len = readwirevalue(wiretype, valuebuffer);
    byte[] stringbuf = new byte[len];
    if(!read(stringbuf, 0,len))
	throw new ASTException("too few bytes");
    return new String(stringbuf,utf8);
}

byte[]
read_primitive_bytes(int sort)
    throws IOException
{
    assert(sort == Sort.Ast_bytes);
    int wiretype = wiretype(sort);
    int len = readwirevalue(wiretype, valuebuffer);
    byte[] value = new byte[len];
    if(!read(value, 0,len))
	throw new ASTException("too few bytes");
    return value;
}

int
read_primitive_int(int sort)
    throws IOException
{
    /* compute the wiretype from the sort */
    int wiretype = wiretype(sort);
    int len = readwirevalue(wiretype, valuebuffer);
    switch (sort) {
    case Sort.Ast_enum: /* fall thru */
    case Sort.Ast_int32:
        return int32_decode(len, valuebuffer);
    case Sort.Ast_uint32:
        return uint32_decode(len, valuebuffer);
    case Sort.Ast_sint32:
        return sint32_decode(len, valuebuffer);
    case Sort.Ast_fixed32:
        return fixed32_decode(4, valuebuffer);
    case Sort.Ast_sfixed32:
        return fixed32_decode(4, valuebuffer);
    default: break;
    }
    throw new ASTException("Unexpected sort: " + sort);
}

long
read_primitive_long(int sort)
    throws IOException
{
    /* compute the wiretype from the sort */
    int wiretype = wiretype(sort);
    int len = readwirevalue(wiretype, valuebuffer);
    switch (sort) {
    case Sort.Ast_int64:
        return int64_decode(len, valuebuffer);
    case Sort.Ast_uint64:
        return uint64_decode(len, valuebuffer);
    case Sort.Ast_sint64:
        return sint64_decode(len, valuebuffer);
    case Sort.Ast_fixed64:
        return fixed64_decode(8, valuebuffer);
    case Sort.Ast_sfixed64:
        return fixed64_decode(8, valuebuffer);
    default: break;
    }
    throw new ASTException("Unexpected sort: " + sort);
}

double[]
read_primitive_packed_double(int sort)
    throws IOException
{
    // extract the count
    assert(sort == Sort.Ast_double);
    int wiretype = Wiretype.Ast_counted;
    int size = readwirevalue(wiretype, valuebuffer);
    int sizeof = wiresize(sort);

    // Pull out the values as doubles
    int ndoubles = size/sizeof;
    double[] output = new double[ndoubles];

    // Read bytes and insert into the converter
    for(int i=0;i<ndoubles;i++) {
	if(!read(valuebuffer, 0,sizeof))
	    throw new ASTException("too few bytes");
	output[i] = float64_decode(8, valuebuffer);
    }	
    return output;
}

float[]
read_primitive_packed_float(int sort)
    throws IOException
{
    // extract the count
    assert(sort == Sort.Ast_float);
    int wiretype = Wiretype.Ast_counted;
    int size = readwirevalue(wiretype, valuebuffer);
    int sizeof = wiresize(sort);

    // Pull out the values as floats
    int nfloats = size/sizeof;
    float[] output = new float[nfloats];

    // Read bytes and insert into the converter
    for(int i=0;i<nfloats;i++) {
	if(!read(valuebuffer, 0,size))
	    throw new ASTException("too few bytes");
	output[i] = float32_decode(4, valuebuffer);
    }	
    return output;
}

boolean[]
read_primitive_packed_bool(int sort)
    throws IOException
{
    // extract the count
    assert(sort == Sort.Ast_bool);
    int wiretype = Wiretype.Ast_counted;
    int size = readwirevalue(wiretype, valuebuffer);
    int sizeof = wiresize(sort);

    // Pull out the values as bools
    int nbools = size/sizeof;
    boolean[] output = new boolean[nbools];

    // Read bytes and insert into the converter
    for(int i=0;i<nbools;i++) {
	if(!read(valuebuffer, 0,size))
	    throw new ASTException("too few bytes");
	output[i] = bool_decode(size, valuebuffer);
    }	
    return output;
}

// Read a sequence of values that are expected to be 32 bit integers
// i.e. sort= Ast_int32,Ast_sint32,Ast_fixed32,Ast_sfixed32

int[]
read_primitive_packed_int(int sort)
    throws IOException
{
    // extract the count; put data into valuebuffer (except for Wiretype.Ast_counted)
    int size = readwirevalue(Wiretype.Ast_counted, valuebuffer);
    int len = 0;
    Ibuffer intdata = new Ibuffer();


// i.e. sort= Ast_(u)int32,Ast_sint32,Ast_fixed32,Ast_sfixed32
loop: for(int avail=size;avail > 0;) {
	switch (wiretype(sort)) {
        case Wiretype.Ast_varint:
            len = readvarint(valuebuffer);
	    if(len < 0) break loop;
            avail -= len;
	    break;
        case Wiretype.Ast_32bit:
            len = readwirevalue(Wiretype.Ast_32bit, valuebuffer);
	    if(len < 0) break loop;
            avail -= len;
	    break;
        default:
	    throw new ASTException("illegal wiretype: " + wiretype(sort));
        }
        switch (sort) {
        case Sort.Ast_int32:
	    intdata.add(int32_decode(len, valuebuffer));
	    break;			
        case Sort.Ast_uint32:
	    intdata.add(uint32_decode(len, valuebuffer));
	    break;			
        case Sort.Ast_sint32:
	    intdata.add(sint32_decode(len, valuebuffer));
	    break;			
        case Sort.Ast_fixed32:
	    intdata.add(fixed32_decode(4, valuebuffer));
	    break;
        case Sort.Ast_sfixed32:
	    intdata.add(fixed32_decode(4, valuebuffer));
	    break;
        default: break;
        }
    } // for
    return intdata.getContent();
}

// Read a sequence of values that are expected to be 64 bit integers
// i.e. sort= Ast_int64,Ast_sint64,Ast_fixed64,Ast_sfixed64

long[]
read_primitive_packed_long(int sort)
    throws IOException
{
    // extract the count
    int count = readwirevalue(Wiretype.Ast_counted, valuebuffer);
    int size = wiresize(sort); // 0 => unknown => varint
    int len = 0;
    Lbuffer longdata = new Lbuffer();

// i.e. sort= Ast_(u)int64,Ast_sint64,Ast_fixed64,Ast_sfixed64
loop: for(int avail=size;avail > 0;) {
	switch (wiretype(sort)) {
        case Wiretype.Ast_varint:
            len = readvarint(valuebuffer);
	    if(len < 0) break loop;
            avail -= len;
	    break;
        case Wiretype.Ast_64bit:
            len = readwirevalue(Wiretype.Ast_64bit, valuebuffer);
	    if(len < 0) break loop;
            avail -= len;
	    break;
        default:
	    throw new ASTException("illegal wiretype: " + wiretype(sort));
        }

        switch (sort) {
        case Sort.Ast_int64:
	    longdata.add(int64_decode(len, valuebuffer));
	    break;			
        case Sort.Ast_uint64:
	    longdata.add(uint64_decode(len, valuebuffer));
	    break;			
        case Sort.Ast_sint64:
	    longdata.add(sint64_decode(len, valuebuffer));
	    break;			
        case Sort.Ast_fixed64:
	    longdata.add(fixed64_decode(8, valuebuffer));
	    break;
        case Sort.Ast_sfixed64:
	    longdata.add(fixed64_decode(8, valuebuffer));
	    break;
        default: break;
        }
    } // for
    return longdata.getContent();
}

void
write_primitive(int sort, double val)
    throws IOException
{
    assert(sort == Sort.Ast_double);
    int count = float64_encode(val, valuebuffer);
    write(count, valuebuffer);
}

void
write_primitive(int sort, float val)
    throws IOException
{
    assert(sort == Sort.Ast_float);
    int count = float32_encode(val, valuebuffer);
    write(count, valuebuffer);
}

void
write_primitive(int sort, boolean val)
    throws IOException
{
    assert(sort == Sort.Ast_bool);
    int count = bool_encode(val, valuebuffer);
    write(count, valuebuffer);
}

void
write_primitive(int sort, String val)
        throws IOException
{
    assert(sort == Sort.Ast_string);
    byte[] bval = val.getBytes(utf8);
    int len = bval.length;
    write_size(len);
    write(len,bval);
}

void
write_primitive(int sort, byte[] bval)
        throws IOException
{
    assert(sort == Sort.Ast_bytes);
    int len = bval.length;
    write_size(len);
    write(len,bval);
}

void
write_primitive(int sort, long val)
        throws IOException
{
    int ival = (int)(val & 0xffffffff);
    int count = 0;
    /* Write the data in proper wiretype format using the sort */
    switch (sort) {
    case Sort.Ast_int32:
	count = int32_encode(ival, valuebuffer);
        write(count, valuebuffer);
	break;
    case Sort.Ast_int64:
	count = int64_encode(val, valuebuffer);
        write(count, valuebuffer);
	break;
    case Sort.Ast_uint32:
	count = uint32_encode(ival, valuebuffer);
        write(count, valuebuffer);
	break;
    case Sort.Ast_uint64:
	count = uint64_encode(val, valuebuffer);
	write(count, valuebuffer);
	break;
    case Sort.Ast_sint32:
	count = sint32_encode(ival, valuebuffer);
        write(count, valuebuffer);
	break;
    case Sort.Ast_sint64:
	count = sint64_encode(val, valuebuffer);
        write(count, valuebuffer);
	break;
    case Sort.Ast_enum:
	count = int32_encode(ival, valuebuffer);
        write(count, valuebuffer);
	break;
    case Sort.Ast_fixed32: /* fall thru */
    case Sort.Ast_sfixed32: /* fall thru */
	count = fixed32_encode(ival, valuebuffer);
        write(count, valuebuffer);
	break;
    case Sort.Ast_fixed64:  /* fall thru */
    case Sort.Ast_sfixed64: /* fall thru */
	count = fixed64_encode(val, valuebuffer);
        write(count, valuebuffer);
	break;
    default:
	throw new ASTException("unexpected sort: "+sort);
    }
}

void
write_primitive_packed(int sort, double[] val)
    throws IOException
{
    assert(sort == Sort.Ast_double);
    if(val == null) return;
    int size = getSizePacked(sort,val);
    write_size(size);
    for(int i=0;i<val.length;i++)
	write_primitive(Sort.Ast_double,val[i]);
}

void
write_primitive_packed(int sort, float[] val)
        throws IOException
{
    assert(sort == Sort.Ast_float);
    if(val == null) return;
    int size = getSizePacked(sort,val);
    write_size(size);
    for(int i=0;i<val.length;i++)
	write_primitive(Sort.Ast_float,val[i]);
}

void
write_primitive_packed(int sort, boolean[] val)
        throws IOException
{
    assert(sort == Sort.Ast_bool);
    if(val == null) return;
    int size = getSizePacked(sort,val);
    write_size(size);
    for(int i=0;i<val.length;i++)
	write_primitive(Sort.Ast_bool,val[i]);
}

void
write_primitive_packed(int sort, int[] val)
        throws IOException
{
    if(val == null) return;
    // ugh; we need to encode into a memory valuebuffer
    // to see the correct length for varints
    int count = 0;
    varintbuffer.clear();
    varintbuffer.setSize(val.length*Sort.MAXVARINTSIZE); // max possible size
    switch (sort) {
    case Sort.Ast_enum:
    case Sort.Ast_int32:
	for(int i=0;i<val.length;i++) {
	    count = int32_encode(val[i], valuebuffer);
	    varintbuffer.add(valuebuffer,0,count);
	}
	break;
    case Sort.Ast_uint32:
	for(int i=0;i<val.length;i++) {
	    count = uint32_encode(val[i], valuebuffer);
	    varintbuffer.add(valuebuffer,0,count);
	}
	break;
    case Sort.Ast_sint32:
	for(int i=0;i<val.length;i++) {
	    count = sint32_encode(val[i], valuebuffer);
	    varintbuffer.add(valuebuffer,0,count);
	}
	break;
    case Sort.Ast_fixed32: /* fall thru */
    case Sort.Ast_sfixed32: /* fall thru */
    case Sort.Ast_float:
	for(int i=0;i<val.length;i++) {
  	    count = fixed32_encode(val[i], valuebuffer);
	    varintbuffer.add(valuebuffer,0,count);
	}
	break;
    default:
	throw new ASTException("unexpected sort: "+sort);
    }
    // write the count and the data
    int len = varintbuffer.getPosition();
    byte[] data = varintbuffer.getBuffer();
    write_size(len);
    write(len,data);
}

void
write_primitive_packed(int sort, long[] val)
        throws IOException
{
    if(val == null) return;
    int count = 0;

    varintbuffer.clear();
    varintbuffer.setSize(val.length*Sort.MAXVARINTSIZE); // max possible size

    switch (sort) {
    case Sort.Ast_enum:
    case Sort.Ast_int64:
	for(int i=0;i<val.length;i++) {
	    count = int64_encode(val[i], valuebuffer);
	    varintbuffer.add(valuebuffer,0,count);
	}
	break;
    case Sort.Ast_uint64:
	for(int i=0;i<val.length;i++) {
	    count = uint64_encode(val[i], valuebuffer);
	    varintbuffer.add(valuebuffer,0,count);
	}
	break;
    case Sort.Ast_sint64:
	for(int i=0;i<val.length;i++) {
	    count = sint64_encode(val[i], valuebuffer);
	    varintbuffer.add(valuebuffer,0,count);
	}
	break;
    case Sort.Ast_fixed64: /* fall thru */
    case Sort.Ast_sfixed64: /* fall thru */
    case Sort.Ast_float:
	for(int i=0;i<val.length;i++) {
  	    count = fixed64_encode(val[i], valuebuffer);
	    varintbuffer.add(valuebuffer,0,count);
	}
	break;
    default:
	throw new ASTException("unexpected sort: "+sort);
    }
    // write the count and the data
    int len = varintbuffer.getPosition();
    byte[] data = varintbuffer.getBuffer();
    write_size(len);
    write(len,data);
}

/* Read into Repeated field */
double[]
repeat_append(int sort, double newval, double[] list)
{
    if(list == null) {list = new double[1];}
    else {
	double[] newlist = new double[list.length+1];
	System.arraycopy(list,0,newlist,0,list.length);
        list = newlist;
    }
    list[list.length-1] = newval;
    return list;
}

float[]
repeat_append(int sort, float newval, float[] list)
{
    if(list == null) {list = new float[1];}
    else {
	float[] newlist = new float[list.length+1];
	System.arraycopy(list,0,newlist,0,list.length);
        list = newlist;
    }
    list[list.length-1] = newval;
    return list;
}

boolean[]
repeat_append(int sort, boolean newval, boolean[] list)
{
    if(list == null) {list = new boolean[1];}
    else {
	boolean[] newlist = new boolean[list.length+1];
	System.arraycopy(list,0,newlist,0,list.length);
        list = newlist;
    }
    list[list.length-1] = newval;
    return list;
}

int[]
repeat_append(int sort, int newval, int[] list)
{
    if(list == null) {list = new int[1];}
    else {
	int[] newlist = new int[list.length+1];
	System.arraycopy(list,0,newlist,0,list.length);
        list = newlist;
    }
    list[list.length-1] = newval;
    return list;
}


long[]
repeat_append(int sort, long newval, long[] list)
{
    if(list == null) {list = new long[1];}
    else {
	long[] newlist = new long[list.length+1];
	System.arraycopy(list,0,newlist,0,list.length);
        list = newlist;
    }
    list[list.length-1] = newval;
    return list;
}

String[]
repeat_append(int sort, String newval, String[] list)
{
    if(list == null) {list = new String[1];}
    else {
	String[] newlist = new String[list.length+1];
	System.arraycopy(list,0,newlist,0,list.length);
        list = newlist;
    }
    list[list.length-1] = newval;
    return list;
}

byte[][]
repeat_append(int sort, byte[] newval, byte[][] list)
{
    if(list == null) {list = new byte[1][];}
    else {
	byte[][] newlist = new byte[list.length+1][];
	System.arraycopy(list,0,newlist,0,list.length);
        list = newlist;
    }
    list[list.length-1] = newval;
    return list;
}

// Special handling for messages and enums
Object
repeat_extend(Object list, java.lang.Class klass)
{
    int len = (list==null?0:Array.getLength(list));
    Object newlist = Array.newInstance(klass,len+1);
    if(len > 0) System.arraycopy(list,0,newlist,0,len);
    list = newlist;
    return list;
}

//////////////////////////////////////////////////
// Implement Encoding abstract methods

/* Return the number of bytes required to store the
   tag for the field, which includes 3 bits for
   the wire-type, and a single bit that denotes the end-of-tag.
*/
static int
encode_tag(int wiretype, int fieldno, byte[] buffer)
{
    int key;
    int count;

    key = (wiretype | (fieldno << 3));
    /* convert key to varint */
    count = uint32_encode(key,buffer);
    return count;
}


/* Pack an unsigned 32-bit integer in base-128 encoding, and
   return the number of bytes needed: this will be 5 or
   less. Note that for java, unsigned ints are encoded
   as signed ints but with the bit pattern proper
   for unsigned ints.
*/

static int
uint32_encode(int value, byte[] out)
{
    int count = 0;
    while (true) {
      if ((value & ~0x7F) == 0) {
	out[count++] = (byte)(value & 0x7f);
	break;
      } else {
	out[count++] = (byte)((value & 0x7f)|0x80);
        value >>>= 7;
      }
    }
    return count;
}

/* Pack a 32-bit signed integer, returning the number of
   bytes needed.  Negative numbers are packed as
   twos-complement 64-bit integers.
*/

static int
int32_encode(int value, byte[] out)
{
  if(value >= 0)
    return uint32_encode(value,out);
  else
    return uint64_encode(value,out);
}

/*
Pack a 32-bit integer in zigwag 
*/

static int
sint32_encode(int value, byte[] out)
{
  return uint32_encode(zigzag32(value), out);
}

/* Pack a 64-bit unsigned integer that fits in a 64-bit uint,
   using base-128  */

static int
int64_encode(long value, byte[] out)
{
    return uint64_encode(value,out);
}

static int
uint64_encode(long value, byte[] out)
{
    int count = 0;
    while (true) {
      if ((value & ~0x7f) == 0) {
	out[count++] = (byte)(value & 0x7f);
	break;
      } else {
	out[count++] = (byte)((value & 0x7f)|0x80);
        value >>>= 7;
      }
    }
    return count;
}

/* Pack a 64-bit signed integer in zigzag encoding, return
   the size of the packed output.  (Max returned value is 10)
*/

static int
sint64_encode(long value, byte[] out)
{
  return uint64_encode(zigzag64(value), out);
}

/* Pack a 32-bit value, little-endian.  Used for fixed32,
   sfixed32, float
*/

static int
fixed32_encode(int value, byte[] out)
{
  // Protobuf apparently uses little endian
  out[0] = (byte)((value)&0xff);
  out[1] = (byte)((value>>>8)&0xff);
  out[2] = (byte)((value>>>16)&0xff);
  out[3] = (byte)((value>>>24)&0xff);
  return 4;
}

/* Pack a 64-bit fixed-length value.
   (Used for fixed64,sfixed64, double)
*/

/* Protobuf writes little endian */

static int
fixed64_encode(long value, byte[] out)
{
  // Protobuf apparently uses little endian
  out[0] = (byte)((value)&0xff);
  out[1] = (byte)((value>>>8)&0xff);
  out[2] = (byte)((value>>>16)&0xff);
  out[3] = (byte)((value>>>24)&0xff);
  out[4] = (byte)((value>>>32)&0xff);
  out[5] = (byte)((value>>>40)&0xff);
  out[6] = (byte)((value>>>48)&0xff);
  out[7] = (byte)((value>>>56)&0xff);
  return 8;
}

/* Pack a boolean as 0 or 1, even though the bool_t
   can really assume any integer value.
*/

/* XXX: perhaps on some platforms "*out = !!value" would be
   a better impl, b/c that is idiotmatic c++ in some stl impls. */

static int
bool_encode(boolean value, byte[] out)
{
  out[0] = (byte) (value ? 1 : 0);
  return 1;
}

static int
float32_encode(float value, byte[] out)
{
   int i = Float.floatToIntBits(value);
   return fixed32_encode(i,out);
}

static int
float64_encode(double value, byte[] out)
{
   long l = Double.doubleToLongBits(value);
   return fixed64_encode(l,out);
}


/* Decode a 32 bit varint */
static int
uint32_decode(int len, byte[] data)
{
    int pos=0;
    byte tmp = data[pos++];
    if(tmp >= 0)
      return (int)tmp;
    int result = tmp & 0x7f;
    if ((tmp = data[pos++]) >= 0) {
      result |= tmp << 7;
    } else {
      result |= (tmp & 0x7f) << 7;
      if ((tmp = data[pos++]) >= 0) {
        result |= tmp << 14;
      } else {
        result |= (tmp & 0x7f) << 14;
        if ((tmp = data[pos++]) >= 0) {
          result |= tmp << 21;
        } else {
          result |= (tmp & 0x7f) << 21;
          result |= (tmp = data[pos++]) << 28;
	}
      }
    }
    return result;
}

static int
int32_decode(int len, byte[] data)
{
    return uint32_decode(len, data);
}

/* Decode possibly 64-bit varint*/
static long
uint64_decode(int len, byte[] data)
{
    int pos = 0;
    int shift = 0;
    long result = 0;
    while (shift < 64) {
      byte b = data[pos++];
      result |= (long)(b & 0x7F) << shift;
      if ((b & 0x80) == 0) break;
      shift += 7;
    }
    return result;
}

static long
int64_decode(int len, byte[] data)
{
  return uint64_decode(len, data);
}

static int
sint32_decode(int len, byte[] data)
{
    return unzigzag32(uint32_decode(len, data));
}

static long
sint64_decode(int len, byte[] data)
{
    return unzigzag64(uint64_decode(len, data));
}

/* remember: protobuf writes little-endian */
static int
fixed32_decode(int len, byte[] data)
{
  int rv = (
        (((int)data[0])&0xff)
      | (((int)data[1]&0xff) << 8)
      | (((int)data[2]&0xff) << 16)
      | (((int)data[3]&0xff) << 24)
      );
  return rv;
}

static long
fixed64_decode(int len, byte[] data)
{
  long rv = 0;
      rv |= (((long)data[0])&0xff);
      rv |=(((long)data[1]&0xff) << 8);
      rv |=(((long)data[2]&0xff) << 16);
      rv |=(((long)data[3]&0xff) << 24);
      rv |=(((long)data[4]&0xff) << 32);
      rv |=(((long)data[5]&0xff) << 40);
      rv |=(((long)data[6]&0xff) << 48);
      rv |=(((long)data[7]&0xff) << 56);
  return rv;
}

static boolean
bool_decode(int len, byte[] data)
{
  int i;
  boolean tf;

  tf = false;
  for(i = 0; i < len; i++) {
    if((data[i] & 0x7f) != 0) tf = true;
  }
  return tf;
}

static double
float64_decode(int len, byte[] buffer)
{
   long ld = fixed64_decode(len, buffer);
   return Double.longBitsToDouble(ld);
}

static float
float32_decode(int len, byte[] buffer)
{
   int i = fixed32_decode(len, buffer);
   return Float.intBitsToFloat(i);
}


/* return the zigzag-encoded 32-bit unsigned int from a 32-bit signed int */

static int
zigzag32(int n)
{
  int zz = (n << 1) ^ (n >> 31);
  return zz;
}

/* return the zigzag-encoded 64-bit unsigned int from a 64-bit signed int */

static long
zigzag64(long n)
{
    long zz = (n << 1) ^ (n >> 63);
    return zz;
}

static int
unzigzag32(int n)
{
  int zz = (n >>> 1) ^ -(n & 1);
  return zz;
}

static long
unzigzag64(long n)
{
  long zz = (n >>> 1) ^ -(n & 1);
  return zz;
}


/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 32-bit uint
   in base-128  */

static int
uint32_size(int value)
{
  if ((value & (0xffffffff <<  7)) == 0) return 1;
  if ((value & (0xffffffff << 14)) == 0) return 2;
  if ((value & (0xffffffff << 21)) == 0) return 3;
  if ((value & (0xffffffff << 28)) == 0) return 4;
  return 5;
}

/* Return the number of bytes required to store
   a variable-length signed integer that fits in 32-bit int
   in base-128  */
static int
int32_size(int v)
{
  return uint32_size(v);
}

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 64-bit uint
   in base-128  */
static int
uint64_size(long value)
{
   if ((value & (0xffffffffffffffffL <<  7)) == 0) return 1;
   if ((value & (0xffffffffffffffffL << 14)) == 0) return 2;
   if ((value & (0xffffffffffffffffL << 21)) == 0) return 3;
   if ((value & (0xffffffffffffffffL << 28)) == 0) return 4;
   if ((value & (0xffffffffffffffffL << 35)) == 0) return 5;
   if ((value & (0xffffffffffffffffL << 42)) == 0) return 6;
   if ((value & (0xffffffffffffffffL << 49)) == 0) return 7;
   if ((value & (0xffffffffffffffffL << 56)) == 0) return 8;
   if ((value & (0xffffffffffffffffL << 63)) == 0) return 9;
   return 10;
}

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 64-bit int
   in base-128 
*/
static int
int64_size(long v)
{
  return uint64_size(v);
}

/* Return the number of bytes required to store
   a variable-length signed integer that fits in 32-bit int,
   converted to unsigned via the zig-zag algorithm,
   then packed using base-128  */

static int
sint32_size(int v)
{
  return uint32_size(zigzag32(v));
}


/* Return the number of bytes required to store
   a variable-length signed integer that fits in 64-bit int,
   converted to unsigned via the zig-zag algorithm,
   then packed using base-128  */
static int
sint64_size(long v)
{
  return uint64_size(zigzag64(v));
}

//////////////////////////////////////////////////
// These functions need access to the io stream
static int
readwiretype(int wiretype, byte[] buffer, AbstractIO io)
    throws IOException
{
    int count;
    switch (wiretype) {
    case Ast_varint:
        for(int i=0;i<buffer.length;i++) {
	    if(!io.read(buffer, i, 1))  {
                count = -1; // eof
                break;
            }
	    if((0x80 & buffer[i]) == 0) {
                count = i+1;
                break;
            }
        }
	break;
    case Ast_32bit:
	count = 4;
        if(!io.read(buffer, 0,4)) count = -1;
	break;
    case Ast_64bit:
	count = 8;
        if(!io.read(buffer, 0,8)) count = -1;
	break;
    case Ast_counted: /* get the count */
        int len = readwiretype(Ast_varint,buffer,io);
	count = uint32_decode(len,buffer);
	break;
    default:
	throw new ASTException("Unexpected wiretype: "+wiretype);
    }
    return 0;
}

} /*class ASTRuntime*/


