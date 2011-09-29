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

package unidata.ast.compiler.generators.java;

import unidata.ast.compiler.*;
import gnu.getopt.Getopt;

import java.util.*;
import java.io.*;

public class JavaGenerator extends unidata.ast.compiler.Generator
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
// Constructor(s)

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
    if(jpackage == null)
	jpackage = (String)topfile.optionLookup("package"); // alternate name

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

    // Start with unidata.ast.Ast_Runtime
    printer.println("import unidata.ast.runtime.*;");
    printer.println("import static unidata.ast.Ast_Runtime.*;");
    printer.blankline();

    List<String> imports = new ArrayList<String>();
    for(AST.File f: files) {
        String optpackage = (String)f.optionLookup("java_package");
	if(optpackage == null || optpackage.length() == 0) continue;
	optpackage = optpackage.trim();
        if(!imports.contains(optpackage)) imports.add(optpackage);
    }
    printer.blankline();

    // Generate the class header
    printer.printf("public class\n%s\n{\n",a.filebase);

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
    Annotation a = (Annotation)e.getAnnotation();
    printer.printf("static public enum %s {\n",e.getName());
    printer.indent();
    List<AST.EnumValue> values = e.getEnumValues();
    int nvalues = values.size();
    for(int i=0;i<nvalues;i++) {
	AST.EnumValue eval = values.get(i);
	printer.printf("%s(%s)%s\n",
	    eval.getName(),
	    eval.getValue(),
	    (i == (nvalues - 1)?";":","));
    }
    // add value field, the getter, and the constructor
    printer.println("private final int value;");
    printer.println("public int getValue() {return value;}");
    printer.printf("%s(int value) {this.value = value;};\n",e.getName());
    // add converter from integer to enum constant
    printer.printf("static %s toEnum(int i)",e.getName());
    printer.println(LBRACE);
    printer.indent();
    printer.println("switch (i) "+LBRACE);
    for(int i=0;i<nvalues;i++) {
	AST.EnumValue eval = values.get(i);
	printer.printf("case %s: return %s.%s;\n",
		eval.getValue(),	
		e.getName(),
	        eval.getName());
    }
    printer.println("default: return null;");
    printer.outdent();
    printer.println("}");

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
    printer.printf("static public class\n%s",converttojname(msg.getName()));
    // Add extends and implements
    if(extendlist != null) {
        printer.printf(" extends ");	
        for(int i=0;i<extendlist.length;i++) {
	    String s = extendlist[i];
	    printer.printf("%s %s",(i==0?"":","),s.trim());
	}
    }
    if(extendlist != null) {
        printer.printf(" implements ");	
        for(int i=0;i<implementslist.length;i++) {
            String s = implementslist[i];
	    printer.printf(", %s",s.trim());
	}
    }
    printer.println("");
    printer.println("{");
    printer.indent();

    // Generate any nested enums
    if(msg.getEnums().size() > 0) {
        for(AST.Enum ast: msg.getEnums()) {
	    generate_enum(ast,printer);
	}
        printer.blankline();
    }


    // Generate any nested messages
    if(msg.getMessages().size() > 0) {
        for(AST.Message ast: msg.getMessages()) {
            generate_messageclass(ast,printer);
	}
        printer.blankline();
    }

    // Generate the fields
    if(msg.getFields().size() > 0) {
        for(AST.Field field: msg.getFields()) {
            if(isRequired(field)) {
                printer.printf("%s %s = %s;\n",
                                jtypefor(field.getType()),
                                jfieldvar(field),
                                defaultfor(field));
            } else if(isOptional(field)) {
                printer.printf("%s %s = null;\n",
                                jobjecttypefor(field.getType()),
                                jfieldvar(field));
            } else { // isRepeated(field)
                printer.printf("%s[] %s = null;\n",
                        jtypefor(field.getType()),
                        jfieldvar(field));
            }
	}
	printer.blankline();
    }

    // Generate the class methods
    generate_constructors(msg,printer);
    printer.blankline();
    generate_writefunction(msg,printer);
    printer.blankline();
    generate_readfunction(msg,printer);
    printer.blankline();
    generate_sizefunction(msg,printer);

    // Generate the class trailer
    printer.outdent();
    printer.println("};\n");
}

void
generate_constructors(AST.Message msg, Printer printer)
    throws Exception
{
    // First, generate the simple constructor
    printer.printf("public %s(ASTRuntime rt)\n",jtypefor(msg));
    printer.println(LBRACE);
    printer.indent();
    printer.println("super(rt);");
    printer.outdent();
    printer.println(RBRACE);
    printer.blankline();

    // Generate the big constructor
    String s = String.format("public %s(",jtypefor(msg));
    printer.println(s+"ASTRuntime rt,");
    // add field decls
    int dent = printer.getIndent();
    printer.indentTo(s.length());
    int len = msg.getFields().size();
    for(int i=0;i<len;i++) {
        AST.Field field =  msg.getFields().get(i);
        printer.printf("%s%s %s%s\n",
			    (isOptional(field)?jobjecttypefor(field.getType())
					      :jtypefor(field.getType())),
			    (isRequired(field)?"":"[]"),
			    jfieldvar(field),
			    (i==len-1?"":","));
    }
    printer.println(")");
    printer.indentTo(dent);
    printer.println(LBRACE);
    printer.indent();
    printer.println("super(rt);");
    // assign args to fields
    for(AST.Field field: msg.getFields()) {
        printer.printf("this.%s = %s;\n",
			    jfieldvar(field),
			    jfieldvar(field));
    }
    printer.outdent();
    printer.println(RBRACE);
}

//////////////////////////////////////////////////
void
generate_writefunction(AST.Message msg, Printer printer)
    throws Exception
{
    printer.println("public void");
    printer.println("write()");
    printer.indent();
    printer.println("throws ASTException");
    printer.outdent();
    printer.println(LBRACE);
    printer.indent();

    // Generate the field serializations
    for(AST.Field field: msg.getFields()) {
	if(!isPrimitive(field) && !isEnum(field))
		printer.println("long size;");
	if(isRequired(field)) {
	    if(isPrimitive(field)) {
		printer.printf("write_tag(%s,%d);\n",
			       jfieldsort(field),field.getId());
		printer.printf("write_primitive(%s,%s);\n",
			       jfieldsort(field),
			       jfieldvar(field));
	    } else if(isEnum(field)) {
		printer.printf("write_tag(%s,%d);\n",
			       jfieldsort(field),field.getId());
		printer.printf("write_primitive(%s,%s.getValue());\n",
			       jfieldsort(field),
			       jfieldvar(field));
	    } else if(isMessage(field)) {
		// Write the tag + count
		printer.printf("write_tag(Ast_counted,%d);\n",
				field.getId());
	        /* prefix msg serialization with encoded message size */
		printer.printf("size = %s.getSize();\n",
			       jfieldvar(field));
		printer.println("write_size(size);");
		printer.printf("%s.write();\n",
			       jfieldvar(field));
	    } else throw new Exception("unknown field type");
	} else if(isOptional(field)) {
	    printer.printf("if(%s != null) "+LBRACE+"\n",
			    jfieldvar(field));
	    printer.indent();
	    if(isPrimitive(field)) {
		printer.printf("write_tag(%s,%d);\n",
			       jfieldsort(field),field.getId());
		printer.printf("write_primitive(%s,%s);\n",
			       jfieldsort(field),
			       jfieldvar(field));
	    } else if(isEnum(field)) {
		printer.printf("write_tag(%s,%d);\n",
			       jfieldsort(field),field.getId());
		printer.printf("write_primitive(%s,%s.getValue());\n",
			       jfieldsort(field),
			       jfieldvar(field));
	    } else if(isMessage(field)) {
		/* precede msg serialization with the tag */
		printer.printf("write_tag(Ast_counted,%d);\n",
				field.getId());
	        /* prefix msg serialization with encoded message size */
		printer.printf("size = %s.getSize();\n",
			       jfieldvar(field));
		printer.println("write_size(size);");
		printer.printf("%s.write();\n",
			        jfieldvar(field));
	    } else throw new Exception("unknown field type");
	    printer.outdent();
	    printer.println(RBRACE);
	} else { // field.getCardinality() == AST.Cardinality.REPEATED
	    if(isPrimitive(field) || isEnum(field)) {
                /* Write the data */
		if(field.isPacked()) {
		    printer.printf("write_tag(Ast_counted,%d);\n",
			           field.getId());
		    printer.printf("size = getSize(%s);\n",
			           jfieldvar(field));
		    printer.println("write_size(size);");
                    printer.printf("write_primitive_packed(%s,%s);\n",
                                   jfieldsort(field),
                                   (isEnum(field)?jfieldvar(field)+".getValue()"
				                 :jfieldvar(field))
				   );
		} else {
	            printer.printf("for(int i=0;i<%s.length;i++) "+LBRACE+"\n",
		    	           jfieldvar(field));
	            printer.indent();
		    printer.printf("write_tag(%s,%d);\n",
			           jfieldsort(field),field.getId());
		    printer.printf("write_primitive(%s,%s[i]);\n",
			       jfieldsort(field),
			       jfieldvar(field));
	            printer.outdent();
		    printer.println(RBRACE);
		}
	    } else if(isMessage(field)) {
	        /* prefix msg serialization with encoded tag and message size*/
                printer.printf("for(int i=0;i<%s.count;i++) "+LBRACE+"\n",
                               jfieldvar(field));
                printer.indent();
                /* precede msg serialization with the tag and count*/
                printer.printf("write_tag(Ast_counted,%d);\n",
                                field.getId());
		printer.printf("size = %s.getSize();\n",
			       jfieldvar(field));
		printer.println("write_size(size);");
                printer.printf("%s[i].write();\n",
                            jfieldvar(field));
                printer.outdent();
                printer.println(RBRACE);
	    } else throw new Exception("unknown field type");
	}
    }
    printer.outdent();
    printer.blankline();
    if(msg.getFields().size() > 0)
        printer.println("done:");
    printer.indent();
    printer.println("return this;");
    printer.outdent();
    printer.blankline();
    printer.printf(RBRACE+" /*%s_write*/\n",msg.getName());
}

void
generate_readfunction(AST.Message msg, Printer printer)
    throws Exception
{
    printer.println("public void");
    printer.println("read()");
    printer.indent();
    printer.println("throws ASTException");
    printer.outdent();
    printer.println(LBRACE);
    printer.indent();
    // wiretype and fieldno are fake call by ref.
    printer.println("int[] wiretype = new int[1];");
    printer.println("int[] fieldno = new int[1];");
    if(Main.getOptionTrace()) {
	printer.println("int pos;");
    }
    printer.blankline();
    printer.println("for(;;) {");
    printer.indent();

    printer.println("if(!read_tag(wiretype,fieldno)) break;");
    if(Main.getOptionTrace()) {
/*
	printer.print("System.err.printf(");
	printer.printf("\"|%s|",msg.getName());
        printer.printf(": before=%%d fiedno=%%d wiretype=%%d after=%%d\\n\",");
	printer.println("pos,fieldno,wiretype,xpos(rt));");    
*/
    }
    // Generate the field de-serializations
    printer.println("switch (fieldno[0]) {");
    for(AST.Field field: msg.getFields()) {
	printer.printf("case %d: { // %s\n",field.getId(),jfieldvar(field));
	printer.indent();
	if(isPrimitive(field)) {
	    generate_read_primitive(msg,field,field.isPacked(),printer);
	} else if(isEnum(field)) {
	    generate_read_enum(msg,field,field.isPacked(),printer);
	} else {
	    // Generate needed local variables
	    if(!isPrimitive(field)) {
	        printer.println("int size;");
	    }
	    if(isRepeated(field))
	        printer.printf("%s tmp;\n",jtypefor(field.getType()));
	    // Verify that the wiretype == Ast_counted
	    printer.println("if(wiretype[0] != Ast_counted)");
	    printer.indent();
	    printer.println("throw new ASTException(AST_EFAIL);");
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
    printer.println("skip_field(wiretype[0],fieldno[0]);");
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
    printer.println("return this;");
    printer.outdent();    printer.printf("} /*%s_read*/\n",msg.getName());
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
	printer.printf("%s = read_primitive_%s();\n",
			       jfieldvar(field),
			       jfieldsortprefix(field));
	break;

    case OPTIONAL:
        printer.printf("%s = null;\n", jfieldvar(field));
        printer.printf("%s = read_primitive_%s();\n",
                           jfieldvar(field),
                           jfieldsortprefix(field));
        break;

    case REPEATED:
	if(ispacked) {
            printer.printf("%s = read_primitive_packed_%s();\n",
                                jfieldvar(field),
                                jfieldsortprefix(field));
	} else {
            printer.printf("%s tmp;\n",jtypefor(field.getType()));
            printer.printf("tmp = read_primitive_%s();\n",
                                jfieldsortprefix(field));
	    printer.printf("%s = repeat_append(%s,tmp,%s);\n",
			    jfieldvar(field),
                            jfieldsort(field),
			    jfieldvar(field));
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
    case OPTIONAL:
	printer.printf("%s = %s.toEnum(read_primitive_enum());\n",
  			       jfieldvar(field),
  			       jtypefor(field.getType()));
	break;
    case REPEATED:
	if(ispacked) {
	    // This is the hard case; must essentially treat
            // as packed int and convert
            printer.println("int[] tmp = read_primitive_packed_enum();");
	    printer.printf("%s = new %s[tmp.length];\n",
			   jfieldvar(field),
			   jfieldsort(field));
	    printer.println("for(int i=0;i<tmp.length;i++;");
	    printer.indent();
  	        printer.printf("%s[i] = %s.toEnum(tmp[i]);\n",
			   jfieldvar(field),
			   jtypefor(field.getType()));
	    printer.outdent();
	    printer.println("tmp = null;");
	} else {
            printer.printf("%s tmp = null;\n",jtypefor(field.getType()));
            printer.printf("tmp = %s.toEnum(read_primitive_enum());\n",
			   jtypefor(field.getType()));
	    printer.printf("%s = (%s[])repeat_append_enum(tmp,(object[])%s);\n",
                            jfieldvar(field),
                            jfieldsort(field),
                            jfieldvar(field));
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
    case OPTIONAL:
        printer.printf("%s = new %s(rt).read();\n",
			    jfieldvar(field),
			    jfieldvar(field.getType()));
	break;

    case REPEATED:
	printer.printf("tmp = new %s(rt).read();\n",
			jtypefor(field.getType()));
        printer.printf("%s = repeat_append(%s,tmp);\n",
				jfieldvar(field),
				jfieldvar(field));
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

    printer.printf("if(%s == null) {\n",
                        jfieldvar(field));
    printer.indent();
    switch (psort) {
    case STRING:
        printer.printf("%s = %s;\n",
                        jfieldvar(field),
			(field_default==null?"null":field_default)
			);
	break;
    case BYTES:
        printer.printf("%s = %d;\n",
                        jfieldvar(field),
			(field_default == null?"null":field_default.length()/2)
			);
        printer.printf("%s = %s;\n",
                        jfieldvar(field),
			(field_default==null?"null":field_default)
			);
	break;
    default:
        printer.printf("%s = %s;\n",
                        jfieldvar(field),field_default);
	break;
    }
    printer.outdent();
    printer.println("}");
}


void generate_sizefunction(AST.Message msg, Printer printer)
    throws Exception
{
    printer.println("public long");
    printer.println("getSize()");
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

	if(isRequired(field)) {
	    if(isPrimitive(field) || isEnum(field)) {
		// Add in the prefix tag size
		printer.printf("totalsize += getTagSize(%s,%d);\n",
			(field.isPacked()?jfieldsort(field):"Ast_counted"),
			field.getId());
		printer.printf("totalsize += getSize(%s,%s);\n",
			       jfieldsort(field),
			       (isEnum(field)?jfieldvar(field)+".getValue()"
			                     :jfieldvar(field)));
	    } else if(isMessage(field)) {
		printer.printf("totalsize += %s.getSize();\n",
			    jfieldvar(field));
		// Add in the prefix tag size
		printer.printf("totalsize += getTagSize(%s,%d);\n",
			(field.isPacked()?jfieldsort(field):"Ast_counted"),
			field.getId());
		// Add in the prefix count
		printer.println("totalsize += getSize(Ast_uint32,fieldsize);");
	    } else throw new Exception("unknown field type");
	} else if(isOptional(field)) {
	    printer.printf("if(%s != null) {\n",
			    jfieldvar(field));
	    printer.indent();
	    if(isPrimitive(field) || isEnum(field)) {
		// Add in the prefix tag size
		printer.printf("totalsize += getTagSize(%s,%d);\n",
			(field.isPacked()?jfieldsort(field):"Ast_counted"),
			field.getId());
		printer.printf("totalsize += getSize(%s,%s);\n",
			       jfieldsort(field),
			       (isEnum(field)?jfieldvar(field)+".getValue()"
			                     :jfieldvar(field)));
	    } else if(isMessage(field)) {
		printer.printf("totalsize += %s.getSize(%s);\n",
			    jfieldvar(field.getType()),
			    jfieldvar(field));
		// Add in the prefix count
		printer.println("totalsize += getSize(Ast_uint32,fieldsize);");
		// Add in the prefix tag size
		printer.printf("totalsize += getTagSize(%s,%d);\n",
			(field.isPacked()?jfieldsort(field):"Ast_counted"),
			field.getId());
	    } else throw new Exception("unknown field type");
	    printer.outdent();
	    printer.printf("}\n");
	} else { // field.getCardinality() == AST.Cardinality.REPEATED
	    printer.printf("for(int i=0;i<%s.length;i++) "+LBRACE+"\n",
			    jfieldvar(field));
	    printer.indent();
	    if(isPrimitive(field) || isEnum(field)) {
		// Add in the prefix tag size
		printer.printf("totalsize += getTagSize(%s,%d);\n",
			(field.isPacked()?jfieldsort(field):"Ast_counted"),
			field.getId());
		printer.printf("totalsize += getSize(%s,%s[i]);\n",
			       jfieldsort(field),
			       (isEnum(field)?jfieldvar(field)+".getValue()"
			                     :jfieldvar(field)));
	    } else if(isMessage(field)) {
		printer.printf("totalsize += %s.getSize(%s[i]);\n",
			    jfieldvar(field.getType()),
			    jfieldvar(field));
		// Add in the prefix count
		printer.println("totalsize += getSize(Ast_uint32,fieldsize);");
		// Add in the prefix tag size
		printer.printf("totalsize += getTagSize(%s,%d);\n",
			(field.isPacked()?jfieldsort(field):"Ast_counted"),
			field.getId());
	    } else throw new Exception("unknown field type");
	    printer.outdent();
	    printer.println(RBRACE);
	}
        printer.blankline();
    }

    printer.println("return totalsize;");
    printer.outdent();
    printer.blankline();
    printer.printf("} /*%s.getSize*/\n",msg.getName());
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
jfieldvar(AST.Type asttype)
    throws Exception
{
    String typename = null;
    if(asttype.getSort() == AST.Sort.ENUM
	      || asttype.getSort() == AST.Sort.MESSAGE) {
	typename = asttype.getName();
    } else { // Illegal
	throw new Exception("jfieldvar: Illegal type: "+asttype.getName());
    }
    return converttojname(typename);
}

String jobjecttypefor(AST.Type asttype) {return jtypefor(asttype,true);}
String jtypefor(AST.Type asttype) {return jtypefor(asttype,false);}

String
jtypefor(AST.Type asttype, boolean objecttype)
{
    String typ = null;

    if(asttype.getSort() == AST.Sort.PRIMITIVETYPE) {
	switch (((AST.PrimitiveType)asttype).getPrimitiveSort()) {
	case SINT32:
	case SFIXED32:
	case INT32:   typ = (objecttype?"Integer":"int"); break;

	case FIXED32:
	case UINT32:   typ = (objecttype?"Long":"long"); break;

	case SINT64:
	case SFIXED64:
	case INT64:   typ = (objecttype?"Long":"long"); break;

	case FIXED64:
	case UINT64:   typ = (objecttype?"Long":"long"); break;

	case FLOAT:   typ = (objecttype?"Float":"float"); break;
	case DOUBLE:   typ = (objecttype?"Double":"double"); break;

	case BOOL:    typ = (objecttype?"Boolean":"boolean"); break;

	case STRING:  typ = "String"; break;
	case BYTES:   typ = "byte[]"; break;

	}
    } else if(asttype.getSort() == AST.Sort.ENUM) {
	typ = asttype.getName();
    } else if(asttype.getSort() == AST.Sort.MESSAGE) {
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
    if(isRepeated(field) | isOptional(field)) {
	return "null";
    } else {
	// See if the field has a defined default
	Object value = field.optionLookup("DEFAULT");
	if(value != null) return value.toString();
	if(fieldtype.getSort() == AST.Sort.PRIMITIVETYPE) {
	    return "0";
	} else if(fieldtype.getSort() == AST.Sort.ENUM) {
	    return "null";
	} else if(fieldtype.getSort() == AST.Sort.MESSAGE) {
	    //return String.format("%s.getDefaultInstance()",jtypefor(fieldtype));
	    return "null";
	}
    }
    return null;
}

String
jfieldsortprefix(AST.Field field)
{
    return jtypesort(field.getType());
}

String
jfieldsort(AST.Field field)
{
    return "Ast_"+jtypesort(field.getType());
}

String
jtypesort(AST.Type asttype)
{
    String sort = null;
    if(asttype.getSort() == AST.Sort.PRIMITIVETYPE) {
	switch (((AST.PrimitiveType)asttype).getPrimitiveSort()) {
	case SINT32: return "sint32";
	case SFIXED32: return "sfixed32";
	case UINT32: return "uint32";
	case FIXED32: return "fixed32";
	case INT32: return "int32";
	case SINT64: return "sint64";
	case SFIXED64: return "sfixed64";
	case UINT64: return "uint64";
	case FIXED64: return "fixed64";
	case INT64: return "int64";
	case FLOAT: return "float";
	case DOUBLE: return "double";
	case BOOL: return "bool";
	case STRING: return "string";
	case BYTES: return "bytes";
	// No default because we want the compiler to complain if any new
	// types are added.
	}
    } else if(asttype.getSort() == AST.Sort.ENUM) {
        return "enum";
    } else if(asttype.getSort() == AST.Sort.MESSAGE) {
        return "message";
    } else {
	System.err.println("Cannot translate type to Java sort:" + asttype.getSort().toString());
    }
    return null;
}


} // JGenerator


