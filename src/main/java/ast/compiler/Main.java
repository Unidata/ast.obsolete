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

package unidata.protobuf.ast.compiler;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import unidata.protobuf.ast.compiler.*;

import java.util.*;
import java.io.*;

public class Main
{
    // Define a map of -L language tags to class name tags
    static String[][] languagemap = new String[][] {
	// Note, the language tag is tested as lower case
	// while the class name tag is used as is.
	{"java","Java"},
	{"c","C"},
	{"c++","Cpp"}, 
	{"c#","Csharp"},
	{"python","Python"},
	{"ruby","Ruby"}
    };

    static final String DFALTLANGUAGE = "c";
    static final String DFALTPACKAGE = "unidata.protobuf.ast.compiler.generators";

    static List<String> includePaths = new ArrayList<String>();
    static List<String> defines = new ArrayList<String>();
    static List<String> arglist = new ArrayList<String>();
    static String optionLanguage = null;
    static boolean optionVerbose = false;
    static boolean optionParseDebug = false;
    static boolean optionSemanticsDebug = false;
    static boolean optionSemanticStepsDebug = false;
    static boolean optionDuplicate = false;
    static boolean optionTrace = false;

    static boolean debug = false;

    // Getters
    static public boolean getDebug() {return debug;}
    static public boolean getOptionTrace() {return optionTrace;}

    //////////////////////////////////////////////////
    static public void main(String[] argv) throws Exception
    {
	int c;

	String options = "-:D:I:L:W:V"; // update usage() if changed
	// In case we ever need to use long options
	StringBuilder longopttag = new StringBuilder();
	LongOpt[] LongOptions = new LongOpt[]{
	    new LongOpt("xxx",LongOpt.REQUIRED_ARGUMENT,longopttag,1)
        };

	Getopt g = new Getopt("Main",argv,options,null);
	while ((c = g.getopt()) != -1) {
            switch (c) {
	    case 0:
		// In case we ever need to use long options
	        int index = 0;
	        try {index = Integer.parseInt(longopttag.toString());}
	        catch (NumberFormatException nfe) {
		    System.err.println("Unknown long option");
		    System.exit(1);
		}
		System.err.println("Unknown long option");
  	        System.exit(1);
                break;
	    case 1: // intermixed non-option
		arglist.add(g.getOptarg());
		break;		
	    case 'D':
		String def = g.getOptarg();
		if(def.length() > 0) defines.add(def);
		break;	
	    case 'I': // Note: these are used for import searches
		String path = g.getOptarg();
		if(path.length() > 0) includePaths.add(path);
		break;	
	    case 'L':
		optionLanguage = g.getOptarg();
		break;
	    case 'W':
		String wvalue = g.getOptarg();
		for(char x: wvalue.toCharArray()) {
		    switch (x) {
		    case 'd': debug = true; break;
		    case 'p': optionParseDebug = true; break;
		    case 't': optionSemanticStepsDebug = true; break;
		    case 's': optionSemanticsDebug = true; break;
		    case 'D': optionDuplicate = true; break;
		    case 'T': optionTrace = true; break;
		    default: break;
		    }
		}
		break;
	    case 'V':
		optionVerbose = true;		
		break;
	    case ':':
	        usage("Command line option requires argument "+g.getOptopt());
	    case '?':
	        usage("Illegal cmd line option: "+g.getOptopt());
	    default: break; // ignore
	    }
        }

	// Map the -L language to the class tag
	String classLanguageTag = null;
        if(optionLanguage == null) optionLanguage = DFALTLANGUAGE;
        for(int i=0;i<languagemap.length;i++) {
	    if(optionLanguage.equalsIgnoreCase(languagemap[i][0])) {
	        classLanguageTag = languagemap[i][1];
		break;
	    }
        }
        if(classLanguageTag == null) {
	    usage("Unknown language: "+optionLanguage);
        }


	if(arglist.size() == 0) {
	    usage("No input file specified");
	}

	String rawfilename = arglist.get(0);
	String escapedfilename = AuxFcns.escapedname(rawfilename);
	String inputfilename = AuxFcns.locatefile(rawfilename,includePaths);

	if(inputfilename == null) {
	    usage("Cannot locate input file: "+rawfilename);
	}
	File inputfile = new File(inputfilename);
	if(!inputfile.canRead()) {
	    usage("Cannot read input file: "+inputfile.toString());
	}
	FileReader rdr = new FileReader(inputfile);

        ASTFactory factory = new ASTFactoryDefault();
        ProtobufParser parser = new ProtobufParser(factory);
	parser.setIncludePaths(includePaths);
	if(optionParseDebug) parser.setDebugLevel(1);

	boolean pass = parser.parse(rawfilename,rdr);
	if(!pass) {
	    usage("Parse failed");
	}

	// Compute the post-getopt argv.
        String[] finalargv = arglist.toArray(new String[arglist.size()]);

        Semantics sem = new ProtobufSemantics();

	// Try to locate language specific classes using reflection
        ClassLoader classLoader = Main.class.getClassLoader();

	// Try to locate the language specific Semantics checking class.
	String semanticsclassname = DFALTPACKAGE
				    + "."
				    + classLanguageTag
				    + "Semantics";
        Semantics langsemantics = null;
        try {
            Class semanticsclass = Class.forName(semanticsclassname);
	    langsemantics = (Semantics)semanticsclass.newInstance();
        } catch (ClassNotFoundException e) {/* Ignore */ }

	String generatorclassname = DFALTPACKAGE
				    + "."
				    + classLanguageTag
				    + "Generator";
	// Try to locate the language specific Generator class
	Generator generator = null;
        try {
            Class generatorclass = Class.forName(generatorclassname);
	    generator = (Generator)generatorclass.newInstance();
        } catch (ClassNotFoundException e) {
	    usage("Generator class not found: "+generatorclassname);
        }

	// Semantic Processing

	if(optionSemanticStepsDebug) Debug.setTag("trace.semantics.steps");
	if(optionSemanticStepsDebug) Debug.setTag("trace.semantics.steps");
	if(optionSemanticsDebug) Debug.setTag("trace.semantics");
	if(optionDuplicate) {
	    Debug.setTag("trace.duplicate.package");
	}

	// Invoke all the initializers
	if(!sem.initialize(parser.getAST(),finalargv,factory)) {
	    usage("Protobuf semantic initialization failure.");
	}
        if(!langsemantics.initialize(parser.getAST(),finalargv,factory)) {
            usage(optionLanguage+": semantic initialization failure.");
        }

        // Do semantic processing
	pass = sem.process(parser.getAST());
	if(!pass) {
	    usage("Protobuf semantic error detected.");
	}

	pass = langsemantics.process(parser.getAST());
	if(!pass) {
	    usage(optionLanguage+": semantic errors detected");
	}

        pass = generator.generate(parser.getAST(),finalargv);
        if(!pass) {
                usage(optionLanguage+": code generation errors detected");
        }


    }

    static void usage(String msg)
    {
	System.err.println(msg);
	System.err.print(
"usage: java -jar ast.jar <options>*\n"
+"where the options are:\n"
+"-V              -- verbose.\n"
+"-D option=value -- define global options.\n"
+"-I include-file -- specify include file to include in .c output;\n"
+"                   the file name may optionally be surrounded by <...>.\n"
+"-L language     -- specify the output language;\n"
+"                   currently only C is suppported.\n"
+"-W woption      -- specify various subsidiary debug options:\n"
+"                   'd' -- turn on general debugging."  
+"                   'p' -- turn on parsing debug output."  
+"                   't' -- track semantic steps."  
+"                   's' -- general semantics debug"  
+"                   'D' -- check for duplicates\n"
	);
	System.err.flush();
        System.exit(1);
    }    
}
