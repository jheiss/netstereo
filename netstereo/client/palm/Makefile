##############################################################################
# $Id$
##############################################################################
# Makefile for NetStereo Palm client
##############################################################################
# $Log$
# Revision 1.1  2001/03/16 21:52:54  jheiss
# Initial revision
#
##############################################################################

PROGRAM=PalmStereo
CC=m68k-palmos-gcc
PILRC=pilrc
OBJRES=m68k-palmos-obj-res
ICONTEXT='PalmStereo'
BUILDPRC=build-prc
APID=MP3R
SDK="/usr/local/palmdev/Palm OS 3.5 Support/Incs/"
CFLAGS=-palmos3.5 -Wall -g -I$(SDK) -I$(SDK)/Core -I$(SDK)/Core/System -I$(SDK)/Core/UI -I$(SDK)/Core/Hardware
LFLAGS=-g

SOURCES=$(PROGRAM).c
RESOURCEHEADER=$(PROGRAM)Rsc.h
HEADERS=$(PROGRAM).h $(RESOURCEHEADER)
OBJS=$(PROGRAM).o
PRC=$(PROGRAM).prc
RESOURCES=$(PROGRAM).rcp
ICON=$(PROGRAM).bmp


####
# Major targets

all: $(PRC)

$(PRC): $(PROGRAM) bin.res
	$(BUILDPRC) $(PRC) $(ICONTEXT) $(APID) *.bin *.grc

$(PROGRAM): $(OBJS)
	$(CC) -o $(PROGRAM) $(OBJS) $(LFLAGS)
	$(OBJRES) $(PROGRAM)

bin.res: $(RESOURCES) $(RESOURCEHEADER) $(ICON)
	$(PILRC) $(RESOURCES)
	touch bin.res

clean:
	rm -f *.bin *.grc *.o bin.res $(PRC) $(PROGRAM)

xfer: $(PRC)
	pilot-xfer -i $(PRC)

dist: $(PRC)
	rm -rf $(PROGRAM)-$(VER)
	mkdir $(PROGRAM)-$(VER)
	cp $(PRC) Makefile GPL TODO $(SOURCES) $(HEADERS) $(RESOURCES) $(ICON) $(PROGRAM)-$(VER)
	tar cf $(PROGRAM)-$(VER).tar $(PROGRAM)-$(VER)
	gzip $(PROGRAM)-$(VER).tar
	rm -rf $(PROGRAM)-$(VER)

####
# Dependency targets

$(OBJS): $(SOURCES) $(HEADERS)

