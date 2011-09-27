/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.protobuf.ast.runtime;

import java.nio.charset.Charset;

public class Runtime
{

/* Define error codes */
public enum Error {
    AST_NOERR(0),
    AST_EOF(-1),
    AST_ENOMEM(-2),
    AST_EFAIL(-3),
    AST_EIO(-4),
    AST_ECURL(-5);

    private final int errno;
    Errno(int err) {this.errno = err;}
    public int getErrno() {return errno; }
} /*enum Error*/

/* Define primitive types enum */
public enum Sort {
    Ast_double(8,Ast_64bit),
    Ast_float(4,Ast_32bit),
    Ast_int32(4,Ast_varint),
    Ast_int64(8,Ast_varint);
    Ast_uint64(8,Ast_varint)
    Ast_sint32(4,Ast_varint),
    Ast_sint64(8,Ast_varint),
    Ast_fixed32(4,Ast_32bit),
    Ast_fixed64(8,Ast_64bit),
    Ast_sfixed32(4,Ast_32bit),
    Ast_sfixed64(8,Ast_64bit),
    Ast_string(0,Ast_counted),
    Ast_bytes(0,Ast_counted),
    Ast_bool(0,Ast_varint),
    Ast_enum(4,Ast_varint),
    Ast_message(0,Ast_counted);

    static final int MAXTYPESIZE = 8;
    static final int MAXVARINTSIZE = 10;

    private final int size;
    private final int wiretype;
    Sort(int size, int wiretype) {this.size = size; this.wiretype = wiretype}
    public int javasize() {return size; }
    public int wiretype() {return wiretype; }

} /*enum Sort*/

/* Define wiretypes as integers, not enum*/
static final int Ast_varint = (0); /* int32, int64, uint32, uint64, sint32, sint64, bool, enum*/
static final int Ast_64bit = (1); /* fixed64, sfixed64, double*/
static final int Ast_counted = (2); /* Length-delimited: string, bytes, embedded messages, packed repeated fields*/
static final int Ast_startgroup = (3); /* Start group (deprecated) */
static final int Ast_endgroup = (4); /* end group (deprecated) */
static final int Ast_32bit = (5); /* fixed32, sfixed32, float*/

/* Define the field modes */
public enum Fieldmode {
    Ast_required,
    Ast_optional,
    Ast_repeated;
}


public enum Sortsize { sizecount=0, size32=32, size64=64 };

static final Charset utf8 = Charset.forName("utf-8");

//////////////////////////////////////////////////
// Instance fields

AbstractIO io = null;

byte[] buffer = new byte[Sort.MAXTYPESIZE]; // max needed except for string|bytes

//////////////////////////////////////////////////
// Constructor(s)

public Runtime()
{
}

public Runtime(AbstractIO io)
{
    this();
    setIO(io);
}

//////////////////////////////////////////////////
// Get/Set

public void
setIO(AbstractIO io)
{
    this.io = io;
}


/* Reclaim a runtime instance  */
public void
close()
{
    io.close();
}

/* Given an unknown field, skip past it */
void
skip_field(int wiretype, int fieldno)
    throws IOException
{
    long len;
    switch (wiretype) {
    case Ast_varint:
        len = readvarint(buffer);
	break;
    case Ast_32bit:
        io.read((len=4),buffer);
	break;
    case Ast_64bit:
	len = 8;
        io.read((len=8),buffer);
	break;
    case Ast_counted:
        len = readvarint(buffer);
        /* get the count */
	len = uint64_decode(len,buffer);
	/* Now skip "len" bytes */
	while(len > 0) {
	    int count = (len > buffer.length?buffer.length:len);
	    if(!io.read(count,buffer))
		throw new IOException("skip_field: too few bytes");
	    len -= count;
	}
	break;
    default: status = AST_EFAIL; break;
	throw new IOException("skip_field: unexpected wiretype: "+wiretype);
    }
}

/* Procedure to calulate size of a tag */
public long
get_tagsize(Sort sort, int fieldno)
{
    int wiretype = sort.wiretype();
    count = encode_tag(wiretype,fieldno,guarantee());
    return count;
}

/* Procedures to read/write tags */
public void
write_tag(long wiretype, long fieldno)
    throws Exception
{
    long count = encode_tag(wiretype,fieldno,guarantee());
    io.write(count,buffer);
}

/* Procedure to extract tags; args simulate call by ref */
public void 
read_tag(long[] wiretypep, long[] fieldnop)
{
    long count;
    long key, wiretype, fieldno;

    /* Extract the wiretype + index */
    count = readvarint(buffer);

    /* convert from varint */
    key = uint32_decode(count,buffer);

    /* Extract the wiretype and fieldno */
    wiretype[0] = (key & 0x7);
    fieldno[0] = (key >>> 3);
}

/* Procedure to write out size */
public void
write_size(long size)
{
    /* write size as varint */
    long len = uint64_encode(size,guarantee());
    io.write(len,buffer);
}

/* Procedure to extract size */
public long
read_size()
{
    long len = readvarint(guarantee());
    return uint64_decode(len,buffer);
}

//////////////////////////////////////////////////

double
read_primitive_double()
    throws IOException
{
    int wiretype = Sort.DOUBLE.wiretype();
    int len = Internal.readcount(wiretype, buffer);
    double value = Internal.float64_decode(len,buffer);
    return value;
}

float
read_primitive_float()
    throw IOException
{
    int wiretype = Sort.FLOAT.wiretype();
    long len = Internal.readcount(wiretype, buffer);
    float value = Internal.float32_decode(len,buffer);
    return value;
}

boolean
read_primitive_boolean()
    throw IOException
{
    int wiretype = Sort.BOOLEAN.wiretype();
    long len = Internal.readcount(wiretype, buffer);
    boolean value = Internal.boolean_decode(len,buffer);
    return value;
}

String
read_primitive_string()
    throw IOException
{
    int wiretype = Sort.STRING.wiretype();
    long len = Internal.readcount(wiretype, buffer);
    byte[] stringbuf = new byte[len];
    if(!io.read(len,stringbuf))
	throw new IOException("too few bytes");
    return new String(stringbuf,utf8);
}

byte[]
read_primitive_bytes()
    throw IOException
    throw IOException
{
    int wiretype = Sort.BYTES.wiretype();
    long len = Internal.readcount(wiretype, buffer);
    byte[] value = new byte[len];
    if(!io.read(len,value))
	throw new IOException("too few bytes");
    System.arraycopy(buffer,0,value,0,len);
    return value;
}

int
read_primitive_int(Sort sort)
    throw IOException
{
    /* compute the wiretype from the sort */
    int wiretype = sort.wiretype();
    long len = Internal.readcount(wiretype, buffer);
    switch (sort) {
    case Ast_enum: /* fall thru */
    case Ast_int32:
        return int32_decode(len,buffer);
    case Ast_int64:
        return int64_decode(len,buffer);
    case Ast_uint32:
        return uint32_decode(len,buffer);
    case Ast_uint64:
        return uint64_decode(len,buffer);
    case Ast_sint32:
        return unzigzag32(uint32_decode(len,buffer));
    case Ast_sint64:
        return unzigzag64(uint64_decode(len,buffer));
    case Ast_fixed32:
        return fixed32_decode(buffer);
    case Ast_sfixed32:
        return fixed32_decode(buffer);
    case Ast_fixed64:
        return fixed64_decode(buffer);
    case Ast_sfixed64:
        return fixed64_decode(buffer);
    default: break;
    }
    throw new IOException(AST_EFAIL);
}

double[]
read_primitive_double_packed(double[] field)
    throw IOException
{
    // extract the count
    int wiretype = sort.wiretype();
    count = readcount(wiretype,buffer);

    // Wrap the byte buffer
    java.nio.ByteBuffer converter = ByteBuffer.wrap(buffer,0,buffer.length);

    // Pull out the values as doubles
    int ndoubles = count/AST.Sort.DOUBLE.jtypesize()];
    int fieldcount = (field == null ? 0 : field.length);
    double[] output = new double[fieldcount+ndoubles];

    // Prefix with the field, if any
    if(fieldcount > 0)
        System.arraycopy(field,0,output,0,fieldcount);

    // Read bytes and insert into the converter
    for(int i=0;i<ndoubles;i++) {
	io.read(Sort.DOUBLE.jtypesize(),buffer)
	converter.put(buffer,0,Sort.DOUBLE.jtypesize());
    }	
    // do mass conversion
    converter.asDoubleBuffer().get(output,fieldcount,ndoubles);
    return output;
}

float
read_primitive_float_packed(float[] field)
    throw IOException
{
    // extract the count
    int wiretype = sort.wiretype();
    count = readcount(wiretype,buffer);

    // Wrap the byte buffer
    java.nio.ByteBuffer converter = ByteBuffer.wrap(buffer,0,buffer.length);

    // Pull out the values as floats
    int nfloats = count/AST.Sort.FLOAT.jtypesize();
    int fieldcount = (field == null ? 0 : field.length);
    float[] output = new float[fieldcount+nfloats];

    // Prefix with the field, if any
    if(fieldcount > 0)
        System.arraycopy(field,0,output,0,fieldcount);

    // Read bytes and insert into the converter
    for(int i=0;i<nfloats;i++) {
	io.read(Sort.FLOAT.jtypesize(),buffer)
	converter.put(buffer,0,Sort.FLOAT.jtypesize());
    }	
    // do mass conversion
    converter.asFloatBuffer().get(output,fieldcount,nfloats);
    return output;
}

boolean
read_primitive_boolean_packed(boolean[] field)
    throw IOException
{
    // extract the count
    int wiretype = sort.wiretype();
    count = readcount(wiretype,buffer);

    // read count bytes of data
    byte[] data = new byte[count];
    io.read(count,data);

    // Pull out the values as booleans
    int nbooleans = count/AST.Sort.BOOLEAN.jtypesize();
    int fieldcount = (field == null ? 0 : field.length);
    boolean[] output = new boolean[fieldcount+nbooleans];
    // Prefix with the field, if any
    if(fieldcount > 0)
        System.arraycopy(field,0,output,0,fieldcount);

    for(int i=0;i<nbooleans;i++) {
	if(!io.read(1,buffer))
   	    throw new IOException("packed boolean: too few bytes");
	output[fieldcount+i] = (buffer[0] == 0 ? false : true);
    }
    return output;
}

// Read a value that fits into a signed 32 bit integer
int[]
read_primitive_int32_packed(Sort sort, int[] field)
    throw IOException
{
    // extract the count; put data into buffer (except for Ast_counted)
    int wiretype = sort.wiretype();
    count = readcount(wiretype,buffer);

    // Convert to a java.nio.ByteBuffer
    java.nio.ByteBuffer data = java.nio.ByteBuffer.wrap(data,0,count);
    java.nio.IntBuffer intdata = data.asIntBuffer();

    int len = 0;
    long lvalue = 0;
    int ivalue = 0;
    int wiretype = sort.wiretype();

loop: for(;;) {
        switch (sort) {
        case Ast_int32:
        case Ast_uint32:
            len = readvarint(buffer);
	    if(len < 0) break loop;
	    lvalue = int64_decode(len,buffer);
	    intdata.put((int)lvalue);	    
	    break;			
        case Ast_sint32:
            len = readvarint(buffer);
	    if(len < 0) break loop;
	    value = int64_decode(len,buffer);
	    intdata.put((int)lvalue);	    
	    break;			

        return unzigzag32(uint32_decode(len,buffer));
    case Ast_sint64:
        return unzigzag64(uint64_decode(len,buffer));
    case Ast_fixed32:
        return fixed32_decode(buffer);
    case Ast_sfixed32:
        return fixed32_decode(buffer);
    case Ast_fixed64:
        return fixed64_decode(buffer);
    case Ast_sfixed64:
        return fixed64_decode(buffer);
    default: break;
    }
    throw new Exception(AST_EFAIL);
}





double read_primitive_double_packed(Sort sort)
float read_primitive_float_packed(Sort sort)
int read_primitive_int_packed(Sort sort)
long read_primitive_long_packed(Sort sort)
boolean read_primitive_boolean_packed(Sort sort)
String read_primitive_string_packed(Sort sort)
byte[] read_primitive_bytes_packed(Sort sort)

void write_primitive(Sort sort, double val)
void write_primitive(Sort sort, float val)
void write_primitive(Sort sort, int val)
void write_primitive(Sort sort, long val)
void write_primitive(Sort sort, boolean val)
void write_primitive(Sort sort, String val)
void write_primitive(Sort sort, byte[] val)

void write_primitive_packed(Sort sort, double[] val)
void write_primitive_packed(Sort sort, float[] val)
void write_primitive_packed(Sort sort, int[] val)
void write_primitive_packed(Sort sort, long[] val)
void write_primitive_packed(Sort sort, boolean[] val)
void write_primitive_packed(Sort sort, String val)
void write_primitive_packed(Sort sort, byte[] val)

Object read_enum(Class enumclass)
void write_enum(Class enumclass)

/* Read Repeated field */
double[] repeat_append(Sort sort, double newval, double[] list)
float[] repeat_append(Sort sort, float newval, float[] list)
int[] repeat_append(Sort sort, int newval, int[] list)
long[] repeat_append(Sort sort, long newval, long[] list)
boolean[] repeat_append(Sort sort, boolean newval, boolean[] list)
String[] repeat_append(Sort sort, String newval, String[] list)
byte[][] repeat_append(Sort sort, byte newval, byte[][] list)



} /*class Runtime*/



