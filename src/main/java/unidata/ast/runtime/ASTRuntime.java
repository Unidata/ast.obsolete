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
 * ASTRuntime provides access to the procedures used by
 * generated code.
 */

public class ASTRuntime
{

//////////////////////////////////////////////////
// Common constants used in the generated code

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
// Static fields and methods

static final String DFALTPACKAGE = "unidata.ast.runtime";


//////////////////////////////////////////////////
// Define enum of known encoding systems; each
// enum also defines the class implementing it.

public enum Encoder {
    Protobuf("ProtobufEncoding"),
    XDR("XDREncoding");
    private final String classname;
    Encoder(String classname) {this.classname = classname;}
    public String getClassName()   { return classname; }
    public static Encoder getEncoder(String name)
    {
	if(XDR.name().equalsIgnoreCase(name)) return XDR;
	else if(Protobuf.name().equalsIgnoreCase(name)) return Protobuf;
	return null;
    }
}

//////////////////////////////////////////////////
// Instance fields

AbstractIO io = null;

Encoding encoder = null;

//////////////////////////////////////////////////
// Constructor(s)

public ASTRuntime()
    throws ASTException
{
    this(Encoder.Protobuf,null);
}

public ASTRuntime(AbstractIO io)
        throws ASTException
{
    this(Encoder.Protobuf,io);
}

public ASTRuntime(Encoder encoding)
        throws ASTException
{
    this(encoding,null);
}

public ASTRuntime(Encoder encoding, AbstractIO io)
        throws ASTException
{
    setEncoding(encoding);
    setIO(io);
}

//////////////////////////////////////////////////
// Get/Set

public void
setIO(AbstractIO io)
{
    this.io = io;
    if(encoder != null && io != null)
	encoder.setIO(io);
}

/* Reclaim a runtime instance  */
public void
close()
    throws IOException
{
    io.close();
}

//////////////////////////////////////////////////
// The Encoding is used to find (by reflection)
// the name of the encoding class

public void
setEncoding(Encoder coder)
    throws ASTException
{
    ClassLoader loader = ASTRuntime.class.getClassLoader();

   // Try to locate the encoder class
   String encodingclassname = DFALTPACKAGE
				    + "."
				    + coder.getClassName()
				    ;
    Encoding encoder = null;
    Class encodingclass = null;
    try {
        encodingclass = Class.forName(encodingclassname);
    } catch (ClassNotFoundException e) {
	throw new ASTException("Class not found: "+encodingclassname);
    }
    try {
	encoder = (Encoding)encodingclass.newInstance();
    } catch (Exception e) {
	throw new ASTException("Could not create encoder instance: "+encodingclassname);
    }
    if(io != null)
        encoder.setIO(io);
    this.encoder = encoder;
}

//////////////////////////////////////////////////
// Implement by delegation all the abstract methods
// of Encoding class
//////////////////////////////////////////////////

boolean
read(byte[] buf, int offset, int len) throws IOException
{return encoder.read(buf, offset, len);}

void
write(int len, byte[] buf) throws IOException
{encoder.write(len, buf);}

void
mark(int avail) throws IOException
{encoder.mark(avail);}

void
unmark() throws IOException
{encoder.unmark();}

//////////////////////////////////////////////////
/* Given an unknown field, skip past it */

public void skip_field(int wiretype, int fieldno) throws IOException
{encoder.skip_field(wiretype, fieldno);}

//////////////////////////////////////////////////
/* Procedure to calulate size of a value;
   note that this is the size of an actual
   value.
 */
public int getSize(int sort, float val)
{return encoder.getSize(sort, val);}

public int getSize(int sort, double val)
{return encoder.getSize(sort, val);}

public int getSize(int sort, boolean val)
{return encoder.getSize(sort, val);}

public int getSize(int sort, String val)
{return encoder.getSize(sort, val);}

public int getSize(int sort, byte[] val)
{return encoder.getSize(sort, val);}

public int getSize(int sort, long val) throws ASTException
{return encoder.getSize(sort, val);}

public int getSizePacked(int sort, int[] val)
{return encoder.getSizePacked(sort, val);}

public int getSizePacked(int sort, long[] val)
{return encoder.getSizePacked(sort, val);}

public int getSizePacked(int sort, float[] val)
{return encoder.getSizePacked(sort, val);}

public int getSizePacked(int sort, double[] val)
{return encoder.getSizePacked(sort, val);}

public int getSizePacked(int sort, boolean[] val)
{return encoder.getSizePacked(sort, val);}

/* Procedure to calculate the size a message
   including its prefix size, when given
   the unprefixed message size
 */
public int getMessageSize(int size)
{return encoder.getMessageSize(size);}

/* Procedure to calulate size of a tag */
public int getTagSize(int sort, int fieldno)
{return encoder.getTagSize(sort, fieldno);}

//////////////////////////////////////////////////
/* Procedures to read/write tags */

public void write_tag(int sort, int fieldno) throws IOException 
{encoder.write_tag(sort, fieldno);}

/* Procedure to extract tags; args simulate call by ref */
public boolean read_tag(int[] wiretype, int[] fieldno) throws IOException
{return encoder.read_tag(wiretype, fieldno);}

/* Procedures to write and sizes */
public void write_size(int size) throws IOException
{encoder.write_size(size);}

public int read_size() throws IOException
{return encoder.read_size();}

//////////////////////////////////////////////////

public double read_primitive_double(int sort) throws IOException
{return encoder.read_primitive_double(sort);}

public float read_primitive_float(int sort) throws IOException
{return encoder.read_primitive_float(sort);}

public boolean read_primitive_boolean(int sort) throws IOException
{return encoder.read_primitive_boolean(sort);}

public String read_primitive_string(int sort) throws IOException
{return encoder.read_primitive_string(sort);}

public byte[] read_primitive_bytes(int sort) throws IOException
{return encoder.read_primitive_bytes(sort);}

public int read_primitive_int(int sort) throws IOException
{return encoder.read_primitive_int(sort);}

public long read_primitive_long(int sort) throws IOException
{return encoder.read_primitive_long(sort);}

public double[] read_primitive_packed_double(int sort) throws IOException
{return encoder.read_primitive_packed_double(sort);}

public float[] read_primitive_packed_float(int sort) throws IOException
{return encoder.read_primitive_packed_float(sort);}

public boolean[] read_primitive_packed_bool(int sort) throws IOException
{return encoder.read_primitive_packed_bool(sort);}

// Read a sequence of values that are expected to be 32 bit integers
// i.e. sort= Ast_int32, _sint32, Ast_fixed32, Ast_sfixed32

public int[] read_primitive_packed_int(int sort) throws IOException
{return encoder.read_primitive_packed_int(sort);}

// Read a sequence of values that are expected to be 64 bit integers
// i.e. sort= Ast_int64, _sint64, Ast_fixed64, Ast_sfixed64

public long[] read_primitive_packed_long(int sort) throws IOException
{return encoder.read_primitive_packed_long(sort);}

// Write a sequence of values
public void write_primitive(int sort, double val) throws IOException
{encoder.write_primitive(sort, val);}

public void write_primitive(int sort, float val) throws IOException
{encoder.write_primitive(sort, val);}

public void write_primitive(int sort, boolean val) throws IOException
{encoder.write_primitive(sort, val);}

public void write_primitive(int sort, String val) throws IOException
{encoder.write_primitive(sort, val);}

public void write_primitive(int sort, byte[] bval) throws IOException
{encoder.write_primitive(sort, bval);}

public void write_primitive(int sort, long val) throws IOException
{encoder.write_primitive(sort, val);}

public void write_primitive_packed(int sort, double[] val) throws IOException
{encoder.write_primitive_packed(sort, val);}

public void write_primitive_packed(int sort, float[] val) throws IOException
{encoder.write_primitive_packed(sort, val);}

public void write_primitive_packed(int sort, boolean[] val) throws IOException
{encoder.write_primitive_packed(sort, val);}

public void write_primitive_packed(int sort, int[] val) throws IOException
{encoder.write_primitive_packed(sort, val);}

public void write_primitive_packed(int sort, long[] val) throws IOException
{encoder.write_primitive_packed(sort, val);}

/* Read into Repeated field */
public double[] repeat_append(int sort, double newval, double[] list)
{return encoder.repeat_append(sort, newval, list);}

public float[] repeat_append(int sort, float newval, float[] list)
{return encoder.repeat_append(sort, newval, list);}

public boolean[] repeat_append(int sort, boolean newval, boolean[] list)
{return encoder.repeat_append(sort, newval, list);}

public int[] repeat_append(int sort, int newval, int[] list)
{return encoder.repeat_append(sort, newval, list);}

public long[] repeat_append(int sort, long newval, long[] list)
{return encoder.repeat_append(sort, newval, list);}

public String[] repeat_append(int sort, String newval, String[] list)
{return encoder.repeat_append(sort, newval, list);}

public byte[][] repeat_append(int sort, byte[] newval, byte[][] list)
{return encoder.repeat_append(sort, newval, list);}

// Special handling for messages and enums
public Object repeat_extend(Object list, java.lang.Class klass)
{return encoder.repeat_extend(list, klass);}

} /*class ASTRuntime*/


