/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.ast.runtime;

import java.io.IOException;
import java.nio.charset.Charset;
import java.lang.reflect.Array;


/**
 * Encoding provides the encoding/decoding
 * methods needed by
 * generated code. Its subtypes will implement
 * specific encodings such as Protobuf or XDR.
 */
abstract public class Encoding
{

/**
 * Simulate all enums as integer within static classes.
 * Deliberately do not use enumeration
 * for (possibly misplaced) efficiency.
 */


/* Define primitive types enum */
static public class Sort
{
    static public final int Ast_double = 0;
    static public final int Ast_float = 1;
    static public final int Ast_int32 = 2;
    static public final int Ast_uint32 = 3;
    static public final int Ast_int64 = 4;
    static public final int Ast_uint64 = 5;
    static public final int Ast_sint32 = 6;
    static public final int Ast_sint64 = 7;
    static public final int Ast_fixed32 = 8;
    static public final int Ast_fixed64 = 9;
    static public final int Ast_sfixed32 = 10;
    static public final int Ast_sfixed64 = 11;
    static public final int Ast_string = 12;
    static public final int Ast_bytes = 13;
    static public final int Ast_bool = 14;
    static public final int Ast_enum = 15;
    static public final int Ast_message = 16;
    static public final int Ast_packed = 17;

    static public final int MAXTYPESIZE = 16;
    static public final int MAXVARINTSIZE = 10;

} /*Sort*/

static public class Wiretype
{
    static public final int Ast_varint = (0); //int64,uint32,uint64,sint32,sint64,bool,enum
    static public final int Ast_64bit = (1); //fixed64,sfixed64,double
    static public final int Ast_counted = (2); //string,bytes,messages,packed
    static public final int Ast_startgroup = (3); //Start group (deprecated)
    static public final int Ast_endgroup = (4); // end group (deprecated)
    static public final int Ast_32bit = (5); //fixed32,sfixed32,float
}

/* Define the field modes */
static public class Fieldmode {
    static public final int Ast_required = 0;
    static public final int Ast_optional = 1;
    static public final int Ast_repeated = 2;
}

static public final Charset utf8 = Charset.forName("utf-8");

//////////////////////////////////////////////////
// Instance fields

AbstractIO io = null;

//////////////////////////////////////////////////
// Constructor(s)

public Encoding()
    throws ASTException
{
}

//////////////////////////////////////////////////
// Get/Set

public void
setIO(AbstractIO io)
{
    this.io = io;
}

//////////////////////////////////////////////////
// Wrappers for io.read and io.write  etc

boolean
read(byte[] buf, int offset, int len) throws IOException
{
    return io.read(buf, offset,len);
}

void
write(int len, byte[] buf) throws IOException
{
    io.write(len,buf);
}

void
mark(int avail)
    throws IOException
{
    io.mark(avail);    
}

void
unmark()
        throws IOException
{
    io.unmark();
}

//////////////////////////////////////////////////
/* Given an unknown field, skip past it */

abstract void skip_field(int wiretype, int fieldno) throws IOException;

//////////////////////////////////////////////////
/* Procedure to calulate size of a value;
   note that this is the size of an actual
   value.
 */

abstract int getSize(int sort, float val);
abstract int getSize(int sort, double val);
abstract int getSize(int sort, boolean val);
abstract int getSize(int sort, String val);
abstract int getSize(int sort, byte[] val);
abstract int getSize(int sort, long val) throws ASTException;
abstract int getSizePacked(int sort, int[] val);
abstract int getSizePacked(int sort, long[] val);
abstract int getSizePacked(int sort, float[] val);
abstract int getSizePacked(int sort, double[] val);
abstract int getSizePacked(int sort, boolean[] val);

/* Procedure to calculate the size a message
   including its prefix size, when given
   the unprefixed message size
 */
abstract int getMessageSize(int size);

/* Procedure to calulate size of a tag */
abstract int getTagSize(int sort, int fieldno);

//////////////////////////////////////////////////
/* Procedures to read/write tags */

abstract void write_tag(int sort, int fieldno) throws IOException ;

/* Procedure to extract tags; args simulate call by ref */
abstract boolean read_tag(int[] wiretype, int[] fieldno) throws IOException;

/* Procedures to write and sizes */
abstract void write_size(int size) throws IOException;
abstract int read_size() throws IOException;

//////////////////////////////////////////////////

abstract double read_primitive_double(int sort) throws IOException;
abstract float read_primitive_float(int sort) throws IOException;
abstract boolean read_primitive_boolean(int sort) throws IOException;
abstract String read_primitive_string(int sort) throws IOException;
abstract byte[] read_primitive_bytes(int sort) throws IOException;
abstract int read_primitive_int(int sort) throws IOException;
abstract long read_primitive_long(int sort) throws IOException;
abstract double[] read_primitive_packed_double(int sort) throws IOException;
abstract float[] read_primitive_packed_float(int sort) throws IOException;
abstract boolean[] read_primitive_packed_bool(int sort) throws IOException;

// Read a sequence of values that are expected to be 32 bit integers
// i.e. sort= Ast_int32,Ast_sint32,Ast_fixed32,Ast_sfixed32

abstract int[] read_primitive_packed_int(int sort) throws IOException;

// Read a sequence of values that are expected to be 64 bit integers
// i.e. sort= Ast_int64,Ast_sint64,Ast_fixed64,Ast_sfixed64

abstract long[] read_primitive_packed_long(int sort) throws IOException;

// Write a sequence of values
abstract void write_primitive(int sort, double val) throws IOException;
abstract void write_primitive(int sort, float val) throws IOException;
abstract void write_primitive(int sort, boolean val) throws IOException;
abstract void write_primitive(int sort, String val) throws IOException;
abstract void write_primitive(int sort, byte[] bval) throws IOException;
abstract void write_primitive(int sort, long val) throws IOException;

abstract void write_primitive_packed(int sort, double[] val) throws IOException;
abstract void write_primitive_packed(int sort, float[] val) throws IOException;
abstract void write_primitive_packed(int sort, boolean[] val) throws IOException;
abstract void write_primitive_packed(int sort, int[] val) throws IOException;
abstract void write_primitive_packed(int sort, long[] val) throws IOException;

/* Read into Repeated field */
abstract double[] repeat_append(int sort, double newval, double[] list);
abstract float[] repeat_append(int sort, float newval, float[] list);
abstract boolean[] repeat_append(int sort, boolean newval, boolean[] list);
abstract int[] repeat_append(int sort, int newval, int[] list);
abstract long[] repeat_append(int sort, long newval, long[] list);
abstract String[] repeat_append(int sort, String newval, String[] list);
abstract byte[][] repeat_append(int sort, byte[] newval, byte[][] list);

// Special handling for messages and enums
abstract Object repeat_extend(Object list, java.lang.Class klass);

} /*class Encoding*/


