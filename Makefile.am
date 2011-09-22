## This is a automake file, part of Unidata's netCDF package.
# Copyright 2005, see the COPYRIGHT file for more information.

# This is the main automake file for netCDF. It builds the different
# netcdf directories. Not all directories are built, depending on the
# options selected during configure.

# Control where install occurs
build_dir = ${abs_builddir}/target

# This directory stores libtool macros, put there by aclocal.
ACLOCAL_AMFLAGS = -I m4

astjar = ast.jar

SUBDIRS=src/main/c/runtime src/test/c doc

# These files get added to the distribution.
EXTRA_DIST =
# List of files to remove for a distclean
DISTCLEANFILES = Makefile
# List of files to remove for a maintainer-clean
MAINTAINERCLEANFILES =

EXTRA_DIST += build.xml CHANGES.txt LICENSE.txt README.txt 

