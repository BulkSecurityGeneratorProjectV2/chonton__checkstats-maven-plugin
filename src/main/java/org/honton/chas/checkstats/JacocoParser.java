package org.honton.chas.checkstats;

import java.io.File;

import javax.xml.stream.events.StartElement;

public class JacocoParser extends XmlParser {

    JacocoParser(File srcDir) {
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
            if(isElementName(startElement, "report")) {
                return new ReportElement(this);
            }
            throw new IllegalStateException("not expected <"+startElement.getName().getLocalPart()+"> at root");
        }
    }
    
    private class ReportElement extends Element {
        
        public ReportElement(RootElement root) {
            super(root);
        }

        @Override
        public Element push(StartElement startElement) {
            if(isElementName(startElement, "package")) {
                return new PackageElement(this, getAttributeValue(startElement, "name"));
            }
            return new Element(this);
        }
    }

    private class PackageElement extends Element {
        
        final String packageName;
        
        public PackageElement(ReportElement root, String packageName) {
            super(root);
            this.packageName = packageName;
        }

        @Override
        public Element push(StartElement startElement) {
            if(isElementName(startElement, "sourcefile")) {
                return new SourceFileElement(this, packageName+"/"+getAttributeValue(startElement, "name"));
            }
            return new Element(this);
        }
    }

    private class SourceFileElement extends Element {

        private final Stat fileStat;

        public SourceFileElement(PackageElement prior, String fileName) {
            super(prior);
            fileStat = getFileStat(fileName, false);
        }

        private void setStat(String type, int missed, int covered) {
            double numerator = (double)missed;
            long denominator = (long)missed + (long)covered;
            fileStat.put(type, numerator / denominator);
        }

        @Override
        public Element push(StartElement startElement) {
            if(isElementName(startElement, "counter")) {
                setStat(getAttributeValue(startElement, "type"),
                        Integer.parseInt(getAttributeValue(startElement, "missed")),
                        Integer.parseInt(getAttributeValue(startElement, "covered"))
                );
            }
            return new Element(this);
        }
    }
}
