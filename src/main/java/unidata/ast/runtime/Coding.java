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

/**
 * Define interface for encoding
 * and decoding operations
 */

abstract public  class Coding
{

    public Coding() {};

/* Return the number of bytes required to store the
   tag for the field, which includes 3 bits for
   the wire-type, and a single bit that denotes the end-of-tag.
*/
abstract public int encode_tag(int wiretype, int fieldno, byte[] buffer);

/* Pack an unsigned 32-bit integer in base-128 encoding, and
   return the number of bytes needed: this will be 5 or
   less. Note that for java, unsigned ints are encoded
   as signed ints but with the bit pattern proper
   for unsigned ints.
*/

abstract public int uint32_encode(int value, byte[] out);

/* Pack a 32-bit signed integer, returning the number of
   bytes needed.  Negative numbers are packed as
   twos-complement 64-bit integers.
*/
abstract public int int32_encode(int value, byte[] out);

/*
Pack a 32-bit integer in zigwag encoding.
*/

abstract public int sint32_encode(int value, byte[] out);

/* Pack a 64-bit unsigned integer that fits in a 64-bit uint,
   using base-128 encoding. */
abstract public int int64_encode(long value, byte[] out);

abstract public int uint64_encode(long value, byte[] out);

/* Pack a 64-bit signed integer in zigzag encoding, return
   the size of the packed output.  (Max returned value is 10)
*/
abstract public int sint64_encode(long value, byte[] out);

/* Pack a 32-bit value, little-endian.  Used for fixed32,
   sfixed32, float
*/
abstract public int fixed32_encode(int value, byte[] out);

/* Pack a 64-bit fixed-length value.
   (Used for fixed64,sfixed64, double)
*/

abstract public int fixed64_encode(long value, byte[] out);

/* Pack a boolean as 0 or 1, even though the bool_t
   can really assume any integer value.
*/
abstract public int bool_encode(boolean value, byte[] out);

abstract public int float32_encode(float value, byte[] out);

abstract public int float64_encode(double value, byte[] out);

/* Decode a 32 bit varint */
abstract public int uint32_decode(int len, byte[] data);

abstract public int int32_decode(int len, byte[] data);

/* Decode possibly 64-bit varint*/
abstract public long uint64_decode(int len, byte[] data);

abstract public long int64_decode(int len, byte[] data);

abstract public int sint32_decode(int len, byte[] data);

abstract public long sint64_decode(int len, byte[] data);

abstract public int fixed32_decode(int len, byte[] data);

abstract public long fixed64_decode(int len, byte[] data);

abstract public boolean bool_decode(int len, byte[] data);

abstract public double float64_decode(int len, byte[] buffer);

abstract public float float32_decode(int len, byte[] buffer);

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 32-bit uint
   in base-128 encoding. */

abstract public int uint32_size(int value);

/* Return the number of bytes required to store
   a variable-length signed integer that fits in 32-bit int
   in base-128 encoding. */
abstract public int int32_size(int v);

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 64-bit uint
   in base-128 encoding. */
abstract public int uint64_size(long value);

/* Return the number of bytes required to store
   a variable-length unsigned integer that fits in 64-bit int
   in base-128 encoding.
*/
abstract public int int64_size(long v);

/* Return the number of bytes required to store
   a variable-length signed integer that fits in 32-bit int,
   converted to unsigned via the zig-zag algorithm,
   then packed using base-128 encoding. */

abstract public int sint32_size(int v);

/* Return the number of bytes required to store
   a variable-length signed integer that fits in 64-bit int,
   converted to unsigned via the zig-zag algorithm,
   then packed using base-128 encoding. */
abstract public int sint64_size(long v);


//////////////////////////////////////////////////
// These functions need access to the io stream
abstract int readwiretype(int wiretype, byte[] buffer, AbstractIO io) throws IOException;

}//Coder

