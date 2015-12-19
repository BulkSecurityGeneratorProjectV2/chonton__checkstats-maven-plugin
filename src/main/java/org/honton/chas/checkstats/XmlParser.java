package org.honton.chas.checkstats;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import lombok.SneakyThrows;

public abstract class XmlParser {

    static final XMLInputFactory factory = XMLInputFactory.newInstance();

    final private Stat reportRootStat;
    final private Path root;

    XmlParser(File srcDir) {
        this.reportRootStat = new Stat();
        this.root = srcDir.toPath();
    }

    private String resolve(String name) {
        return root.relativize(new File(name).toPath()).toString();
    }

    Stat getFileStat(String filename, boolean resolve) {
        if(resolve) {
            filename = resolve(filename);
        }
        Stat stat = reportRootStat.getStat(filename);
        if (stat == null) {
            stat = new Stat();
            reportRootStat.put(filename, stat);
        }
        return stat;
    }

    static void incrementStat(Stat stat, String key) {
        Number prior = stat.getNumber(key);
        stat.put(key, prior != null ? prior.intValue() + 1 : 1);
    }

    @SneakyThrows
    public Stat summarize(File file) {
        InputStream is = new FileInputStream(file);
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
        XMLEventReader eventReader = factory.createXMLEventReader(reader);
        try {
            processFile(eventReader, getRootElement());
            return reportRootStat;
        } finally {
            eventReader.close();
        }
    }

    abstract Element getRootElement();

    @SneakyThrows
    private void processFile(XMLEventReader eventReader, Element current) {
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            switch (event.getEventType()) {
            case XMLStreamConstants.START_ELEMENT:
                StartElement startElement = event.asStartElement();
                current = current.push(startElement);
                break;
            case XMLStreamConstants.END_ELEMENT:
                current = current.pop();
                break;
            }
        }
    }

    static class Element {
        private final Element prior;

        public Element(Element prior) {
            this.prior = prior;
        }

        public Element push(StartElement startElement) {
            return new Element(this);
        }

        public Element pop() {
            return prior;
        }

        static boolean isElementName(StartElement startElement, String name) {
            return startElement.getName().getLocalPart().equals(name);
        }

        static String getAttributeValue(StartElement startElement, String name) {
            Attribute attribute = startElement.getAttributeByName(new QName(name));
            return attribute != null ? attribute.getValue() : null;
        }
    }

}
