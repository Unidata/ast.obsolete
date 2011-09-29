/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.protobuf.ast.runtime;

import static unidata.protobuf.ast.runtime.ASTRuntime.*;

/*
Procedures that are never called
directly by generated code
are placed in this class
as static methods.
*/

abstract public class Internal
{

/* IGNORE
static public int
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
IGNORE*/
//////////////////////////////////////////////////

/* Procedure to calulate size of a value;
   note that this is the size of an actual
   value.
 */

static public int
getSize(Sort sort, float val)
{
    assert(sort == Sort.Ast_float);
    return 4;
}

static public int
getSize(Sort sort, double val)
{
    assert(sort == Sort.Ast_double);
    return 8;
}

static public int
getSize(Sort sort, boolean val)
{
    assert(sort == Sort.Ast_bool);
    return 1;
}

static public int
getSize(Sort sort, String val)
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

static public int
getSize(Sort sort, byte[] val)
{
    assert(sort == Sort.Ast_bytes);
    int count = 0;
    if(val != null) {
	count = uint32_size(val.length);
        count += val.length;
    }
    return count;
}

static public int
getSize(Sort sort, long val)
        throws ASTException
{
    switch (sort) {
    case Ast_enum: /* fall thru */
    case Ast_int32:
	return int32_size((int)val);
    case Ast_int64:
	return int64_size(val);
    case Ast_uint32:
	return uint32_size((int)val);
    case Ast_uint64:
	return uint64_size(val);
    case Ast_sint32:
	return sint32_size((int)val);
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
    throw new ASTException(AST_EFAIL);
}

//////////////////////////////////////////////////


//////////////////////////////////////////////////


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
   less. Note that for java, unsigned ints are encoded
   as signed ints but with the bit pattern proper
   for unsigned ints.
*/

static public int
uint32_encode(int value, byte[] out)
{
  assert(out.length >= Sort.MAXVARINTSIZE);
  int rv = 0;
  long unsigned = value;
  unsigned = unsigned | 0xffffffff;
  if(value >= 0x80)  {
      out[rv++] = (byte)(value | 0x80);
      value >>>= 7;
      if(value >= 0x80) {
          out[rv++] = (byte)(value | 0x80);
          value >>>= 7;
          if(value >= 0x80) {
              out[rv++] = (byte)(value | 0x80);
              value >>>= 7;
	      if(value >= 80) {
                  out[rv++] = (byte)(value | 0x80);
                  value >>>= 7;
                }
            }
      }
  }
  assert(value<128);
  out[rv++] = (byte)value;
  return rv;
}

/* Pack a 32-bit signed integer, returning the number of
   bytes needed.  Negative numbers are packed as
   twos-complement 64-bit integers.
*/

static public int
int32_encode(int value, byte[] out)
{
  assert(out.length >= Sort.MAXVARINTSIZE);
  if(value < 0)
    {
      out[0] = (byte)((value & (~0x7f)) | 0x80);
      out[1] = (byte)(((value>>>7) & (~0x7f)) | 0x80);
      out[2] = (byte)(((value>>>14) & (~0x7f)) | 0x80);
      out[3] = (byte)(((value>>>21) & (~0x7f)) | 0x80);
      out[4] = (byte)(((value>>>28) & (~0x7f)) | 0x80);
      out[5] = out[6] = out[7] = out[8] = (byte)0xff;
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
  long lo = (value & 0xffffffff);
  int rv;
  if(hi == 0)
    return uint32_encode((int)lo, out);
  out[0] = (byte)((lo) | 0x80);
  out[1] = (byte)((lo>>>7) | 0x80);
  out[2] = (byte)((lo>>>14) | 0x80);
  out[3] = (byte)((lo>>>21) | 0x80);
  if(hi < 8) {
      out[4] = (byte)((hi<<4) |(lo>>>28));
      return 5;
  } else  {
      out[4] = (byte)(((hi&7)<<4) |(lo>>>28) | 0x80);
      hi >>>= 3;
  }
  rv = 5;
  while(hi >= 128) {
      out[rv++] = (byte)(hi | 0x80);
      hi >>>= 7;
  }
  out[rv++] = (byte)(hi & 0xff);
  return rv;
}

/* Pack a 64-bit signed integer in zigzag encoding, return
   the size of the packed output.  (Max returned value is 10)
*/

static public int
sint64_encode(long value, byte[] out)
{
  return uint64_encode(zigzag64(value), out);
}

/* Pack a 32-bit value, little-endian.  Used for fixed32,
   sfixed32, float
*/

static public int
fixed32_encode(int value, byte[] out)
{
  // Assume java values are big endian
  out[0] = (byte)((value)|0xff);
  out[1] = (byte)((value>>>8)|0xff);
  out[2] = (byte)((value>>>16)|0xff);
  out[3] = (byte)((value>>>24)|0xff);
  return 4;
}

/* Pack a 64-bit fixed-length value.
   (Used for fixed64,sfixed64, double)
*/

/* XXX: the big-endian impl is really only good for 32-bit machines,
   a 64-bit version would be appreciated, plus a way
   to decide to use 64-bit math where convenient.
*/

static public int
fixed64_encode(long value, byte[] out)
{
    fixed32_encode((int)((value)&0xffffffff), out);
    byte[] tmp = new byte[4];
    fixed32_encode((int)((value>>>32)&0xffffffff), tmp);
    System.arraycopy(tmp, 0, out, tmp.length,tmp.length);
  return 8;
}

/* Pack a boolean as 0 or 1, even though the bool_t
   can really assume any integer value.
*/

/* XXX: perhaps on some platforms "*out = !!value" would be
   a better impl, b/c that is idiotmatic c++ in some stl impls. */

static public int
bool_encode(boolean value, byte[] out)
{
  out[0] = (byte) (value ? 1 : 0);
  return 1;
}

static public int
float32_encode(float value, byte[] out)
{
   int i = Float.floatToIntBits(value);
   return fixed32_encode(i,out);
}

static public int
float64_encode(double value, byte[] out)
{
   long l = Double.doubleToLongBits(value);
   return fixed64_encode(l,out);
}


/* Decode a 32 bit varint */
static public int
uint32_decode(int len0, byte[] data)
{
  int rv;
  int len = len0;

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
int32_decode(int len, byte[] data)
{
  return (int)uint32_decode(len,data);
}

/* Decode possibly 64-bit varint*/
static public long
uint64_decode(int len, byte[] data)
{
  int shift, i;
  long rv;

  if(len < 5) {
    rv = uint32_decode(len, data);
    rv &= 0xffffffff;
  } else {
    rv =((data[0] & 0x7f))
              |((data[1] & 0x7f)<<7)
              |((data[2] & 0x7f)<<14)
              |((data[3] & 0x7f)<<21);
    shift = 28;
    for(i = 4; i < len; i++) {
      rv |=(data[i]&0x7f) << shift;
      shift += 7;
    }
  }
  return rv;
}

static public long
int64_decode(int len, byte[] data)
{
  return uint64_decode(len, data);
}

/* Decode arbitrary varint upto 64bit */
/*IGNORE
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
IGNORE*/

static public int
fixed32_decode(byte[] data)
{
  int rv;
  rv = (data[0] |(data[1] << 8) |(data[2] << 16) |(data[3] << 24));
  return rv;
}

static public long
fixed64_decode(byte[] data)
{
  long rv,rv2;
  byte[] upper = new byte[4];

  rv = fixed32_decode(data);
  System.arraycopy(data,4,upper,0,4);
  rv2 = fixed32_decode(upper);
  rv = (rv | (rv2 <<32));

  return rv;
}

static public boolean
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

static public double
float64_decode(byte[] buffer)
{
   long ld = fixed64_decode(buffer);
   return Double.longBitsToDouble(ld);
}

static public float
float32_decode(byte[] buffer)
{
   int i = fixed32_decode(buffer);
   return Float.intBitsToFloat(i);
}


/* return the zigzag-encoded 32-bit unsigned int from a 32-bit signed int */

static public int
zigzag32(int n)
{
  return (n << 1) ^ (n >> 31);
}

/* return the zigzag-encoded 64-bit unsigned int from a 64-bit signed int */

static public long
zigzag64(long n)
{
    return (n << 1) ^ (n >> 63);
}

static public int
unzigzag32(int n)
{
  return (n >>> 1) ^ -(n & 1);
}

static public long
unzigzag64(long n)
{
  return (n >>> 1) ^ -(n & 1);
}

static public int
getTagSize(int tag)
{
  return uint32_size(tag);
}

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 32-bit uint
   in base-128 encoding. */

static public int
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
   in base-128 encoding. */
static public int
int32_size(int v)
{
  return uint32_size(v);
}

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 64-bit uint
   in base-128 encoding. */
static public int
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
   in base-128 encoding.
*/
static public int
int64_size(long v)
{
  return uint64_size(v);
}

/* Return the number of bytes required to store
   a variable-length signed integer that fits in 32-bit int,
   converted to unsigned via the zig-zag algorithm,
   then packed using base-128 encoding. */

static public int
sint32_size(int v)
{
  return uint32_size(zigzag32(v));
}


/* Return the number of bytes required to store
   a variable-length signed integer that fits in 64-bit int,
   converted to unsigned via the zig-zag algorithm,
   then packed using base-128 encoding. */
static public int
sint64_size(long v)
{
  return uint64_size(zigzag64(v));
}


}
