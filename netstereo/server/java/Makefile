##############################################################################
# $Id$
##############################################################################
# Makefile for the NetStereo server
##############################################################################
# $Log$
# Revision 1.1  2001/03/21 01:07:08  jheiss
# Initial revision
#
##############################################################################

JAVAC=javac
PROGRAM=StereoServer
VER=0.1

# Implicit rule for Java code
%.class: %.java
	$(JAVAC) $<

classes := $(patsubst %.java,%.class,$(wildcard *.java))

all: $(classes)

clean:
	rm -f *.class

dist: $(classes)
	rm -rf $(PROGRAM)-$(VER)
	mkdir $(PROGRAM)-$(VER)
	find Makefile README GPL TODO stereoserver.init server.properties *.class *.java de/ | cpio -pdum $(PROGRAM)-$(VER)
	tar cf $(PROGRAM)-$(VER).tar $(PROGRAM)-$(VER)
	gzip $(PROGRAM)-$(VER).tar
	rm -rf $(PROGRAM)-$(VER)

