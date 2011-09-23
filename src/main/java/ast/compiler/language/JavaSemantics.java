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

package unidata.protobuf.ast.compiler.language

import java.util.List;
import java.util.ArrayList;
import java.io.*;

import static unidata.protobuf.ast.compiler.AST.*;
import static unidata.protobuf.ast.compiler.Debug.*;

/**
 * Implement any needed semantic tests
 *for generating Java code.
 */

public class JavaSemantics extends Semantics
{

//////////////////////////////////////////////////

AST.Root root = null;
ASTFactory factory = null;
String[] argv = null;

//////////////////////////////////////////////////
// Constructor
public JavaSemantics() {}

//////////////////////////////////////////////////

public boolean
initialize(AST.Root root, String[] argv, ASTFactory factory)
{
    this.factory = factory;
    this.root = root;
    this.factory = factory;
    this.argv = argv;

    // Define the predefined options
    List<OptionDef> odefs = root.getOptionDefs();

    // Add the predefined optiondefs; user defined options
    // will have already been added by parser
    odefs.add(new OptionDef("java_file", "string"));
    odefs.add(new OptionDef("package", "string"));
    odefs.add(new OptionDef("imports", "string"));
    odefs.add(new OptionDef("imports", "string"));
    odefs.add(new OptionDef("extends", "string"));
    odefs.add(new OptionDef("implements", "string"));

    return true;
}

//////////////////////////////////////////////////

public boolean
process(AST.Root root)
{
    boolean status = true;
    status = fixstringoptions(root);
    return status;
}

boolean
fixstringoptions(AST.Root root)
{
    for(AST node: root.getNodeSet()) {
	if(node.getSort() != AST.Sort.OPTION) continue;
	AST.Option option = (AST.Option)node;
	if(!option.isStringValued()) continue;
        String value = option.getValue();
	/* Add quotes and escapes */
	value = '"'
	        + AuxFcns.escapify(value,'"',AuxFcns.EscapeMode.EMODE_C)
		+ '"';
        option.setValue(value);
    }
    return true;
}

} // class Semantics
