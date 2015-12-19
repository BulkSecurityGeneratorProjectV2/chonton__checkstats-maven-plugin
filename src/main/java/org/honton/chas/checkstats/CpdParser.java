package org.honton.chas.checkstats;

import java.io.File;

import javax.xml.stream.events.StartElement;

public class CpdParser extends XmlParser {

    CpdParser(File srcDir) {
        super(srcDir);
    }

    @Override
    Element getRootElement() {
        return new RootElement();
    }

    private void incrementFileStat(String fileName) {
        Stat fileStat = getFileStat(fileName, true);
        incrementStat(fileStat, "copyPastes");
    }
    
    private class RootElement extends Element {
        
        public RootElement() {
            super(null);
        }

        @Override
        public Element push(StartElement startElement) {
            if(isElementName(startElement, "pmd-cpd")) {
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
            if(isElementName(startElement, "duplication")) {
                return new DuplicationElement(this);
            }
            return new Element(this);
        }
    }
    
    private class DuplicationElement extends Element {
        
        public DuplicationElement(CpdElement root) {
            super(root);
        }

        @Override
        public Element push(StartElement startElement) {
            if(isElementName(startElement, "file")) {
                incrementFileStat(getAttributeValue(startElement, "path"));
            }
            return new Element(this);
        }
    }
}
