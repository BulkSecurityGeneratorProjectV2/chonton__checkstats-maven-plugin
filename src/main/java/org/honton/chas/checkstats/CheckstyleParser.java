package org.honton.chas.checkstats;

import java.io.File;

import javax.xml.stream.events.StartElement;

public class CheckstyleParser extends XmlParser {
    
    CheckstyleParser(File srcDir) {
        super(srcDir);
    }

    @Override
    Element getRootElement() {
        return new RootElement();
    }
    
    private class RootElement extends Element {
        
        public RootElement() {
            super(null);
        }

        @Override
        public Element push(StartElement startElement) {
            if(isElementName(startElement, "checkstyle")) {
                return new CheckstyleElement(this);
            }
            throw new IllegalStateException("not expected <"+startElement.getName().getLocalPart()+"> at root");
        }
    }
    
    private class CheckstyleElement extends Element {
        
        public CheckstyleElement(RootElement root) {
            super(root);
        }

        @Override
        public Element push(StartElement startElement) {
            if(isElementName(startElement, "file")) {
                return new FileElement(this, getAttributeValue(startElement, "name"));
            }
            return new Element(this);
        }
    }
    
    private class FileElement extends Element {

        private final Stat fileStat;

        public FileElement(CheckstyleElement prior, String fileName) {
            super(prior);
            fileStat = getFileStat(fileName, true);
        }

        @Override
        public Element push(StartElement startElement) {
            if(isElementName(startElement, "error")) {
                String severity = getAttributeValue(startElement, "severity");
                increment(severity);
            }
            return new Element(this);
        }

        private void increment(String value) {
            Number prior = fileStat.getNumber(value);
            fileStat.put(value, prior!=null ?prior.intValue()+1 :1);
        }
    }

}
