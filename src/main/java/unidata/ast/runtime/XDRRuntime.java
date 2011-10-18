/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
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

import static unidata.ast.runtime.ASTRuntime.Wiretype.Ast_32bit;
import static unidata.ast.runtime.ASTRuntime.Wiretype.Ast_counted;
import static unidata.ast.runtime.ASTRuntime.Wiretype.Ast_varint;

public class XDRRuntime extends ASTRuntime
{

//////////////////////////////////////////////////

public XdrCoding()
{
    super();
}

XdrCoding(AbstractIO io)
{
    super(io);
}


static protected class Encoding
{

/* Return the number of bytes required to store the
   tag for the field, which includes 3 bits for
   the wire-type, and a single bit that denotes the end-of-tag.
*/
int
encode_tag(int wiretype, int fieldno, byte[] buffer)
{
    int key = (wiretype | (fieldno << 3));
    /* convert key to varint */
    int count = uint32_encode(key,buffer);
    return count;
}


/* Pack an unsigned 32-bit integer in base-128 encoding, and
   return the number of bytes needed: this will be 5 or
   less. Note that for java, unsigned ints are encoded
   as signed ints but with the bit pattern proper
   for unsigned ints.
*/

int
uint32_encode(int value, byte[] out)
{
    return fixed32_encode(value,out);
}

/* Pack a 32-bit signed integer, returning the number of
   bytes needed.  Negative numbers are packed as
   twos-complement 64-bit integers.
*/

int
int32_encode(int value, byte[] out)
{
  if(value >= 0)
    return uint32_encode(value,out);
  else
    return uint64_encode(value,out);
}

/*
Pack a 32-bit integer in zigwag encoding.
*/

int
sint32_encode(int value, byte[] out)
{
    return fixed32_encode(value,out);
}

/* Pack a 64-bit unsigned integer that fits in a 64-bit uint,
   using base-128 encoding. */

int
int64_encode(long value, byte[] out)
{
    return uint64_encode(value,out);
}

int
uint64_encode(long value, byte[] out)
{
    return fixed64_encode(value, out);
}

/* Pack a 64-bit signed integer in zigzag encoding, return
   the size of the packed output.  (Max returned value is 10)
*/

int
sint64_encode(long value, byte[] out)
{
  return uint64_encode(value, out);
}

/* Pack a 32-bit value; Used for fixed32,
   sfixed32, float
*/

int
fixed32_encode(int value, byte[] out)
{
  // xdr uses big endian
  out[3] = (byte)((value)&0xff);
  out[2] = (byte)((value>>>8)&0xff);
  out[1] = (byte)((value>>>16)&0xff);
  out[0] = (byte)((value>>>24)&0xff);
  return 4;
}

/* Pack a 64-bit fixed-length value.
   (Used for fixed64,sfixed64, double)
*/

/* Protobuf writes little endian */

int
fixed64_encode(long value, byte[] out)
{
  out[7] = (byte)((value)&0xff);
  out[6] = (byte)((value>>>8)&0xff);
  out[5] = (byte)((value>>>16)&0xff);
  out[4] = (byte)((value>>>24)&0xff);
  out[3] = (byte)((value>>>32)&0xff);
  out[2] = (byte)((value>>>40)&0xff);
  out[1] = (byte)((value>>>48)&0xff);
  out[0] = (byte)((value>>>56)&0xff);
  return 8;
}

/* Pack a boolean as 0 or 1, even though the bool_t
   can really assume any integer value.
*/

/* XXX: perhaps on some platforms "*out = !!value" would be
   a better impl, b/c that is idiotmatic c++ in some stl impls. */

int
bool_encode(boolean value, byte[] out)
{
    return fixed32_encode((value ? 1 : 0),out);
}

int
float32_encode(float value, byte[] out)
{
   int i = Float.floatToIntBits(value);
   return fixed32_encode(i,out);
}

int
float64_encode(double value, byte[] out)
{
   long l = Double.doubleToLongBits(value);
   return fixed64_encode(l,out);
}


/* Decode a 32 bit varint */
int
uint32_decode(int len, byte[] data)
{
    return fixed32_decode(len,data);
}

int
int32_decode(int len, byte[] data)
{
    return uint32_decode(len, data);
}

/* Decode possibly 64-bit varint*/
long
uint64_decode(int len, byte[] data)
{
    return fixed64_decode(len,data);
}

long
int64_decode(int len, byte[] data)
{
  return uint64_decode(len, data);
}

int
sint32_decode(int value, byte[] out)
{
    return fixed32_decode(value,out);
}

long
sint64_decode(int value, byte[] out)
{
    return fixed64_decode(value,out);
}

int
fixed32_decode(int len, byte[] data)
{
  int rv = (
        (((int)data[3])&0xff)
      | (((int)data[2]&0xff) << 8)
      | (((int)data[1]&0xff) << 16)
      | (((int)data[0]&0xff) << 24)
      );
  return rv;
}

long
fixed64_decode(int len, byte[] data)
{
  long rv = 0;
      rv |= (((long)data[7])&0xff);
      rv |= (((long)data[6]&0xff) << 8);
      rv |= (((long)data[5]&0xff) << 16);
      rv |= (((long)data[4]&0xff) << 24);
      rv |= (((long)data[3]&0xff) << 32);
      rv |= (((long)data[2]&0xff) << 40);
      rv |= (((long)data[1]&0xff) << 48);
      rv |= (((long)data[0]&0xff) << 56);
  return rv;
}

boolean
bool_decode(int len, byte[] data)
{
    int i = fixed32_decode(len,data);
    return (i==0? false : true);
}

double
float64_decode(int len, byte[] buffer)
{
   long ld = fixed64_decode(len, buffer);
   return Double.longBitsToDouble(ld);
}

float
float32_decode(int len, byte[] buffer)
{
   int i = fixed32_decode(len, buffer);
   return Float.intBitsToFloat(i);
}

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 32-bit uint
   in xdr encoding.
*/

int
uint32_size(int value)
{
    return 4;
}

/* Return the number of bytes required to store
   a variable-length signed integer that fits in 32-bit int
   in base-128 encoding. */
int
int32_size(int v)
{
  return uint32_size(v);
}

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 64-bit uint
   in base-128 encoding. */
int
uint64_size(long value)
{
    return 8;
}

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 64-bit int
   in base-128 encoding.
*/
int
int64_size(long v)
{
  return uint64_size(v);
}

/* Return the number of bytes required to store
   a variable-length signed integer that fits in 32-bit int,
   converted to unsigned via the zig-zag algorithm,
   then packed using base-128 encoding. */

int
sint32_size(int v)
{
  return uint32_size(v);
}


/* Return the number of bytes required to store
   a variable-length signed integer that fits in 64-bit int,
   converted to unsigned via the zig-zag algorithm,
   then packed using base-128 encoding. */
int
sint64_size(long v)
{
  return uint64_size(v);
}

//////////////////////////////////////////////////
// These functions need access to the io stream
int
readwiretype(int wiretype, byte[] buffer, AbstractIO io)
    throws IOException
{
    int count = 0;
    switch (wiretype) {
    case Ast_varint:
    case Ast_32bit:
	count = 4;
        if(!io.read(buffer, 0,4)) count = -1;
	break;
    case ASTRuntime.Wiretype.Ast_64bit:
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
    return count;
}
} // class Encoding

} // class XDRRuntime
