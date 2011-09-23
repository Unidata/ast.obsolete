/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

// For code taken from google protobuf src:
//
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

/*
This generator generates a single .java file per proto file.
If a user has imports, then those import files
must be separately generated and compiled.

*/

package unidata.protobuf.ast.compiler;

import gnu.getopt.Getopt;

import java.util.*;
import java.io.*;

public class JavaGenerator extends Generator
{

//////////////////////////////////////////////////

static final String LANGUAGE = "J";

//////////////////////////////////////////////////

static final String DFALTDIR = ".";

static final String DIGITCHARS = "0123456789";
static final String IDCHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    + "abcdefghijklmnopqrstuvwxyz"
    + DIGITCHARS
    + "_$";

/* Protobuf identifiers are same as Java Identifiers */
static final String JCHARS = IDCHARS;

static final short IDMAX = 0x7f;
static final short IDMIN = ' ';

static final String LBRACE = "{";
static final String RBRACE = "}";

static final String[] JKeywords = new String[] {
"abstract", "assert", "boolean", "break",
"byte", "case", "catch", "char",
"class", "const", "continue", "default",
"do", "double", "else", "enum",
"extends", "final", "finally", "float",
"for", "goto", "if", "implements",
"import", "instanceof", "int", "interface",
"long", "native", "new", "package",
"private", "protected", "public", "return",
"short", "static", "strictfp", "super",
"switch", "synchronized", "this", "throw",
"throws", "transient", "try", "void",
"volatile", "while"
};


//////////////////////////////////////////////////
// Define the per-node extra info; grouped here into a single class.

static public class Annotation
{
    String fileprefix = null;
    String filebase = null;
    List<AST.File> imports = null;
    String javapackage = null;
}

//////////////////////////////////////////////////
// Misc. static functions

static boolean isPrimitive(AST.Field field)
{
    return (field.getType().getSort() == AST.Sort.PRIMITIVETYPE);
}

static AST.PrimitiveSort getPrimitiveSort(AST.Field field)
{
    if(!isPrimitive(field)) return null;
    return ((AST.PrimitiveType)(field.getType())).getPrimitiveSort();
}

static boolean isEnum(AST.Field field)
{
    return (field.getType().getSort() == AST.Sort.ENUM);
}

static boolean isMessage(AST.Field field)
{
    return (field.getType().getSort() == AST.Sort.MESSAGE);
}

static boolean isRequired(AST.Field field)
{
    return (field.getCardinality() == AST.Cardinality.REQUIRED);
}

static boolean isOptional(AST.Field field)
{
    return (field.getCardinality() == AST.Cardinality.OPTIONAL);
}

static boolean isRepeated(AST.Field field)
{
    return (field.getCardinality() == AST.Cardinality.REPEATED);
}


//////////////////////////////////////////////////
// Instance variables

String outputdir = null;

String filebase = null; // used also for the output file name

//////////////////////////////////////////////////
// Constructor

public
JavaGenerator()
{
}

//////////////////////////////////////////////////
// Command line processing

List<String>
processcommandline(String[] argv)
{
    int c;
    List<String> arglist = new ArrayList<String>();
    Getopt g = new Getopt(LANGUAGE+"Generator",argv,"-:o",null);
    while ((c = g.getopt()) != -1) {
	switch (c) {
	case 1: // intermixed non-option
	    arglist.add(g.getOptarg());
	    break;
	case ':':
	    System.err.println("Command line option requires argument "+g.getOptopt());
	    System.exit(1);
	case '?':
	    System.err.println("Illegal cmd line option: "+g.getOptopt());
	    System.exit(1);
	// True options start here
	case 'o':
	    String dir = g.getOptarg();
	    if(dir != null && dir.length() > 0) outputdir = dir;
	    break;
	default:
	    System.err.println("Unexpected getopt tag: "+c);
	    System.exit(1);
	}
    }
    return arglist;
}
//////////////////////////////////////////////////

/*
- compute which files will generate code
- compute the filename for the top package
- compute the reference path for each message and enum
- for each enum in code generating files
   - generate the top level enum definitions
   - generate the top level message classes
 - for each top-level generated message,
   generate the (de)serialize functions
   and the free function. Recurse for nested messages.
*/

public boolean
generate(AST.Root root, String[] argv) throws Exception
{
    List<String> arglist = processcommandline(argv);
    List<AST.File> codefiles = new ArrayList<AST.File>();
    AST.File topfile = root.getTopFile();

    // Assign annotation objects
    for(AST ast: root.getNodeSet()) {
	switch (ast.getSort()) {
	case PACKAGE: case FILE: case MESSAGE: case ENUM:
	    Annotation a = new Annotation();
	    ast.setAnnotation(a);
	    break;
	default: break;
	}
    }

    // Find files that will contribute code
    List<String> includes = new ArrayList<String>();
    // Topfile is always treated as compiled
    // prime the search
    codefiles.add(topfile);
    String tmp = (String)topfile.optionLookup("compile");
    if(tmp != null && tmp.length() > 0) {
        String[] compilefiles = tmp.split("[,]|[ \t]+");
        for(String jfile: compilefiles) {
	    // Locate the file
            for(AST.File file: root.getFileSet()) {
	        if(jfile.equals(file.getName())) {
		    if(!codefiles.contains(file))
		        codefiles.add(file);
		}
	    }
	}
    }

    // Compute the Java output file name
    String prefix = AuxFcns.getFilePrefix(topfile.getName());
    String basename = AuxFcns.getBaseName(topfile.getName());
    String jfilename = (String)topfile.optionLookup("java_file");
    if(jfilename != null) {
	if(!AuxFcns.getFilePrefix(jfilename).equals("")) {
	    prefix = AuxFcns.getFilePrefix(jfilename);
	}
        basename = AuxFcns.getBaseName(jfilename);
    } else {
	basename = AuxFcns.getBaseName(topfile.getName());
    }

    // Compute the Java package
    String jpackage = (String)topfile.optionLookup("java_package");

    // outputdir overrides any prefix
    if(outputdir != null) prefix = outputdir;
    if(prefix.length() == 0) prefix = ".";
    Annotation a = (Annotation)topfile.getAnnotation();
    a.filebase = basename;
    a.fileprefix = prefix;
    a.javapackage = jpackage;

    // Truncate .java file
    FileWriter filewriter = null;
    String filename = a.fileprefix + "/" + a.filebase;
    try {
	filewriter = new FileWriter(filename+".java");
    } catch (Exception e) {
	System.err.println("Cannot access file: "+filename+".java");
	return false;
    }
    // close the file to truncate
    try {
	filewriter.close();
    } catch (Exception e) {};

    // Generate the file <filebase>.java content
    Printer printer = null;
    FileWriter wfile = null;
    File file = null;

    try {
	// Open the output .java file
	file = new File(filename+".java");
	if(!file.canWrite()) {
	    System.err.println("Cannot access: "+file);
	    return false;
	}
	wfile = new FileWriter(file);
	printer = new Printer(wfile);
	generate_java(topfile,codefiles,printer);
	printer.close(); wfile.close();
    } catch (Exception e) {
	System.err.println("Generation Failure: "+file+":"+e);
	e.printStackTrace();
	return false;
    }
    return true;
} // generate()


static AST.File
matchfile(String fname, List<AST.File> files)
{
    for(AST.File f: files) {
	if(f.getName().equals(fname)) return f;
    }
    return null;
}

void
generate_java(AST.File topfile, List<AST.File> files, Printer printer)
	throws Exception
{
    Annotation a = (Annotation)topfile.getAnnotation();
    if(a.javapackage != null && a.javapackage.length() > 0)
        printer.printf("package %s\n",a.javapackage);
    printer.blankline();

    // Add imports

    // Start with unidata.protobuf.ast.Ast_Runtime
    printer.printf("import unidata.protobuf.ast.Ast_Runtime;");
    printer.printf("import static unidata.protobuf.ast.Ast_Runtime.*;");

    List<String> imports = new ArrayList<String>();
    for(AST.File f: files) {
        String optpackage = (String)f.optionLookup("java_package");
	if(optpackage == null || optpackage.length() == 0) continue;
	optpackage = optpackage.trim();
        if(!imports.contains(optpackage)) imports.add(optpackage);
    }

    // Generate the class header
    printer.printf("public class %s\n{\n",a.filebase);

    for(AST.File f: files) {
        // Generate the top-level enum definitions
        for(AST.Enum ast: f.getEnums()) {
	    generate_enum(ast,printer);
	}
    }

    // Generate the message structures as static nested classes
    // and any nested enums and messages
    for(AST.File f: files) {
        for(AST.Message ast: f.getMessages()) {
	    generate_messageclass(ast,printer);
	}
    }

    printer.blankline();
    // Generate the class trailer
    printer.printf("}\n");
}

void
generate_enum(AST.Enum e, Printer printer) throws Exception
{
    printer.blankline();
    Annotation a = (Annotation)e.getAnnotation();
    printer.blankline();
    printer.printf("static enum %s {\n",e.getName());
    printer.indent();
    List<AST.EnumValue> values = e.getEnumValues();
    int nvalues = values.size();
    for(int i=0;i<nvalues;i++) {
	AST.EnumValue eval = values.get(i);
	printer.printf("%s%s\n",
	    eval.getName(),
	    (i == (nvalues - 1)?";":","));
    }
    printer.outdent();
    printer.printf("} //enum %s\n",e.getName());
}

void
generate_messageclass(AST.Message msg, Printer printer) throws Exception
{
    // If the "declare" option is set, then do nothing
    if(AuxFcns.getbooleanvalue((String)msg.optionLookup("declare")))
	return;
    // See if any (java language) "extends" are specified
    String optextends = (String)msg.optionLookup("extends");
    String[] extendlist = null;
    if(optextends != null && optextends.trim().length() > 0) {
	optextends = optextends.trim();
	extendlist = optextends.split("[,]|[ \t]+");
    }
    // See if any "implements" are specified
    String optimplements = (String)msg.optionLookup("implements");
    String[] implementslist = null;
    if(optimplements != null && optimplements.trim().length() > 0) {
	optimplements = optimplements.trim();
	implementslist = optimplements.split("[,]|[ \t]+");
    }

    Annotation a = (Annotation)msg.getAnnotation();
    printer.blankline();
    // Generate the class header
    printer.printf("static public class %s",converttojname(msg.getName()));
    // Add extends and implements
    printer.printf(" extends unidata.protobuf.ast.AbstractMessage");	
    if(extendlist != null) {
        for(int i=0;i<extendlist.length;i++) {
	    String s = extendlist[i];
	    printer.printf("%s %s",(i==0?"":","),s.trim());
	}
    }
    if(implementslist != null) {
        printer.printf(" implements");	
        for(int i=0;i<implementslist.length;i++) {
	    String s = implementslist[i];
	    printer.printf("%s %s",(i==0?"":","),s.trim());
	}
    }
    printer.println("");
    printer.indent();

    // Generate any nested enums
    for(AST.Enum ast: msg.getEnums()) {
        generate_enum(ast,printer);
    }

    printer.blankline();

    // Generate any nested messages
    for(AST.Message ast: msg.getMessages()) {
        generate_messageclass(ast,printer);
    }

    printer.blankline();

    // Generate the fields
    for(AST.Field field: msg.getFields()) {
	if(isRequired(field) || isOptional(field)) {
	    printer.printf("%s %s;\n",
			    jtypefor(field.getType()),
			    jfieldvar(field));
	} else { // isRepeated(field)
	    printer.printf("%s[] %s;\n",
		    jtypefor(field.getType()),
		    jfieldvar(field));
	}
    }

    // Generate the class methods
    generate_constructor(msg,printer);
    generate_writefunction(msg,printer);
    generate_readfunction(msg,printer);
    generate_reclaimfunction(msg,printer);
    generate_sizefunction(msg,printer);

    // Generate the class trailer
    printer.outdent();
    printer.println("};\n");
}

void
generate_constructor(AST.Message msg, Printer printer)
    throws Exception
{
    printer.printf("public %s(Ast_runtime rt)\n",
		    jtypefor(msg), jmsgvar(msg));
    printer.println(LBRACE);
    printer.indent();
    printer.println("super(rt);");
    printer.outdent();
    printer.println(RBRACE);
}

void
generate_writefunction(AST.Message msg, Printer printer)
    throws Exception
{
    printer.printf("public void\nwrite()\n");
    printer.indent();
    printer.println("throws Ast_Exception");
    printer.outdent();
    printer.println(LBRACE);
    printer.indent();

    printer.blankline();
    // Generate the field serializations
    for(AST.Field field: msg.getFields()) {
	printer.indent();
	if(!isPrimitive(field) && !isEnum(field))
		printer.println("long size;");
	if(isRequired(field)) {
	    if(isPrimitive(field) || isEnum(field)) {
		printer.printf("write_primitive(%s,%d,&%s->%s);\n",
			       jtypesort(field.getType()),field.getId(),
			       jmsgvar(msg),jfieldvar(field));
	    } else if(isMessage(field)) {
		// Write the tag + count
		printer.printf("write_tag(Ast_counted,%d);\n",
				field.getId());
	        /* prefix msg serialization with encoded message size */
		printer.printf("size = %s.get_size(rt,%s.%s);\n",
			   jclassname(field.getType()),
                           jmsgvar(msg),jfieldvar(field));
		printer.println("status = write_size(size);");
		printer.printf("%s_write(%s.%s);\n",
			       jclassname(field.getType()),
			       jmsgvar(msg),jfieldvar(field));
	    } else throw new Exception("unknown field type");
	} else if(isOptional(field)) {
	    printer.printf("if(%s != null) "+LBRACE+"\n",
			    jmsgvar(msg),jfieldvar(field));
	    printer.indent();
	    if(isPrimitive(field) || isEnum(field)) {
		printer.printf("status = write_primitive(%s,%d,%s.%s);\n",
			       jtypesort(field.getType()), field.getId(),
			       jmsgvar(msg),jfieldvar(field));
	    } else if(isMessage(field)) {
		/* precede msg serialization with the tag */
		printer.printf("write_tag(Ast_counted,%d);\n",
				field.getId());
	        /* prefix msg serialization with encoded message size */
		printer.printf("size = %s.get_size(%s.%s);\n",
			   jclassname(field.getType()),jmsgvar(msg),jfieldvar(field));
		printer.println("write_size(size);");
		printer.printf("%s.write(%s.%s);\n",
			    jclassname(field.getType()),
			    jmsgvar(msg),jfieldvar(field));
	    } else throw new Exception("unknown field type");
	    printer.outdent();
	    printer.println(RBRACE);
	} else { // field.getCardinality() == AST.Cardinality.REPEATED
	    if(isPrimitive(field) || isEnum(field)) {
                /* Write the data */
		if(field.isPacked()) {
                    printer.printf("write_primitive_packed(%s,%d,%s.%s);\n",
                                   jtypesort(field.getType()),
                                   field.getId(),
                                   jmsgvar(msg),jfieldvar(field));
		} else {
	            printer.printf("for(int i=0;i<%s.%s.length;i++) "+LBRACE+"\n",
		    	           jmsgvar(msg),jfieldvar(field));
	            printer.indent();
		    printer.printf("write_primitive(%s,%d,%s.%s[i]);\n",
			       jtypesort(field.getType()), field.getId(),
			       jmsgvar(msg),jfieldvar(field));
	            printer.outdent();
		    printer.println(RBRACE);
		}
	    } else if(isMessage(field)) {
                printer.printf("for(int i=0;i<%s.%s.count;i++) "+LBRACE+"\n",
                               jmsgvar(msg),jfieldvar(field));
                printer.indent();
                /* precede msg serialization with the tag */
                printer.printf("write_tag(Ast_counted,%d);\n",
                                field.getId());
	        /* prefix msg serialization with encoded message size */
		printer.printf("size = %s.get_size(%s.%s.values[i]);\n",
			   jclassname(field.getType()),jmsgvar(msg),jfieldvar(field));
		printer.println("write_size(size);");
                printer.printf("%s.write(%s.%s.values[i]);\n",
                            jclassname(field.getType()),
                            jmsgvar(msg),jfieldvar(field));
                printer.outdent();
                printer.println(RBRACE);
	    } else throw new Exception("unknown field type");
	}
	printer.outdent();
	printer.println(RBRACE);
    }
    printer.outdent();
    printer.blankline();
    if(msg.getFields().size() > 0)
        printer.println("done:");
    printer.indent();
    printer.println("return;");
    printer.outdent();
    printer.blankline();
    printer.printf(RBRACE+" /*%s_write*/\n",msg.getName());
}

void
generate_readfunction(AST.Message msg, Printer printer)
    throws Exception
{
    printer.printf("public %s\nread()\n",jtypefor(msg));
    printer.indent();
    printer.println("throws Ast_Exception");
    printer.outdent();
    printer.println(LBRACE);
    printer.indent();
    printer.println("long[] wiretype = new long[1];\n");
    printer.println("long[] fieldno = new long[1];\n");
    printer.printf("%s %s = null;\n",jtypefor(msg),jmsgvar(msg));
    if(Main.optionTrace) {
	printer.println("long pos;");
    }
    // Create the target instance
    printer.blankline();
    printer.printf("%s = new %s(this.rt);\n",
		    jmsgvar(msg),jtypefor(msg));
    printer.printf("if(%s == null) throw new Ast_exception(AST_ENOMEM);\n",
                    jmsgvar(msg));
    printer.blankline();
    printer.println("for(;;) {");
    printer.indent();
    if(Main.optionTrace) {
	printer.println("pos = (long)xpos(this.rt);");
    }
    printer.println("if(!read_tag(wiretype,fieldno)) break;");
    if(Main.optionTrace) {
	printer.print("System.err.printf(");
	printer.printf("\"|%s|",msg.getName());
        printer.printf(": before=%%ld fieldno=%%ld wiretype=%%ld after=%%ld\\n\",");
	printer.println("pos,fieldno,wiretype,xpos(rt));");    
    }
    // Generate the field de-serializations
    printer.println("switch (fieldno) {");
    for(AST.Field field: msg.getFields()) {
	printer.printf("case %d: {\n",field.getId());
	printer.indent();
	if(isPrimitive(field)) {
	    generate_read_primitive(msg,field,field.isPacked(),printer);
	} else if(isEnum(field)) {
	    generate_read_enum(msg,field,field.isPacked(),printer);
	} else {
	    // Generate needed local variables
	    if(!isPrimitive(field)) {
	        printer.println("long size;");
	    }
	    if(isRepeated(field))
	        printer.printf("%s tmp;\n",jtypefor(field.getType()));
	    // Verify that the wiretype == Ast_counted
	    printer.println("if(wiretype != Ast_counted)");
	    printer.indent();
	    printer.println("throw new Ast_exception(AST_EFAIL);");
	    printer.outdent();
	    // Read an instance
	    generate_read_message(msg,field,printer);
	}
	printer.println("} break;");
	printer.outdent();
    }
    // add default
    printer.println("default:");
    printer.indent();
    printer.println("skip_field(wiretype,fieldno);");
    printer.outdent();
    printer.println("} /*switch*/"); // switch
    printer.outdent();
    printer.println("}/*for*/"); // for(;;)
    // Generate defaults for primitive typed optionals
    for(AST.Field field: msg.getFields()) {
	if(isOptional(field)
	   && field.getType().getSort() == AST.Sort.PRIMITIVETYPE) {
	    generate_read_default_primitive(msg,field,printer);
	}
    }
    // return result
    printer.printf("return %s;\n",jmsgvar(msg));
    printer.outdent();
    printer.printf("} /*%s_read*/\n",msg.getName());
}

void
generate_mark(Printer printer) throws IOException
{
    // Get the count and mark the input
    printer.println("size = read_size();");
    printer.println("mark(size);");
}

void
generate_unmark(Printer printer) throws IOException
{
    printer.println("unmark();");
}

void
generate_read_primitive(AST.Message msg, AST.Field field, boolean ispacked, Printer printer)
    throws Exception
{
    AST.PrimitiveSort psort = getPrimitiveSort(field);

    switch (field.getCardinality()) {

    case REQUIRED:
        switch (psort) {
        case STRING: case BYTES:
	default: break;
	}
	printer.printf("read_primitive(%s,%d,&%s.%s);\n",
			       jtypesort(field.getType()),
			       field.getId(),
			       jmsgvar(msg),jfieldvar(field));
	break;

    case OPTIONAL:
        printer.printf("%s.%s = 1;\n",
                        jmsgvar(msg),jfieldvar(field));
        switch (psort) {
        case STRING:
            printer.printf("%s.%s = null;\n", jmsgvar(msg),jfieldvar(field));
            break;
        case BYTES:
            printer.printf("%s.%s = null;\n", jmsgvar(msg),jfieldvar(field));
            break;
        default:
            printer.printf("%s.%s = 0;\n", jmsgvar(msg),jfieldvar(field));
            break;
        }
        printer.printf("read_primitive(%s,%d,&%s.%s.value);\n",
                           jtypesort(field.getType()),
			   field.getId(),
                           jmsgvar(msg),jfieldvar(field));
        break;

    case REPEATED:
	if(ispacked) {
            printer.printf("read_primitive_packed(%s,%d,&%s.%s);\n",
                                jtypesort(field.getType()),
				field.getId(),
                                jmsgvar(msg),jfieldvar(field));;
	} else {
            printer.printf("%s tmp;\n",jtypefor(field.getType()));
            printer.printf("read_primitive(%s,%d,&tmp);\n",
                                jtypesort(field.getType()),field.getId());
	    printer.printf("tmp = repeat_append(%s,%s.%s,tmp);\n",
                            jtypesort(field.getType()),
                            jmsgvar(msg),jfieldvar(field));
	}
        break;
    }
}

void
generate_read_enum(AST.Message msg, AST.Field field, boolean ispacked, Printer printer)
    throws Exception
{
    switch (field.getCardinality()) {

    case REQUIRED:
	printer.printf("%s.%s = read_primitive(%s,%d);\n",
			       jtypesort(field.getType()),
  			       jfieldvar(field),
			       field.getId(),
			       jmsgvar(msg));
	break;

    case OPTIONAL:
        printer.printf("%s.%s = read_primitive(%s,%d);\n",
                           jtypesort(field.getType()),
	  	  	   jfieldvar(field),
			   field.getId(),
                           jmsgvar(msg));
        break;

    case REPEATED:
	if(ispacked) {
            printer.printf("s.%s = read_primitive_packed(%s,%d);\n",
                                jtypesort(field.getType()),
				jfieldvar(field),
				field.getId(),
                                jmsgvar(msg));
	} else {
            printer.printf("%s tmp = null;\n",jtypefor(field.getType()));
            printer.printf("tmp = read_primitive(%s,%d);\n",
                                jtypesort(field.getType()),field.getId());
	    printer.printf("tmp = repeat_append(%s,%s.%s);\n",
                            jtypesort(field.getType()),
                            jmsgvar(msg),jfieldvar(field));
	}
        break;
    }
}

void
generate_read_message(AST.Message msg, AST.Field field, Printer printer)
    throws Exception
{
    // Pick up the prefixed length and mark rt
    generate_mark(printer);

    switch (field.getCardinality()) {
    case REQUIRED:
        printer.printf("%s.%s = %s.read();\n",
			    jmsgvar(msg),jfieldvar(field),
			    jclassname(field.getType()));
	break;

    case OPTIONAL:
	printer.printf("%s.%s = null;\n",
			    jmsgvar(msg),jfieldvar(field));
	printer.printf("%s.%s = %s_read(&%s.%s.value);\n",
			    jmsgvar(msg),jfieldvar(field),
			    jclassname(field.getType()));
        break;

    case REPEATED:
	printer.printf("tmp = %s.read();\n",jtypefor(field.getType()));
        printer.printf("tmp = repeat_append(%s,%s.%s,tmp);\n",
				jtypesort(field.getType()),
				jmsgvar(msg),jfieldvar(field));
    }

    // Unmark
    generate_unmark(printer);
}

void
generate_read_default_primitive(AST.Message msg, AST.Field field, Printer printer)
    throws Exception
{
    AST.PrimitiveSort psort = ((AST.PrimitiveType)(field.getType())).getPrimitiveSort();
    String field_default = defaultfor(field);

    if(field_default == null) {
        switch (psort) {
	case STRING: field_default = null; break;
	case BYTES: field_default = null; break;
	case BOOL: field_default = "true"; break;
        default: field_default = "0"; break;
        }
    } 

    // Do some conversions for some types
    switch (psort) {
    case BOOL: {
        if("1".equalsIgnoreCase(field_default.toString()))
	    field_default = "true";
        else if("0".equalsIgnoreCase(field_default.toString()))
	    field_default = "false";
    } break;
    case STRING:
        if(field_default != null)
	    field_default = '"' + AuxFcns.escapify(field_default,'"',
				      AuxFcns.EscapeMode.EMODE_C) + '"';
     break;
    default: break;
    }

    printer.printf("if(%s.%s == null) {\n",
                        jmsgvar(msg),jfieldvar(field));
    printer.indent();
    switch (psort) {
    case STRING:
        printer.printf("%s.%s = %s;\n",
                        jmsgvar(msg),jfieldvar(field),
			(field_default==null?"null":field_default)
			);
	break;
    case BYTES:
        printer.printf("%s.%s = %d;\n",
                        jmsgvar(msg),jfieldvar(field),
			(field_default == null?"null":field_default.length()/2)
			);
        printer.printf("%s.%s = %s;\n",
                        jmsgvar(msg),jfieldvar(field),
			(field_default==null?"null":field_default)
			);
	break;
    default:
        printer.printf("%s.%s = %s;\n",
                        jmsgvar(msg),jfieldvar(field),field_default);
	break;
    }
    printer.outdent();
    printer.println("}");
}

void generate_reclaimfunction(AST.Message msg, Printer printer)
    throws Exception
{
    printer.printf("public void\nreclaim()\n");
    printer.println(LBRACE);
    printer.indent();

    // Generate the field reclaims
    for(AST.Field field: msg.getFields()) {
        AST.Sort sort = field.getType().getSort();
        AST.PrimitiveSort psort = getPrimitiveSort(field);
	printer.println(LBRACE);
	printer.indent();

        if(sort == AST.Sort.MESSAGE) {
	    // reclaim the submessage
	    printer.printf("%s.%s.reclaim();\n",
			       jmsgvar(msg),jfieldvar(field));
	}	
        printer.printf("%s.%s = null;");
    }
    printer.outdent();
    printer.printf("} /*%s_reclaim*/\n",msg.getName());
}


void generate_sizefunction(AST.Message msg, Printer printer)
    throws Exception
{
    printer.printf("long\n%get_size()\n");
    printer.println("{");
    printer.indent();
    printer.println("long totalsize = 0;");
    if(msg.getFields().size() > 0)
        printer.println("long fieldsize = 0;");
    printer.blankline();

    // sum the field sizes; make sure to include the tag if not packed
    for(AST.Field field: msg.getFields()) {
	switch (field.getType().getSort()) {
	case MESSAGE: break;
	case ENUM: break;
	case PRIMITIVETYPE: break;
	default: continue;
	}

	printer.println(LBRACE);
	printer.indent();

	if(isRequired(field)) {
	    if(isPrimitive(field) || isEnum(field)) {
		// Add in the prefix tag size
		printer.printf("fieldsize += get_tagsize(%s,%d);\n",
			(field.isPacked()?jtypesort(field.getType()):"Ast_counted"),
			field.getId());
		printer.printf("fieldsize += get_size(%s,&%s.%s);\n",
			       jtypesort(field.getType()),
			       jmsgvar(msg),jfieldvar(field));
	    } else if(isMessage(field)) {
		printer.printf("fieldsize += %s.%s.get_size();\n",
			    jmsgvar(msg),jfieldvar(field));
		// Add in the prefix tag size
		printer.printf("fieldsize += get_tagsize(%s,%d);\n",
			(field.isPacked()?jtypesort(field.getType()):"Ast_counted"),
			field.getId());
		// Add in the prefix count
		printer.println("fieldsize += get_size(Ast_uint32,fieldsize);");
	    } else throw new Exception("unknown field type");
	} else if(isOptional(field)) {
	    printer.printf("if(%s.%s != null) {\n",
			    jmsgvar(msg),jfieldvar(field));
	    printer.indent();
	    if(isPrimitive(field) || isEnum(field)) {
		// Add in the prefix tag size
		printer.printf("fieldsize += get_tagsize(%s,%d);\n",
			(field.isPacked()?jtypesort(field.getType()):"Ast_counted"),
			field.getId());
		printer.printf("fieldsize += get_size(%s,%s.%s);\n",
			       jtypesort(field.getType()),
			       jmsgvar(msg),jfieldvar(field));
	    } else if(isMessage(field)) {
		printer.printf("fieldsize += %s.get_size(%s.%s);\n",
			    jclassname(field.getType()),
			    jmsgvar(msg),jfieldvar(field));
		// Add in the prefix count
		printer.println("fieldsize += get_size(Ast_uint32,fieldsize);");
		// Add in the prefix tag size
		printer.printf("fieldsize += get_tagsize(%s,%d);\n",
			(field.isPacked()?jtypesort(field.getType()):"Ast_counted"),
			field.getId());
	    } else throw new Exception("unknown field type");
	    printer.outdent();
	    printer.printf("}\n");
	} else { // field.getCardinality() == AST.Cardinality.REPEATED
	    printer.printf("for(int i=0;i<%s.%s.length;i++) {\n",
			    jmsgvar(msg),jfieldvar(field));
	    printer.indent();
	    if(isPrimitive(field) || isEnum(field)) {
		// Add in the prefix tag size
		printer.printf("fieldsize += get_tagsize(%s,%d);\n",
			(field.isPacked()?jtypesort(field.getType()):"Ast_counted"),
			field.getId());
		printer.printf("fieldsize += get_size(%s,%s.%s[i]);\n",
			       jtypesort(field.getType()),
			       jmsgvar(msg),jfieldvar(field));
	    } else if(isMessage(field)) {
		printer.printf("fieldsize += %s.get_size(%s.%s[i]);\n",
			    jclassname(field.getType()),
			    jmsgvar(msg),jfieldvar(field));
		// Add in the prefix count
		printer.println("fieldsize += get_size(Ast_uint32,fieldsize);");
		// Add in the prefix tag size
		printer.printf("fieldsize += get_tagsize(%s,%d);\n",
			(field.isPacked()?jtypesort(field.getType()):"Ast_counted"),
			field.getId());
	    } else throw new Exception("unknown field type");
	    printer.outdent();
	    printer.println(RBRACE);
	}
        printer.println("totalsize += fieldsize;");
    }

    printer.println("return totalsize;");
    printer.outdent();
    printer.blankline();
    printer.printf("} /*%s.get_size*/\n",msg.getName());
    printer.blankline();
}


//////////////////////////////////////////////////

// Convert a msg name to an acceptable Java variable name
String
jmsgvar(AST.Message msg)
{
    String jname = msg.getName().toLowerCase() + "_v";
    return jname;
}

// Convert a field name to an acceptable Java variable name
String
jfieldvar(AST.Field field)
{
    String jname = converttojname(field.getName());
    return jname;
}

String
converttojname(String name)
{
    /* Java and protobuf identifiers are same,
       except we ned to rename Java keywords
    */
    if(Arrays.binarySearch((Object[])JKeywords,(Object)name) >= 0)
	name = name + "_";
    return name;
}

String
jclassname(AST.Type asttype)
    throws Exception
{
    String typename = null;
    if(asttype.getSort() == AST.Sort.ENUM
	      || asttype.getSort() == AST.Sort.MESSAGE) {
	typename = asttype.getName();
    } else { // Illegal
	throw new Exception("jclassname: Illegal type: "+asttype.getName());
    }
    return converttojname(typename);
}

String
jtypefor(AST.Type asttype)
{
    String typ = null;

    if(asttype.getSort() == AST.Sort.PRIMITIVETYPE) {
	switch (((AST.PrimitiveType)asttype).getPrimitiveSort()) {
	case SINT32:
	case SFIXED32:
	case INT32:   typ = "Integer"; break;

	case FIXED32:
	case UINT32:   typ = "Long"; break;

	case SINT64:
	case SFIXED64:
	case INT64:   typ = "Long"; break;

	case FIXED64:
	case UINT64:   typ = "Long"; break;

	case FLOAT:   typ = "Float"; break;
	case DOUBLE:  typ = "Double"; break;

	case BOOL:    typ = "Boolean"; break;
	case STRING:  typ = "String"; break;

	case BYTES:   typ = "Byte[]"; break;
	// No default because we want the compiler to complain if any new
	// types are added.
	}
    } else if(asttype.getSort() == AST.Sort.ENUM
	      || asttype.getSort() == AST.Sort.MESSAGE) {
	typ = asttype.getName();
    } else { // Illegal
	System.err.println("Cannot translate type to Java Type: "+asttype.getName());

    }
    return typ;
}

String
defaultfor(AST.Field field)
{
    AST.Type fieldtype = field.getType();
    if(isRepeated(field)) {
	// repeated default is always a list of the type of the field
	return String.format("new ArrayList<%s>()",jtypefor(fieldtype));
    } else {
	// See if the field has a defined default
	Object value = field.optionLookup("DEFAULT");
	if(value == null) return null;
	if(fieldtype.getSort() == AST.Sort.PRIMITIVETYPE) {
	    return (String)value;
	} else if(fieldtype.getSort() == AST.Sort.ENUM) {
	    return (String)value;
	} else if(fieldtype.getSort() == AST.Sort.MESSAGE) {
	    return String.format("%s.getDefaultInstance()",
				    fieldtype.getName());
	}
    }
    return null;
}


String
jtypesort(AST.Type asttype)
{
    String sort = null;
    if(asttype.getSort() == AST.Sort.PRIMITIVETYPE) {
	switch (((AST.PrimitiveType)asttype).getPrimitiveSort()) {
	case SINT32: return "Ast_sint32";
	case SFIXED32: return "Ast_sfixed32";
	case UINT32: return "Ast_uint32";
	case FIXED32: return "Ast_fixed32";
	case INT32: return "Ast_int32";
	case SINT64: return "Ast_sint64";
	case SFIXED64: return "Ast_sfixed64";
	case UINT64: return "Ast_uint64";
	case FIXED64: return "Ast_fixed64";
	case INT64: return "Ast_int64";
	case FLOAT: return "Ast_float";
	case DOUBLE: return "Ast_double";
	case BOOL: return "Ast_bool";
	case STRING: return "Ast_string";
	case BYTES: return "Ast_bytes";
	// No default because we want the compiler to complain if any new
	// types are added.
	}
    } else if(asttype.getSort() == AST.Sort.ENUM) {
        return "Ast_enum";
    } else if(asttype.getSort() == AST.Sort.MESSAGE) {
        return "Ast_message";
    } else {
	System.err.println("Cannot translate type to Java sort:" + asttype.getSort().toString());
    }
    return null;
}


} // JGenerator


