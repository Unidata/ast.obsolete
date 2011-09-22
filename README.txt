The ast system is an implementation
of the Google protobuf system.

There are several reasons for creating ast.

- The existing C code generator (http://code.google.com/p/protobuf-c")
  IMO generates overly complicated code.
    As a rule, what is needed is code to serialize (aka encode)
  and deserialize (aka decode) protobuf messages.  The RPC
  support should be entirely separate from the (de)serialize
  code.  Even Google is deprecating the RPC output for, one
  presumes, the same reason.

- The Unidata ast compiler uses a more traditional compiler
  approach based on the use of a (mostly) read-only abstract
  syntax tree (AST).  The key is that the AST is never (well,
  almost never) modified to add language specific code, which
  is the approach of the Google compiler.  Instead, code
  generation is performed by repeatedly walking the bare AST
  to obtain information.
    The result is a much cleaner system with the AST serving as
  a well-defined and fixed intermediary between parsing and
  code generation.

- Java is a much better language that C++ for writing
  portable compilers.

- Finally, building the AST compiler helped the author to
  get a better understanding of the protobuf protocol.

A C language code generator accompanies the compiler to
demonstrate its use. Other code generators for, e.g. Java
and C++, are possible, but the existing google generators
are probably adequate for those languages.

The following documents are available:
- INSTALL.txt describes the installation procedure
- CHANGES.txt describes release changes.
- LICENSE.txt describes the license, which basically Apache v2.
- doc/astuserman.html provides a more detailed manual
  for using ast.
- doc/astinternals.html provides a detailed description
  of the internal operation of ast and how one might
  extend it for other languages.

Author: Dennis Heimbigner
Organization: UCAR/Unidata
Email: dmh@ucar.edu

Copyright 2011, UCAR/Unidata.
