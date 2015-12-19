package org.honton.chas.checkstats;

import java.io.File;

import javax.xml.stream.events.StartElement;

public class FindbugsParser extends XmlParser {
    
    FindbugsParser(File srcDir) {
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
            if(isElementName(startElement, "BugCollection")) {
                return new BugCollectionElement(this);
            }
            else {
                throw new IllegalStateException("not expected <"+startElement.getName().getLocalPart()+"> at root");
            }
        }
    }
    
    private class BugCollectionElement extends Element {
        
        public BugCollectionElement(RootElement outer) {
            super(outer);
        }

        @Override
        public Element push(StartElement startElement) {
            if(isElementName(startElement, "FindBugsSummary")) {
                return new FindBugsSummaryElement(this);
            }
            else {
                return new Element(this);
            }
        }
    }
    
    private class FindBugsSummaryElement extends Element {
        
        public FindBugsSummaryElement(BugCollectionElement outer) {
            super(outer);
        }

        @Override
        public Element push(StartElement startElement) {
            if(isElementName(startElement, "FileStats")) {
                return new FileStatsElement(this, startElement);
            }
            else {
                return new Element(this);
            }
        }
    }
    
    private class FileStatsElement extends Element {
        public FileStatsElement(FindBugsSummaryElement outer, StartElement startElement) {
            super(outer);
            String fileName = getAttributeValue(startElement, "path");
            String bugCount = getAttributeValue(startElement, "bugCount");
            getFileStat(fileName, false).put("bugCount", new Integer(bugCount));
        }
    }

}
