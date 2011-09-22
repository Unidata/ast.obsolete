AC_PREREQ(2.53)

AC_INIT([ast], [1.0], [dmh@ucar.edu])

# Keep libtool macros in an m4 directory.
AC_CONFIG_MACRO_DIR([m4])

AC_CANONICAL_HOST

AC_CONFIG_HEADERS([config.h])

# This call is required by automake.
AM_INIT_AUTOMAKE([foreign dist-zip])

AC_MSG_NOTICE([finding utilities])
AC_PROG_LIBTOOL
AC_PROG_INSTALL

AC_MSG_NOTICE([finding C compiler])
AC_PROG_CC
AM_PROG_CC_C_O
AC_C_CONST

AC_CHECK_HEADERS([stdio.h stdarg.h errno.h stdlib.h string.h stdint.h])
AC_CHECK_TYPES([ptrdiff_t, uchar, longlong])
AC_C_CHAR_UNSIGNED
AC_C_BIGENDIAN
AC_CHECK_SIZEOF(short)
AC_CHECK_SIZEOF(int)
AC_CHECK_SIZEOF(long)
AC_CHECK_SIZEOF(float)
AC_CHECK_SIZEOF(double)
AC_CHECK_SIZEOF(off_t)
AC_CHECK_SIZEOF(size_t)
AC_CHECK_SIZEOF(ptrdiff_t)

AC_MSG_NOTICE([generating header files and makefiles])
AC_CONFIG_FILES([Makefile
		 doc/Makefile
		 src/main/c/runtime/Makefile
		 src/test/c/Makefile
		])
AC_OUTPUT()

