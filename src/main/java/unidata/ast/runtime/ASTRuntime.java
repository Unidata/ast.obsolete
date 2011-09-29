/*********************************************************************
 *   Copyright 2010, UCAR/Unidata
 *   See netcdf/COPYRIGHT file for copying and redistribution conditions.
 *   $Id$
 *   $Header$
 *********************************************************************/

package unidata.ast.runtime;

import java.io.IOException;
import java.nio.charset.Charset;

import static unidata.ast.runtime.Internal.*;

public class ASTRuntime
{

/* Define error codes as strings  */
static final String AST_EOF = "";
static final String AST_ENOMEM = "";
static final String AST_EFAIL= "";
static final String AST_EIO = "";
static final String AST_ECURL = "";

/* Define primitive types enum */
public enum Sort {
    Ast_double(8,Ast_64bit),
    Ast_float(4,Ast_32bit),
    Ast_int32(0,Ast_varint),
    Ast_uint32(0,Ast_varint),
    Ast_int64(0,Ast_varint),
    Ast_uint64(0,Ast_varint),
    Ast_sint32(0,Ast_varint),
    Ast_sint64(0,Ast_varint),
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
    Sort(int size, int wiretype) {this.size = size; this.wiretype = wiretype;}
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


static final Charset utf8 = Charset.forName("utf-8");

//////////////////////////////////////////////////
// Instance fields

AbstractIO io = null;

byte[] buffer = new byte[Sort.MAXTYPESIZE]; // max needed except for string|bytes

ByteBuffer abuffer = new ByteBuffer();

//////////////////////////////////////////////////
// Constructor(s)

public ASTRuntime()
    throws ASTException
{
}

public ASTRuntime(AbstractIO io)
        throws ASTException
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
    throws IOException
{
    io.close();
}

/* Given an unknown field, skip past it */
void
skip_field(int wiretype, int fieldno)
    throws IOException
{
    int len;
    switch (wiretype) {
    case Ast_varint:
        len = readvarint(buffer);
	break;
    case Ast_32bit:
        read((len=4),buffer);
	break;
    case Ast_64bit:
	len = 8;
        read((len=8),buffer);
	break;
    case Ast_counted:
        len = readvarint(buffer);
        /* get the count */
	len = uint32_decode(len,buffer);
	/* Now skip "len" bytes */
	while(len > 0) {
	    int count = (len > buffer.length?buffer.length:len);
	    if(!read(count,buffer))
		throw new ASTException("skip_field: too few bytes");
	    len -= count;
	}
	break;
    default:
	throw new ASTException("skip_field: unexpected wiretype: "+wiretype);
    }
}

//////////////////////////////////////////////////
// Wrappers for io.read and io.write

boolean read(int len, byte[] buf) throws IOException
{return io.read(len,buf);}

void write(int len, byte[] buf) throws IOException {io.write(len,buf);}

//////////////////////////////////////////////////
/* Procedure to calulate size of a tag */
public int
getTagSize(Sort sort, int fieldno)
{
    int wiretype = sort.wiretype();
    int count = encode_tag(wiretype,fieldno,buffer);
    return count;
}

/* Procedures to read/write tags */
public void
write_tag(int wiretype, int fieldno)
    throws IOException
{
    int count = encode_tag(wiretype,fieldno,buffer);
    io.write(count,buffer);
}

/* Procedure to extract tags; args simulate call by ref */
public void 
read_tag(int[] wiretype, int[] fieldno)
    throws IOException
{
    int count;
    int key;

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
write_size(int size)
    throws IOException
{
    /* write size as varint */
    int len = uint32_encode(size,buffer);
    io.write(len,buffer);
}

/* Procedure to extract size */
public int
read_size()
    throws IOException
{
    int len = readvarint(buffer);
    return uint32_decode(len, buffer);
}

//////////////////////////////////////////////////

public int
readvarint(byte[] buffer)
    throws IOException
{
    int i=0;
    boolean more = true;
    while(i<Sort.MAXVARINTSIZE && more) {
	if(!read(1,buffer)) return -1; // eof
	if((0x80 & buffer[i]) == 0) more = false;
	buffer[i] = (byte)(0x7f & buffer[i]);
        i++;
    }
    return i;
}

/* Based on the wiretype, extract the proper number of bytes
   for an integer base value; return the length
   and place the bytes into the buffer.
   For ast counted, do this for the count,
   not the content.
*/

public int
readandcount(int wiretype, byte[] buffer)
    throws IOException
{
    int len = 0;
    int count;
    switch (wiretype) {
    case Ast_varint:
        count = readvarint(buffer);
	break;
    case Ast_32bit:
	count = 4;
        if(!read(4,buffer)) count = -1;
	break;
    case Ast_64bit:
	count = 8;
        if(!read(8,buffer)) count = -1;
	break;
    case Ast_counted: /* get the count */
        len = readvarint(buffer);
	count = uint32_decode(len,buffer);
	break;
    default:
	throw new ASTException(AST_EFAIL);
    }
    return count;
}

//////////////////////////////////////////////////

double
read_primitive_double()
    throws IOException
{
    int wiretype = Sort.Ast_double.wiretype();
    int len = readandcount(wiretype, buffer);
    double value = float64_decode(buffer);
    return value;
}

float
read_primitive_float()
    throws IOException
{
    int wiretype = Sort.Ast_float.wiretype();
    long len = readandcount(wiretype, buffer);
    float value = float32_decode(buffer);
    return value;
}

boolean
read_primitive_boolean()
    throws IOException
{
    int wiretype = Sort.Ast_bool.wiretype();
    int len = readandcount(wiretype, buffer);
    boolean value = bool_decode(len,buffer);
    return value;
}

String
read_primitive_string()
    throws IOException
{
    int wiretype = Sort.Ast_string.wiretype();
    int len = readandcount(wiretype, buffer);
    byte[] stringbuf = new byte[len];
    if(!read(len,stringbuf))
	throw new ASTException("too few bytes");
    return new String(stringbuf,utf8);
}

byte[]
read_primitive_bytes()
    throws IOException
{
    int wiretype = Sort.Ast_bytes.wiretype();
    int len = readandcount(wiretype, buffer);
    byte[] value = new byte[len];
    if(!read(len,value))
	throw new ASTException("too few bytes");
    System.arraycopy(buffer,0,value,0,len);
    return value;
}

int
read_primitive_int(Sort sort)
    throws IOException
{
    /* compute the wiretype from the sort */
    int wiretype = sort.wiretype();
    int len = readandcount(wiretype, buffer);
    switch (sort) {
    case Ast_enum: /* fall thru */
    case Ast_int32:
        return int32_decode(len,buffer);
    case Ast_uint32:
        return uint32_decode(len,buffer);
    case Ast_sint32:
        return unzigzag32(uint32_decode(len,buffer));
    case Ast_fixed32:
        return fixed32_decode(buffer);
    case Ast_sfixed32:
        return fixed32_decode(buffer);
    default: break;
    }
    throw new ASTException(AST_EFAIL);
}

long
read_primitive_int64(Sort sort)
    throws IOException
{
    /* compute the wiretype from the sort */
    int wiretype = sort.wiretype();
    int len = readandcount(wiretype, buffer);
    switch (sort) {
    case Ast_int64:
        return int64_decode(len,buffer);
    case Ast_uint64:
        return uint64_decode(len,buffer);
    case Ast_sint64:
        return unzigzag64(uint64_decode(len,buffer));
    case Ast_fixed64:
        return fixed64_decode(buffer);
    case Ast_sfixed64:
        return fixed64_decode(buffer);
    default: break;
    }
    throw new ASTException(AST_EFAIL);
}

double[]
read_primitive_double_packed()
    throws IOException
{
    // extract the count
    int wiretype = Ast_counted;
    int count = readandcount(wiretype,buffer);
    int size = Sort.Ast_double.javasize();

    // Pull out the values as doubles
    int ndoubles = count/size;
    double[] output = new double[ndoubles];

    // Read bytes and insert into the converter
    for(int i=0;i<ndoubles;i++) {
	if(!read(size,buffer))
	    throw new ASTException("too few bytes");
	output[i] = float64_decode(buffer);
    }	
    return output;
}

float[]
read_primitive_float_packed()
    throws IOException
{
    // extract the count
    int wiretype = Ast_counted;
    int count = readandcount(wiretype,buffer);
    int size = Sort.Ast_float.javasize();

    // Pull out the values as floats
    int nfloats = count/size;
    float[] output = new float[nfloats];

    // Read bytes and insert into the converter
    for(int i=0;i<nfloats;i++) {
	if(!read(size,buffer))
	    throw new ASTException("too few bytes");
	output[i] = float32_decode(buffer);
    }	
    return output;
}

boolean[]
read_primitive_bool_packed()
    throws IOException
{
    // extract the count
    int wiretype = Ast_counted;
    int count = readandcount(wiretype,buffer);
    int size = Sort.Ast_bool.javasize();

    // Pull out the values as bools
    int nbools = count/size;
    boolean[] output = new boolean[nbools];

    // Read bytes and insert into the converter
    for(int i=0;i<nbools;i++) {
	if(!read(size,buffer))
	    throw new ASTException("too few bytes");
	output[i] = bool_decode(size,buffer);
    }	
    return output;
}

// Read a sequence of values that are expected to be 32 bit integers
// i.e. sort= Ast_int32,Ast_sint32,Ast_fixed32,Ast_sfixed32

int[]
read_primitive_int32_packed(Sort sort)
    throws IOException
{
    // extract the count; put data into buffer (except for Ast_counted)
    int count = readandcount(Ast_counted,buffer);
    int size = Sort.Ast_bool.javasize(); // 0 => unknown => varint
    int len = 0;
    IntBuffer intdata = new IntBuffer();

// i.e. sort= Ast_(u)int32,Ast_sint32,Ast_fixed32,Ast_sfixed32
loop: for(;;) {
	switch (sort.wiretype) {
        case Ast_varint:
            len = readvarint(buffer);
	    if(len < 0) break loop;
	    break;	
        case Ast_32bit:
            len = readandcount(Ast_32bit,buffer);
	    if(len < 0) break loop;
	    break;
        default:
	    throw new ASTException("illegal wiretype: " + sort.wiretype);
        }
        switch (sort) {
        case Ast_int32:
	    intdata.add(int32_decode(len,buffer));
	    break;			
        case Ast_uint32:
	    intdata.add(uint32_decode(len,buffer));
	    break;			
        case Ast_sint32:
	    intdata.add(unzigzag32(uint32_decode(len,buffer)));
	    break;			
        case Ast_fixed32:
	    intdata.add(fixed32_decode(buffer));
	    break;
        case Ast_sfixed32:
	    intdata.add(fixed32_decode(buffer));
	    break;
        default: break;
        }
    } // for
    return intdata.getContent();
}

// Read a sequence of values that are expected to be 64 bit integers
// i.e. sort= Ast_int64,Ast_sint64,Ast_fixed64,Ast_sfixed64

long[]
read_primitive_int64_packed(Sort sort)
    throws IOException
{
    // extract the count; put data into buffer (except for Ast_counted)
    int count = readandcount(Ast_counted,buffer);
    int size = Sort.Ast_bool.javasize(); // 0 => unknown => varint
    int len = 0;
    LongBuffer longdata = new LongBuffer();

// i.e. sort= Ast_(u)int64,Ast_sint64,Ast_fixed64,Ast_sfixed64
loop: for(;;) {
	switch (sort.wiretype) {
        case Ast_varint:
            len = readvarint(buffer);
	    if(len < 0) break loop;
	    break;	
        case Ast_64bit:
            len = readandcount(Ast_64bit,buffer);
	    if(len < 0) break loop;
	    break;
        default:
	    throw new ASTException("illegal wiretype: " + sort.wiretype);
        }

        switch (sort) {
        case Ast_int64:
	    longdata.add(int64_decode(len,buffer));
	    break;			
        case Ast_uint64:
	    longdata.add(uint64_decode(len,buffer));
	    break;			
        case Ast_sint64:
	    longdata.add(unzigzag64(uint64_decode(len,buffer)));
	    break;			
        case Ast_fixed64:
	    longdata.add(fixed64_decode(buffer));
	    break;
        case Ast_sfixed64:
	    longdata.add(fixed64_decode(buffer));
	    break;
        default: break;
        }
    } // for
    return longdata.getContent();
}

void
write_primitive(Sort sort, double val)
    throws IOException
{
    assert(sort == Sort.Ast_double);
    int count = float64_encode(val,buffer);
    write(count,buffer);
}

void
write_primitive(Sort sort, float val)
    throws IOException
{
    assert(sort == Sort.Ast_float);
    int count = float32_encode(val,buffer);
    write(count,buffer);
}

void
write_primitive(Sort sort, boolean val)
    throws IOException
{
    assert(sort == Sort.Ast_bool);
    int count = bool_encode(val,buffer);
    write(count,buffer);
	throw new ASTException("write failure");
}

void
write_primitive(Sort sort, String val)
        throws IOException
{
    assert(sort == Sort.Ast_string);
    byte[] bval = val.getBytes(utf8);
    int len = bval.length;
    int count = uint32_encode(len,buffer);
    write_size(len);
    write(len,bval);
}

void
write_primitive(Sort sort, byte[] bval)
        throws IOException
{
    assert(sort == Sort.Ast_bytes);
    int len = bval.length;
    int count = uint32_encode(len,buffer);
    write_size(len);
    write(len,bval);
}

void
write_primitive(Sort sort, long val)
        throws IOException
{
    int ival = (int)(val & 0xffffffff);
    int count = 0;
    /* Write the data in proper wiretype format using the sort */
    switch (sort) {
    case Ast_int32:
	count = int32_encode(ival,buffer);
        write(count,buffer);
	break;
    case Ast_int64:
	count = int64_encode(val,buffer);
        write(count,buffer);
	break;
    case Ast_uint32:
	count = uint32_encode(ival,buffer);
        write(count,buffer);
	break;
    case Ast_uint64:
	count = uint64_encode(val,buffer);
	write_size(count);
	break;
    case Ast_sint32:
	count = sint32_encode(ival,buffer);
        write(count,buffer);
	break;
    case Ast_sint64:
	count = sint64_encode(ival,buffer);
        write(count,buffer);
	break;
    case Ast_enum:
	count = int32_encode(ival,buffer);
        write(count,buffer);
	break;
    case Ast_fixed32: /* fall thru */
    case Ast_sfixed32: /* fall thru */
    case Ast_float:
	count = fixed32_encode(ival,buffer);
        write(count,buffer);
	break;
    case Ast_fixed64:  /* fall thru */
    case Ast_sfixed64: /* fall thru */
    case Ast_double:
	count = fixed64_encode(ival,buffer);
        write(count,buffer);
	break;
    default:
	throw new ASTException("unexpected sort: "+sort);
    }
}

void
write_primitive_packed(Sort sort, double[] val)
    throws IOException
{
    assert(sort == Sort.Ast_double);
    write_size(val.length*getSize(Sort.Ast_double,(double)0.0));
    for(int i=0;i<val.length;i++)
	write_primitive(Sort.Ast_double,val[i]);
}

void
write_primitive_packed(Sort sort, float[] val)
        throws IOException
{
    assert(sort == Sort.Ast_float);
    write_size(val.length*getSize(Sort.Ast_float,(float)0.0));
    for(int i=0;i<val.length;i++)
	write_primitive(Sort.Ast_float,val[i]);
}

void
write_primitive_packed(Sort sort, boolean[] val)
        throws IOException
{
    assert(sort == Sort.Ast_bool);
    write_size(val.length*getSize(Sort.Ast_bool,true));
    for(int i=0;i<val.length;i++)
	write_primitive(Sort.Ast_bool,val[i]);
}

void
write_primitive_packed(Sort sort, int[] val)
        throws IOException
{
    // ugh; we need to encode into a memory buffer
    // to see the correct length for varints
    int count = 0;
    abuffer.clear();
    abuffer.setSize(val.length*Sort.MAXVARINTSIZE); // max possible size
    switch (sort) {
    case Ast_enum:
    case Ast_int32:
	for(int i=0;i<val.length;i++) {
	    count = int32_encode(val[i],buffer);
	    abuffer.add(buffer,0,count);
	}
	break;
    case Ast_uint32:
	for(int i=0;i<val.length;i++) {
	    count = uint32_encode(val[i],buffer);
	    abuffer.add(buffer,0,count);
	}
	break;
    case Ast_sint32:
	for(int i=0;i<val.length;i++) {
	    count = sint32_encode(val[i],buffer);
	    abuffer.add(buffer,0,count);
	}
	break;
    case Ast_fixed32: /* fall thru */
    case Ast_sfixed32: /* fall thru */
    case Ast_float:
	for(int i=0;i<val.length;i++) {
  	    count = fixed32_encode(val[i],buffer);
	    abuffer.add(buffer,0,count);
	}
	break;
    default:
	throw new ASTException("unexpected sort: "+sort);
    }
    // write the count and the data
    int len = abuffer.getPosition();
    byte[] data = abuffer.getBuffer();
    count = uint32_encode(len,buffer);
    write_size(count);
    write(len,data);
}

void
write_primitive_packed(Sort sort, long[] val)
        throws IOException
{
    int count = 0;

    abuffer.clear();
    abuffer.setSize(val.length*Sort.MAXVARINTSIZE); // max possible size

    switch (sort) {
    case Ast_enum:
    case Ast_int64:
	for(int i=0;i<val.length;i++) {
	    count = int64_encode(val[i],buffer);
	    abuffer.add(buffer,0,count);
	}
	break;
    case Ast_uint64:
	for(int i=0;i<val.length;i++) {
	    count = uint64_encode(val[i],buffer);
	    abuffer.add(buffer,0,count);
	}
	break;
    case Ast_sint64:
	for(int i=0;i<val.length;i++) {
	    count = sint64_encode(val[i],buffer);
	    abuffer.add(buffer,0,count);
	}
	break;
    case Ast_fixed64: /* fall thru */
    case Ast_sfixed64: /* fall thru */
    case Ast_float:
	for(int i=0;i<val.length;i++) {
  	    count = fixed64_encode(val[i],buffer);
	    abuffer.add(buffer,0,count);
	}
	break;
    default:
	throw new ASTException("unexpected sort: "+sort);
    }
    // write the count and the data
    int len = abuffer.getPosition();
    byte[] data = abuffer.getBuffer();
    count = uint32_encode(len,buffer);
    write_size(count);
    write(len,data);
}

/* Read into Repeated field */
double[]
repeat_append(Sort sort, double newval, double[] list)
{
    if(list == null) {list = new double[1];}
    else {
	double[] newlist = new double[list.length+1];
	System.arraycopy(list,0,newlist,0,list.length);
    }
    list[list.length-1] = newval;
    return list;
}

float[]
repeat_append(Sort sort, float newval, float[] list)
{
    if(list == null) {list = new float[1];}
    else {
	float[] newlist = new float[list.length+1];
	System.arraycopy(list,0,newlist,0,list.length);
    }
    list[list.length-1] = newval;
    return list;
}

boolean[]
repeat_append(Sort sort, boolean newval, boolean[] list)
{
    if(list == null) {list = new boolean[1];}
    else {
	boolean[] newlist = new boolean[list.length+1];
	System.arraycopy(list,0,newlist,0,list.length);
    }
    list[list.length-1] = newval;
    return list;
}

int[]
repeat_append(Sort sort, int newval, int[] list)
{
    if(list == null) {list = new int[1];}
    else {
	int[] newlist = new int[list.length+1];
	System.arraycopy(list,0,newlist,0,list.length);
    }
    list[list.length-1] = newval;
    return list;
}


long[]
repeat_append(Sort sort, long newval, long[] list)
{
    if(list == null) {list = new long[1];}
    else {
	long[] newlist = new long[list.length+1];
	System.arraycopy(list,0,newlist,0,list.length);
    }
    list[list.length-1] = newval;
    return list;
}

String[]
repeat_append(Sort sort, String newval, String[] list)
{
    if(list == null) {list = new String[1];}
    else {
	String[] newlist = new String[list.length+1];
	System.arraycopy(list,0,newlist,0,list.length);
    }
    list[list.length-1] = newval;
    return list;
}

byte[][]
repeat_append(Sort sort, byte[] newval, byte[][] list)
{
    if(list == null) {list = new byte[1][];}
    else {
	byte[][] newlist = new byte[list.length+1][];
	System.arraycopy(list,0,newlist,0,list.length);
    }
    list[list.length-1] = newval;
    return list;
}

} /*class ASTRuntime*/



