##############################################################################
# $Id$
##############################################################################
# Makefile for the Java NetStereo client
##############################################################################
# $Log$
##############################################################################

JAVAC=javac
PROGRAM=StereoClient
VER=0.1

# Implicit rule for Java code
%.class: %.java
	$(JAVAC) $<

classes := $(patsubst %.java,%.class,$(wildcard *.java))

all: $(classes)

clean:
	rm -f *.class

dist:
	rm -rf $(PROGRAM)-$(VER)
	mkdir $(PROGRAM)-$(VER)
	find Makefile README GPL TODO *.class *.java | cpio -pdum $(PROGRAM)-$(VER)
	tar cf $(PROGRAM)-$(VER).tar $(PROGRAM)-$(VER)
	gzip $(PROGRAM)-$(VER).tar
	rm -rf $(PROGRAM)-$(VER)

