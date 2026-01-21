package com.globalpozitif.giblauncher.core;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.globalpozitif.giblauncher.core.model.JnlpDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class JnlpParser {
    private static final Logger logger = LoggerFactory.getLogger(JnlpParser.class);
    private final XmlMapper xmlMapper;

    public JnlpParser() {
        this.xmlMapper = new XmlMapper();
    }

    public JnlpDoc parse(File file) throws IOException {
        logger.info("JNLP dosyası parse ediliyor: {}", file.getAbsolutePath());
        return xmlMapper.readValue(file, JnlpDoc.class);
    }

    public JnlpDoc parse(InputStream inputStream) throws IOException {
        logger.info("JNLP akışı parse ediliyor...");
        return xmlMapper.readValue(inputStream, JnlpDoc.class);
    }

    public JnlpDoc parse(URL url) throws IOException {
        logger.info("JNLP URL'den parse ediliyor: {}", url);
        return xmlMapper.readValue(url, JnlpDoc.class);
    }
}
