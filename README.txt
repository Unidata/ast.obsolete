Introduction
------------
The installation is a rather ugly mix of ant and Make
based actitivies. One should use the build.xml
to build and install the Java-based compiler and use
the ./configure, make process to build the C-code run-time.

Install
-------
1. Edit build.xml and modify the "prefix" property to
   define the location for the installation of ast.jar. The default
   is the local directory "/usr/local".

2. Invoke "ant install" to build the Java-based compiler
   and install in ${prefix}/ast.jar.

3. Invoke ./configure -prefix=<installdirectory>,
   where <installdirectory> is the same as the prefix specified
   int step 1.

4. Invoke "make install".

5. If desired, invoke "make check".

Using the compiler
------------------

1. Invoke the compiler using a command similar to this:
       java -jar ${prefix}/ast.jar XXX.proto
   This compiles XXX.proto and produces XXX.{c,h}.

2. Invoke (e.g.) gcc to compile and load a test program.
	gcc -o test.exe testast.c XXX.c -L${prefix}/lib -last
