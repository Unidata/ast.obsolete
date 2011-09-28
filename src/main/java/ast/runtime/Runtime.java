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
    Ast_int64(8,Ast_varint),
    Ast_uint64(8,Ast_varint),
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


static final Charset utf8 = Charset.forName("utf-8");

//////////////////////////////////////////////////
// Instance fields

AbstractIO io = null;

byte[] buffer = new byte[Sort.MAXTYPESIZE]; // max needed except for string|bytes

// Wrap the byte buffer
java.nio.ByteBuffer converter = ByteBuffer.wrap(buffer,0,buffer.length);

// For packed encoding
ASTBuffer abuffer = new ASTBuffer();

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
    throws ASTException
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
		throw new ASTException("skip_field: too few bytes");
	    len -= count;
	}
	break;
    default: status = AST_EFAIL; break;
	throw new ASTException("skip_field: unexpected wiretype: "+wiretype);
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
    throws ASTException
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
    throws ASTException
{
    int wiretype = Sort.DOUBLE.wiretype();
    int len = Internal.readcount(wiretype, buffer);
    double value = Internal.float64_decode(len,buffer);
    return value;
}

float
read_primitive_float()
    throw ASTException
{
    int wiretype = Sort.FLOAT.wiretype();
    long len = Internal.readcount(wiretype, buffer);
    float value = Internal.float32_decode(len,buffer);
    return value;
}

boolean
read_primitive_boolean()
    throw ASTException
{
    int wiretype = Sort.BOOLEAN.wiretype();
    long len = Internal.readcount(wiretype, buffer);
    boolean value = Internal.boolean_decode(len,buffer);
    return value;
}

String
read_primitive_string()
    throw ASTException
{
    int wiretype = Sort.STRING.wiretype();
    long len = Internal.readcount(wiretype, buffer);
    byte[] stringbuf = new byte[len];
    if(!io.read(len,stringbuf))
	throw new ASTException("too few bytes");
    return new String(stringbuf,utf8);
}

byte[]
read_primitive_bytes()
    throw ASTException
{
    int wiretype = Sort.BYTES.wiretype();
    long len = Internal.readcount(wiretype, buffer);
    byte[] value = new byte[len];
    if(!io.read(len,value))
	throw new ASTException("too few bytes");
    System.arraycopy(buffer,0,value,0,len);
    return value;
}

int
read_primitive_int(Sort sort)
    throw ASTException
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
    throw new ASTException(AST_EFAIL);
}

double[]
read_primitive_double_packed(double[] field)
    throw ASTException
{
    // extract the count
    int wiretype = sort.wiretype();
    count = readcount(wiretype,buffer);

    converter.clear();

    // Pull out the values as doubles
    int ndoubles = count/AST.Sort.DOUBLE.jtypesize()];
    int fieldcount = (field == null ? 0 : field.length);
    double[] output = new double[fieldcount+ndoubles];

    // Prefix with the field, if any
    if(fieldcount > 0)
        System.arraycopy(field,0,output,0,fieldcount);

    // Read bytes and insert into the converter
    for(int i=0;i<ndoubles;i++) {
	if(!read(Sort.DOUBLE.jtypesize(),buffer))
	    throw new ASTException("too few bytes");
	converter.put(buffer,0,Sort.DOUBLE.jtypesize());
    }	
    // do mass conversion
    converter.rewind();
    converter.asDoubleBuffer().get(output,fieldcount,ndoubles);
    return output;
}

float
read_primitive_float_packed(float[] field)
    throw ASTException
{
    // extract the count
    int wiretype = sort.wiretype();
    count = readcount(wiretype,buffer);

    // Wrap the byte buffer
    converter.clear();

    // Pull out the values as floats
    int nfloats = count/AST.Sort.FLOAT.jtypesize();
    int fieldcount = (field == null ? 0 : field.length);
    float[] output = new float[fieldcount+nfloats];

    // Prefix with the field, if any
    if(fieldcount > 0)
        System.arraycopy(field,0,output,0,fieldcount);

    // Read bytes and insert into the converter
    for(int i=0;i<nfloats;i++) {
	if(!read(Sort.FLOAT.jtypesize(),buffer))
	    throw new ASTException("too few bytes");
	converter.put(buffer,0,Sort.FLOAT.jtypesize());
    }	
    // do mass conversion
    converter.rewind();
    converter.asFloatBuffer().get(output,fieldcount,nfloats);
    return output;
}

boolean
read_primitive_boolean_packed(boolean[] field)
    throw ASTException
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
   	    throw new ASTException("packed boolean: too few bytes");
	output[fieldcount+i] = (buffer[0] == 0 ? false : true);
    }
    return output;
}

// Read a sequence of values that are expected to be 32 bit integers
// i.e. sort= Ast_int32,Ast_sint32,Ast_fixed32,Ast_sfixed32

int[]
read_primitive_int32_packed(Sort sort, int[] field)
    throw ASTException
{
    // extract the count; put data into buffer (except for Ast_counted)
    count = readcount(Ast_count,buffer);

    List<int> intdata = new ArrayList<int>();

    int len = 0;

// i.e. sort= Ast_(u)int32,Ast_sint32,Ast_fixed32,Ast_sfixed32
loop: for(;;) {
	switch (sort.wiretype) {
        case Ast_varint:
            len = readvarint(buffer);
	    if(len < 0) break loop;
	    break;	
        case Ast_32bit:
            len = readcount(Ast_32bit,buffer);
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
	    intdata.add(fixed32_decode(buffer))
	    break;
        case Ast_sfixed32:
	    intdata.add(fixed32_decode(buffer))
	    break;
        default: break;
        }
    } // for
    return intdata.toArray(new int[intdata.size()]);
}

// Read a sequence of values that are expected to be 64 bit integers
// i.e. sort= Ast_int64,Ast_sint64,Ast_fixed64,Ast_sfixed64

int[]
read_primitive_int64_packed(Sort sort, int[] field)
    throw ASTException
{
    // extract the count; put data into buffer (except for Ast_counted)
    count = readcount(Ast_count,buffer);

    List<long> longdata = new ArrayList<long>();

    int len = 0;

// i.e. sort= Ast_(u)int64,Ast_sint64,Ast_fixed64,Ast_sfixed64
loop: for(;;) {
	switch (sort.wiretype) {
        case Ast_varint:
            len = readvarint(buffer);
	    if(len < 0) break loop;
	    break;	
        case Ast_64bit:
            len = readcount(Ast_64bit,buffer);
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
	    longdata.add(fixed64_decode(buffer))
	    break;
        case Ast_sfixed64:
	    longdata.add(fixed64_decode(buffer))
	    break;
        default: break;
        }
    } // for
    return longdata.toArray(new int[longdata.size()]);
}

void
write_primitive(Sort sort, double val)
    throws ASTException
{
    assert(sort == Sort.Ast_double);
    converter.clear();
    converter.asDoubleBuffer().put(val);
    converter.rewind(); 
    long aslong = convert.asLongBuffer().get(0);
    count = fixed64_encode(aslong,buffer);
    if(!write(count,buffer) != count)
	throw new ASTException("write failure");
}

void
write_primitive(Sort sort, double val)
    throws ASTException
{
    assert(sort == Sort.Ast_float);
    converter.clear();
    converter.asFloatBuffer().put(val);
    converter.rewind(); 
    int asint = convert.asIntegerBuffer().get(0);
    count = fixed32_encode(asint,buffer);
    if(!write(count,buffer) != count)
	throw new ASTException("write failure");
}

void
write_primitive(Sort sort, boolean val)
    throws ASTException
{
    assert(sort == Sort.Ast_boolean);
    buffer[0] = (val?1:0);
    count = fixed32_encode(1,buffer);
    if(!write(count,buffer) != count)
	throw new ASTException("write failure");
}

void
write_primitive(Sort sort, String val)
{
    assert(sort == Sort.Ast_string);
    byte[] bval = val.getBytes(utf8);
    int len = bval.length;
    count = uint32_encode(len,buffer);
    if(!write(count,buffer)
	throw new ASTException("write failure");
    if(!write(len,bval))
        throw new ASTException("write failure");
}

void
write_primitive(Sort sort, byte[] bval)
{
    assert(sort == Sort.Ast_bytes);
    int len = bval.length;
    count = uint32_encode(len,buffer);
    if(!write(count,buffer)
	throw new ASTException("write failure");
    if(!write(len,bval))
        throw new ASTException("write failure");
}

void
write_primitive(Sort sort, long val)
{
    int ival = (int)(val & 0xffffffff);

    /* Write the data in proper wiretype format using the sort */
    switch (sort) {
    case ast_int32:
	count = int32_encode(ival,buffer);
	if(!write(count,buffer))
	    throw new ASTException("write failure");
	break;
    case ast_int64:
	count = int64_encode(val,buffer);
	if(!write(count,buffer))
	    throw new ASTException("write failure");
	break;
    case ast_uint32:
	count = uint32_encode(ival,buffer);
	if(!write(count,buffer))
	    throw new ASTException("write failure");
	break;
    case ast_uint64:
	count = uint64_encode(val,buffer);
	if(!write(count,buffer))
	    throw new ASTException("write failure");
	break;
    case ast_sint32:
	count = sint32_encode(ival,buffer);
	if(!write(count,buffer))
	    throw new ASTException("write failure");
	break;
    case ast_sint64:
	count = sint64_encode(ival,buffer);
	if(!write(count,buffer))
	    throw new ASTException("write failure");
	break;
    case ast_bool:
	count = boolean_encode(ival,buffer);
	if(!write(count,buffer))
	    throw new ASTException("write failure");
	break;
    case ast_enum:
	count = int32_encode(ival,buffer);
	if(!write(count,buffer))
	    throw new ASTException("write failure");
	break;
    case ast_fixed32: /* fall thru */
    case ast_sfixed32: /* fall thru */
    case ast_float:
	count = fixed32_encode(ival,buffer);
	if(!write(count,buffer))
	    throw new ASTException("write failure");
	break;
    case ast_fixed64:  /* fall thru */
    case ast_sfixed64: /* fall thru */
    case ast_double:
	count = fixed64_encode(ival,buffer);
	if(!write(count,buffer))
	    throw new ASTException("write failure");
	break;
    default:
	throw new ASTException("unexpected sort: "+sort);
    }
}

void
write_primitive_packed(Sort sort, double[] val)
{
    assert(sort == Sort.Ast_double);
    int count = uint32_encode(val.length()*get_size(Sort.Ast_double,0.0),buffer);
    if(!write(count,buffer) != count)
        throw new ASTException("write failure");
    for(int i=0;i<val.length;i++) {
        converter.clear();
        converter.asDoubleBuffer().put(val);
	converter.rewind();
        long aslong = converter.asLongBuffer().get(0);
        count = fixed64_encode(aslong,buffer);
        if(!write(count,buffer))
  	    throw new ASTException("write failure");
    }
}

void
write_primitive_packed(Sort sort, float[] val)
{
    assert(sort == Sort.Ast_float);
    int count = uint32_encode(val.length()*get_size(Sort.Ast_float,0.0),buffer);
    if(!write(count,buffer) != count)
        throw new ASTException("write failure");
    for(int i=0;i<val.length;i++) {
        converter.clear();
        converter.asFloatBuffer().put(val);
	converter.rewind();
        long aslong = converter.asLongBuffer().get(0);
        count = fixed64_encode(aslong,buffer);
        if(!write(count,buffer))
  	    throw new ASTException("write failure");
    }
}

void
write_primitive_packed(Sort sort, boolean[] val)
{
    assert(sort == Sort.Ast_float);
    int len = val.length;
    int count = uint32_encode(len,buffer);
    if(!write(count,buffer) != count)
        throw new ASTException("write failure");
    byte[] bbytes = new byte[len];
    for(int i=0;i<val.length;i++) {
	bbytes[i] = (val[i]?1:0);	
        if(!write(bbytes.length,buffer))
  	    throw new ASTException("write failure");
    }
}

void
write_primitive_packed(Sort sort, int[] val)
{
    // ugh; we need to encode into a memory buffer
    // to see the correct length for varint
    int len = val.length;
    int count = 0;
    abuffer.rewind();
    switch (sort) {
    case ast_enum:
    case ast_int32:
	for(int i=0;i<val.length;i++) {
	    count = int32_encode(val[i],buffer);
	    abuffer.write(buffer,0,count);
	}
	break;
    case ast_uint32:
	for(int i=0;i<val.length;i++) {
	    count = uint32_encode(val[i],buffer);
	    abuffer.write(buffer,0,count);
	}
	break;
    case ast_sint32:
	for(int i=0;i<val.length;i++) {
	    count = sint32_encode(val[i],buffer);
	    abuffer.write(buffer,0,count);
	}
	break;
    case ast_fixed32: /* fall thru */
    case ast_sfixed32: /* fall thru */
    case ast_float:
	for(int i=0;i<val.length;i++) {
  	    count = fixed32_encode(val[i],buffer);
	    abuffer.write(buffer,0,count);
	}
	break;
    default:
	throw new ASTException("unexpected sort: "+sort);
    }
    // write the count and the data
    count = uint32_encode(abuffer.getLength(),buffer);
    if(!write(count,buffer) != count)
        throw new ASTException("write failure");
    if(!write(abuffer.getLength(),abuffer.getBuffer()) != count)
        throw new ASTException("write failure");
}

void
write_primitive_packed(Sort sort, long[] val)
{
    // ugh; we need to encode into a memory buffer
    // to see the correct length for varint
    int len = val.length;
    int count = 0;
    abuffer.rewind();
    switch (sort) {
    case ast_enum:
    case ast_int64:
	for(int i=0;i<val.length;i++) {
	    count = int64_encode(val[i],buffer);
	    abuffer.write(buffer,0,count);
	}
	break;
    case ast_uint64:
	for(int i=0;i<val.length;i++) {
	    count = uint64_encode(val[i],buffer);
	    abuffer.write(buffer,0,count);
	}
	break;
    case ast_sint64:
	for(int i=0;i<val.length;i++) {
	    count = sint64_encode(val[i],buffer);
	    abuffer.write(buffer,0,count);
	}
	break;
    case ast_fixed64: /* fall thru */
    case ast_sfixed64: /* fall thru */
    case ast_float:
	for(int i=0;i<val.length;i++) {
  	    count = fixed64_encode(val[i],buffer);
	    abuffer.write(buffer,0,count);
	}
	break;
    default:
	throw new ASTException("unexpected sort: "+sort);
    }
    // write the count and the data
    count = uint32_encode(abuffer.getLength(),buffer);
    if(!write(count,buffer) != count)
        throw new ASTException("write failure");
    if(!write(abuffer.getLength(),abuffer.getBuffer()) != count)
        throw new ASTException("write failure");
}

/* Read Repeated field */
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

Byte[][]
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

} /*class Runtime*/



