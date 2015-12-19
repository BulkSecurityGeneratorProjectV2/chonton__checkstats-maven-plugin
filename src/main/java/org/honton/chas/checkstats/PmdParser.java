package org.honton.chas.checkstats;

import java.io.File;

import javax.xml.stream.events.StartElement;

public class PmdParser extends XmlParser {

    PmdParser(File srcDir) {
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
            if(isElementName(startElement, "pmd")) {
                return new CpdElement(this);
            }
            throw new IllegalStateException("not expected <"+startElement.getName().getLocalPart()+"> at root");
        }
    }
    
    private class CpdElement extends Element {
        
        public CpdElement(RootElement root) {
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
    
    private static String[] PRIORITY_CLASS= {
        "",
        "critical",
        "likely",
        "confusing",
        "optional",
        "pedantic"
    };
    
    private class FileElement extends Element {

        private final Stat fileStat;

        public FileElement(CpdElement prior, String fileName) {
            super(prior);
            fileStat = getFileStat(fileName, true);
        }

        private void incrementFileStat(String priority) {
            String key = PRIORITY_CLASS[Integer.parseInt(priority)];
            incrementStat(fileStat, key);
        }

        @Override
        public Element push(StartElement startElement) {
            if(isElementName(startElement, "violation")) {
                incrementFileStat(getAttributeValue(startElement, "priority"));
            }
            return new Element(this);
        }
    }
}
