package org.honton.chas.checkstats;

import java.io.File;

import org.apache.maven.plugin.logging.Log;

import lombok.SneakyThrows;

public enum StandardStatistic {
    
    Checkstyle("checkstyle-result.xml") {
        @Override
        XmlParser createXmlParser(File srcDir) {
            return new CheckstyleParser(srcDir);
        }
    },
    
    Cpd("cpd.xml") {
        @Override
        XmlParser createXmlParser(File srcDir) {
            return new CpdParser(srcDir);
        }
    },
    
    Pmd("pmd.xml") {
        @Override
        XmlParser createXmlParser(File srcDir) {
            return new PmdParser(srcDir);
        }
    },
    
    Findbugs("findbugsXml.xml") {
        @Override
        XmlParser createXmlParser(File srcDir) {
            return new FindbugsParser(srcDir);
        }        
    },
    
    JaCoCo("site/jacoco/jacoco.xml") {
        @Override
        XmlParser createXmlParser(File srcDir) {
            return new JacocoParser(srcDir);
        }        
    };

    private final String reportName;

    abstract XmlParser createXmlParser(File srcDir);

    StandardStatistic(String reportName) {
        this.reportName = reportName;
    }

    @SneakyThrows
    public Stat read(Log log, File srcDir, File targetDir) {
        File reportFile = new File(targetDir, reportName);
        if (reportFile.isFile()) {
            return createXmlParser(srcDir).summarize(reportFile);
        } else {
            log.info(name() + " report (" + reportFile.getCanonicalPath() + ") was not found");
            return null;
        }
    }

}
