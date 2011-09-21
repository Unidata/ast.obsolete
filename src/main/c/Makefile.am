## This is a automake file, part of Unidata's netCDF package.
# Copyright 2005, see the COPYRIGHT file for more information.

# This is the main automake file for netCDF. It builds the different
# netcdf directories. Not all directories are built, depending on the
# options selected during configure.

# This directory stores libtool macros, put there by aclocal.
ACLOCAL_AMFLAGS = -I m4

# These files get added to the distribution.
EXTRA_DIST =
# List of files to remove for a distclean
DISTCLEANFILES = Makefile
# List of files to remove for a maintainer-clean
MAINTAINERCLEANFILES =

#pkgconfigdir=$(libdir)/pkgconfig
#pkgconfig_DATA = ast.pc

# What needs to go in the binary dist?
#BINFILES = include/netcdf.h share/man/man3/netcdf.3 lib/libnetcdf.a

# Build the runtime library
libast_la_SOURCES = \
ast_byteio.c \
ast_curl.c \
ast_debug.c \
ast_internal.c \
ast_runtime.c \
ast_util.c \
ast_byteio.h \
ast_curl.h \
ast_debug.h \
ast_internal.h \
ast_runtime.h \
ast_util.h \
ast.h

lib_LTLIBRARIES = libast.la

#noinst_HEADERS = ast_curl.h ast_internal.h ast_util.h
include_HEADERS = ast.h ast_byteio.h ast_debug.h ast_runtime.h
