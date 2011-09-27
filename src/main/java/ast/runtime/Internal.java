/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.protobuf.ast.runtime;

import static unidata.protobuf.ast.runtime.Runtime.*;

/*
Procedures that are never called
directly by generated code
are placed in this class
as static methods.
*/

abstract public class Internal
{

static public long
jtypesize(Sort sort)
{
    switch(sort) {
    case Ast_double: return 8;
    case Ast_float: return 4;

    case Ast_enum:
    case Ast_bool:
    case Ast_int32:
    case Ast_sint32:
    case Ast_fixed32:
    case Ast_sfixed32:
    case Ast_uint32: return 4;

    case Ast_int64:
    case Ast_sint64:
    case Ast_fixed64:
    case Ast_sfixed64:
    case Ast_uint64: return 8;

    case Ast_string: return 8;
    case Ast_bytes: return 8;

    case Ast_message:return 8;

    default: assert(0);
    }
    return 0;
}

//////////////////////////////////////////////////

/* Procedure to calulate size of a value */
public long
get_size(Sort sort, float val)
{
    assert(sort == Ast_float);
    return 4;
}

public long
get_size(Sort sort, double val)
{
    assert(sort == Ast_double);
    return 8;
}

public long
get_size(Sort sort, boolean val)
{
    assert(sort == Ast_boolean);
    return 1;
}

public long
get_size(Sort sort, String val)
{
    assert(sort == Ast_string)
    /* string count is size for length counter + strlen(string) */
    long count = 0;
    if(val != null) {
	int slen = val.length();
        count = uint32_size(slen);
	count += slen;
    }
    return count
}

public long
get_size(Sort sort, byte[] val)
{
    assert(sort == Ast_bytes)
    long count = 0;
    if(val != null) {
	count = uint32_size(val.length);
        count += val.length;
    }
    return count;
}

public long
get_size(Sort sort, long val)
{
    switch (sort) {
    case Ast_enum: /* fall thru */
    case Ast_int32:
	return int32_size(val);
    case Ast_int64:
	return int64_size(val);
    case Ast_uint32:
	return uint32_size(val);
    case Ast_uint64:
	return uint64_size(val);
    case Ast_sint32:
	return sint32_size(val);
    case Ast_sint64:
	return sint64_size(val);
    case Ast_fixed32:
	return 4;
    case Ast_sfixed32:
	return 4;
    case Ast_fixed64:
	return 8;
    case Ast_sfixed64:
	return 8;
    default:
	break;
    }
    throw new Exception(AST_EFAIL);
}

//////////////////////////////////////////////////


//////////////////////////////////////////////////

/* Based on the wiretype, extract the proper number of bytes
   for an integer base value; return the length
   and place the bytes into the buffer.
   For ast counted, do this for the count,
   not the content.
*/

static public int
readcount(int wiretype, byte[] buffer)
    throws Exception
{
    long len = 0;
    int count;
    switch (wiretype) {
    case Ast_varint:
        count = readvarint(buffer);
	break;
    case Ast_32bit:
	count = 4;
        if(!io.read(4,buffer)) count = -1;
	break;
    case Ast_64bit:
	count = 8;
        if(!io.read(8,buffer)) count = -1;
	break;
    case Ast_counted: /* get the count */
        len = readvarint(buffer);
	count = uint64_decode(len,buffer);	
	break;
    default:
	throw new Exception(AST_EFAIL);
    }
    return count;
}

static public long
readvarint(byte[] buffer)
    throws IOException
{
    long i=0;
    boolean more = true;
    while(i<VARINTMAX64 && more) {
	if(!io.read(i,1,buffer)) return -1; // eof
	if((0x80 & buffer[i]) == 0) more = false;
	buffer[i] = 0x7f & buffer[i];
    }
    return i;
}

/* Return the number of bytes required to store the
   tag for the field(which includes 3 bits for
   the wire-type, and a single bit that denotes the end-of-tag.
*/
static public int
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
   less.
*/

static public int
uint32_encode(long value, byte[] out)
{
  assert(out.length >= VARINTMAX);
  int rv = 0;
  if(value >= 0x80)
    {
      out[rv++] = value | 0x80;
      value >>>= 7;
      if(value >= 0x80)
        {
          out[rv++] = value | 0x80;
          value >>>= 7;
          if(value >= 0x80)
            {
              out[rv++] = value | 0x80;
              value >>>= 7;
              if(value >= 0x80)
                {
                  out[rv++] = value | 0x80;
                  value >>>= 7;
                }
            }
        }
    }
  assert(value<128);
  out[rv++] = value;
  return rv;
}

/* Pack a 32-bit signed integer, returning the number of
   bytes needed.  Negative numbers are packed as
   twos-complement 64-bit integers.
*/

static public int
int32_encode(int value, byte[] out)
{
  assert(out.length >= VARINTMAX);
  if(value < 0)
    {
      out[0] = value | 0x80;
      out[1] =(value>>>7) | 0x80;
      out[2] =(value>>>14) | 0x80;
      out[3] =(value>>>21) | 0x80;
      out[4] =(value>>>28) | 0x80;
      out[5] = out[6] = out[7] = out[8] = 0xff;
      out[9] = 0x01;
      return 10;
    }
  else
    return uint32_encode(value, out);
}

/*
Pack a 32-bit integer in zigwag encoding.
*/

static public int
sint32_encode(int value, byte[] out)
{
  return uint32_encode(zigzag32(value), out);
}

/* Pack a 64-bit unsigned integer that fits in a 64-bit uint,
   using base-128 encoding. */

static public int
int64_encode(long value, byte[] out)
{
    return uint64_encode(value,out);
}

static public int
uint64_encode(long value, byte[] out)
{
  long hi = value>>>32;
  uint32_t lo = value;
  unsigned rv;
  if(hi == 0)
    return uint32_encode((uint32_t)lo, out);
  out[0] =(lo) | 0x80;
  out[1] =(lo>>>7) | 0x80;
  out[2] =(lo>>>14) | 0x80;
  out[3] =(lo>>>21) | 0x80;
  if(hi < 8)
    {
      out[4] =(hi<<4) |(lo>>>28);
      return 5;
    }
  else
    {
      out[4] =((hi&7)<<4) |(lo>>>28) | 0x80;
      hi >>>= 3;
    }
  rv = 5;
  while(hi >= 128)
    {
      out[rv++] = hi | 0x80;
      hi >>>= 7;
    }
  out[rv++] = hi;
  return rv;
}

/* Pack a 64-bit signed integer in zigzan encoding, return
   the size of the packed output.  (Max returned value is 10)
*/

static public long
sint64_encode(long value, byte[] out)
{
  return uint64_encode(zigzag64(value), out);
}

/* Pack a 32-bit value, little-endian.  Used for fixed32,
   sfixed32, float
*/

static public long
fixed32_encode(long value, byte[] out)
{
  // Assume java values are big endian
  out[0] = (value)|0xff;
  out[1] = (value>>>8)|0xff;
  out[2] = (value>>>16)|0xff;
  out[3] = (value>>>24)|0xff;
  return 4;
}

/* Pack a 64-bit fixed-length value.
   (Used for fixed64,sfixed64, double)
*/

/* XXX: the big-endian impl is really only good for 32-bit machines,
   a 64-bit version would be appreciated, plus a way
   to decide to use 64-bit math where convenient.
*/

static public long
fixed64_encode(long value, byte[] out)
{
  fixed32_encode(value, out);
  fixed32_encode(value>>>32, out+4);
  return 8;
}

/* Pack a boolean as 0 or 1, even though the bool_t
   can really assume any integer value.
*/

/* XXX: perhaps on some platforms "*out = !!value" would be
   a better impl, b/c that is idiotmatic c++ in some stl impls. */

static public long
boolean_encode(boolean value, byte[] out)
{
  out[0] = value ? 1 : 0;
  return 1;
}

/* Decode a 32 bit varint */
static public long
uint32_decode(long len0, byte[] data)
{
  long rv;
  long len = len0;

  if(len > 5) len = 5;
  rv = data[0] & 0x7f;
  if(len > 1) {
      rv |=((data[1] & 0x7f) << 7);
      if(len > 2) {
          rv |=((data[2] & 0x7f) << 14);
          if(len > 3) {
              rv |=((data[3] & 0x7f) << 21);
              if(len > 4)
                rv |=(data[4] << 28);
            }
        }
  }
  return rv;
}

static public int
int32_decode(long len, byte[] data)
{
  return (int)uint32_decode(len,data);
}

/* Decode possibly 64-bit varint*/
static public long
uint64_decode(long len, byte[] data)
{
  unsigned shift, i;
  long rv;

  if(len < 5) {
    rv = uint32_decode(len, data);
  } else {
    rv =((data[0] & 0x7f))
              |((data[1] & 0x7f)<<7)
              |((data[2] & 0x7f)<<14)
              |((data[3] & 0x7f)<<21);
    shift = 28;
    for(i = 4; i < len; i++) {
      rv |=(data[i]&0x7f) << shift);
      shift += 7;
    }
  }
  return rv;
}

static public long
int64_decode(long len, byte[] data)
{
  return uint64_decode(len, data);
}

/* Decode arbitrary varint upto 64bit */
static public long
varint_decode(long buflen, byte[] buffer, long[] countp)
{
  long shift, i;
  long rv = 0;
  long count = 0;

  for(count=0,shift=0,i=0;i<buflen;i++,shift+=7) {
    byte byt = buffer[i];
    count++;
    rv |= ((byt & 0x7f) << shift);
    if((byt & 0x80)==0) break;
  }
  countp[0] = count;
  return rv;
}

static public long
fixed32_decode(byte[] data)
{
  long rv;
  rv = (data[0] |(data[1] << 8) |(data[2] << 16) |(data[3] << 24));
  return rv;
}

static public long
fixed64_decode(byte[] data)
{
  uint64_t rv;
  byte[] upper = new byte[4];

  rv = fixed32_decode(data);
  System.arraycopy(data,4,upper,0,4);
  rv2 = fixed32_decode(upper);
  rv = (rv | (rv2 <<32));

  return rv;
}

static public boolean
boolean_decode(long len, byte[] data)
{
  int i;
  boolean tf;

  tf = 0;
  for(i = 0; i < len; i++) {
    if(data[i] & 0x7f) tf = 1;
  }
  return tf;
}

/* return the zigzag-encoded 32-bit unsigned int from a 32-bit signed int */

static public long
zigzag32(int v)
{
  if(v < 0)
    return((long)(-v)) * 2 - 1;
  else
    return ((long)v) * 2;
}

/* return the zigzag-encoded 64-bit unsigned int from a 64-bit signed int */

static public long
zigzag64(long v)
{
  if(v < 0)
    return((long)(-v)) * 2 - 1;
  else
    return v * 2;
}

static public int
unzigzag32(long v)
{
  if( v & 1 == 1)
    return -(v>>>1) - 1;
  else
    return v>>>1;
}

static public long
unzigzag64(long v)
{
  if(v & 1 == 1)
    return -(v>>>1) - 1;
  else
    return v>>>1;
}

static public long
get_tag_size(long number)
{
  if(number < (long)(1<<4))
    return 1;
  else if(number < (long)(1<<11))
    return 2;
  else if(number < (long)(1<<18))
    return 3;
  else if(number < (long)(1<<25))
    return 4;
  else
    return 5;
}

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 32-bit uint
   in base-128 encoding. */

static public long
uint32_size(long v)
{
  if(v < (long)(1<<7))
    return 1;
  else if(v < (long)(1<<14))
    return 2;
  else if(v < (long)(1<<21))
    return 3;
  else if(v < (long)(1<<28))
    return 4;
  else
    return 5;
}

/* Return the number of bytes required to store
   a variable-length signed integer that fits in 32-bit int
   in base-128 encoding. */
static public long
int32_size(int32_t v)
{
  if(v < (long)0)
    return 10;
  else if(v < (long)(1<<7))
    return 1;
  else if(v < (long)(1<<14))
    return 2;
  else if(v < (long)(1<<21))
    return 3;
  else if(v < (long)(1<<28))
    return 4;
  else
    return 5;
}

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 64-bit uint
   in base-128 encoding. */
static public long
uint64_size(long v)
{
  long upper_v =(v>>>32);
  if(upper_v == 0)
    return uint32_size((int)v);
  else if(upper_v < (long)(1<<3))
    return 5;
  else if(upper_v < (long)(1<<10))
    return 6;
  else if(upper_v < (long)(1<<17))
    return 7;
  else if(upper_v < (long)(1<<24))
    return 8;
  else if(upper_v < (long)(1<<31))
    return 9;
  else
    return 10;
}

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 64-bit int
   in base-128 encoding.
*/
static public long
int64_size(long v)
{
  long upper_v = (v>>>32);
  if(upper_v == 0)
    return int32_size((int)v);
  else if(upper_v < (long)(1<<3))
    return 5;
  else if(upper_v < (long)(1<<10))
    return 6;
  else if(upper_v < (long)(1<<17))
    return 7;
  else if(upper_v < (long)(1<<24))
    return 8;
  else if(upper_v < (long)(1<<31))
    return 9;
  else
    return 10;
}

/* Return the number of bytes required to store
   a variable-length signed integer that fits in 32-bit int,
   converted to unsigned via the zig-zag algorithm,
   then packed using base-128 encoding. */

static public long
sint32_size(int v)
{
  return uint32_size(zigzag32(v));
}


/* Return the number of bytes required to store
   a variable-length signed integer that fits in 64-bit int,
   converted to unsigned via the zig-zag algorithm,
   then packed using base-128 encoding. */
static public long
sint64_size(long v)
{
  return uint64_size(zigzag64(v));
}


}
