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

import static unidata.ast.runtime.Internal.*;

public class ASTRuntime
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

    static public final int MAXTYPESIZE = 16;
    static public final int MAXVARINTSIZE = 10;

    static public int javasize(int sort)
    {
	switch (sort) {
        case Ast_float: case Ast_fixed32: case Ast_sfixed32: case Ast_enum: return 4;
        case Ast_double: case Ast_fixed64: case Ast_sfixed64: return 8;
        case Ast_bool: return 1;
	default: break;
	}
	return 0;
    }

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

byte[] valuebuffer = new byte[Sort.MAXTYPESIZE]; // size is max needed except for string|bytes|packed
                                                 // used to encode single primitive values
byte[] sizebuffer = new byte[Sort.MAXTYPESIZE]; // used to encode tags and sizes.

ByteBuffer varintbuffer = new ByteBuffer(); // for writing packed data that is encoded as varints

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
    switch (Sort.wiretype(sort)) {
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
    switch (Sort.wiretype(sort)) {
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
/* Procedure to calulate size of a tag */
int
getTagSize(int sort, int fieldno)
{
    int wiretype = Sort.wiretype(sort);
    int count = encode_tag(wiretype,fieldno, sizebuffer);
    return count;
}

/* Procedures to read/write tags */
void
write_tag(int wiretype, int fieldno)
    throws IOException
{
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

int
readvarint(byte[] buffer)
    throws IOException
{
    for(int i=0;i<buffer.length;i++) {
	if(!read(buffer, i,1))
            return -1; // eof
	if((0x80 & buffer[i]) == 0) return i+1;
    }
    return 0;
}

/* Based on the wiretype, extract the proper number of bytes
   for an integer base value; return the length
   and place the bytes into the valuebuffer.
   For ast counted, do this for the count,
   not the content.
*/

int
readandcount(int wiretype, byte[] buffer)
    throws IOException
{
    int len = 0;
    int count;
    switch (wiretype) {
    case Wiretype.Ast_varint:
        count = readvarint(buffer);
	break;
    case Wiretype.Ast_32bit:
	count = 4;
        if(!read(buffer, 0,4)) count = -1;
	break;
    case Wiretype.Ast_64bit:
	count = 8;
        if(!read(buffer, 0,8)) count = -1;
	break;
    case Wiretype.Ast_counted: /* get the count */
        len = readvarint(buffer);
	count = uint32_decode(len,buffer);
	break;
    default:
	throw new ASTException("Unexpected wiretype: "+wiretype);
    }
    return count;
}

//////////////////////////////////////////////////

double
read_primitive_double(int sort)
    throws IOException
{
    assert(sort == Sort.Ast_double);
    int wiretype = Sort.wiretype(sort);
    int len = readandcount(wiretype, valuebuffer);
    double value = float64_decode(Sort.javasize(Sort.Ast_double), valuebuffer);
    return value;
}

float
read_primitive_float(int sort)
    throws IOException
{
    assert(sort == Sort.Ast_float);
    int wiretype = Sort.wiretype(sort);
    long len = readandcount(wiretype, valuebuffer);
    float value = float32_decode(Sort.javasize(Sort.Ast_float), valuebuffer);
    return value;
}

boolean
read_primitive_boolean(int sort)
    throws IOException
{
    assert(sort == Sort.Ast_bool);
    int wiretype = Sort.wiretype(sort);
    int len = readandcount(wiretype, valuebuffer);
    boolean value = bool_decode(len, valuebuffer);
    return value;
}

String
read_primitive_string(int sort)
    throws IOException
{
    assert(sort == Sort.Ast_string);
    int wiretype = Sort.wiretype(sort);
    int len = readandcount(wiretype, valuebuffer);
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
    int wiretype = Sort.wiretype(sort);
    int len = readandcount(wiretype, valuebuffer);
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
    int wiretype = Sort.wiretype(sort);
    int len = readandcount(wiretype, valuebuffer);
    switch (sort) {
    case Sort.Ast_enum: /* fall thru */
    case Sort.Ast_int32:
        return int32_decode(len, valuebuffer);
    case Sort.Ast_uint32:
        return uint32_decode(len, valuebuffer);
    case Sort.Ast_sint32:
        return unzigzag32(uint32_decode(len, valuebuffer));
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
    int wiretype = Sort.wiretype(sort);
    int len = readandcount(wiretype, valuebuffer);
    switch (sort) {
    case Sort.Ast_int64:
        return int64_decode(len, valuebuffer);
    case Sort.Ast_uint64:
        return uint64_decode(len, valuebuffer);
    case Sort.Ast_sint64:
        return unzigzag64(uint64_decode(len, valuebuffer));
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
    int size = readandcount(wiretype, valuebuffer);
    int sizeof = Sort.javasize(sort);

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
    int size = readandcount(wiretype, valuebuffer);
    int sizeof = Sort.javasize(sort);

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
    int size = readandcount(wiretype, valuebuffer);
    int sizeof = Sort.javasize(sort);

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
    int size = readandcount(Wiretype.Ast_counted, valuebuffer);
    int len = 0;
    Ibuffer intdata = new Ibuffer();


// i.e. sort= Ast_(u)int32,Ast_sint32,Ast_fixed32,Ast_sfixed32
loop: for(int avail=size;avail > 0;) {
	switch (Sort.wiretype(sort)) {
        case Wiretype.Ast_varint:
            len = readvarint(valuebuffer);
	    if(len < 0) break loop;
            avail -= len;
	    break;
        case Wiretype.Ast_32bit:
            len = readandcount(Wiretype.Ast_32bit, valuebuffer);
	    if(len < 0) break loop;
            avail -= len;
	    break;
        default:
	    throw new ASTException("illegal wiretype: " + Sort.wiretype(sort));
        }
        switch (sort) {
        case Sort.Ast_int32:
	    intdata.add(int32_decode(len, valuebuffer));
	    break;			
        case Sort.Ast_uint32:
	    intdata.add(uint32_decode(len, valuebuffer));
	    break;			
        case Sort.Ast_sint32:
	    intdata.add(unzigzag32(uint32_decode(len, valuebuffer)));
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
    int count = readandcount(Wiretype.Ast_counted, valuebuffer);
    int size = Sort.javasize(sort); // 0 => unknown => varint
    int len = 0;
    Lbuffer longdata = new Lbuffer();

// i.e. sort= Ast_(u)int64,Ast_sint64,Ast_fixed64,Ast_sfixed64
loop: for(int avail=size;avail > 0;) {
	switch (Sort.wiretype(sort)) {
        case Wiretype.Ast_varint:
            len = readvarint(valuebuffer);
	    if(len < 0) break loop;
            avail -= len;
	    break;
        case Wiretype.Ast_64bit:
            len = readandcount(Wiretype.Ast_64bit, valuebuffer);
	    if(len < 0) break loop;
            avail -= len;
	    break;
        default:
	    throw new ASTException("illegal wiretype: " + Sort.wiretype(sort));
        }

        switch (sort) {
        case Sort.Ast_int64:
	    longdata.add(int64_decode(len, valuebuffer));
	    break;			
        case Sort.Ast_uint64:
	    longdata.add(uint64_decode(len, valuebuffer));
	    break;			
        case Sort.Ast_sint64:
	    longdata.add(unzigzag64(uint64_decode(len, valuebuffer)));
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

} /*class ASTRuntime*/


