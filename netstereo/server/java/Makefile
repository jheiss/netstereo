##############################################################################
# $Id$
##############################################################################
# Makefile for the NetStereo server
##############################################################################
# $Log$
##############################################################################

JAVAC=javac
PROGRAM=NetStereo
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
	find Makefile README GPL TODO run killserver server.properties *.class *.java de/ | cpio -pdum $(PROGRAM)-$(VER)
	tar cf $(PROGRAM)-$(VER).tar $(PROGRAM)-$(VER)
	gzip $(PROGRAM)-$(VER).tar
	rm -rf $(PROGRAM)-$(VER)

