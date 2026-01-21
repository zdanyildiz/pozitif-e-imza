package com.globalpozitif.giblauncher.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class J2se {
    @JacksonXmlProperty(isAttribute = true)
    private String version;

    @JacksonXmlProperty(isAttribute = true, localName = "initial-heap-size")
    private String initialHeapSize;

    @JacksonXmlProperty(isAttribute = true, localName = "max-heap-size")
    private String maxHeapSize;

    @JacksonXmlProperty(isAttribute = true, localName = "java-vm-args")
    private String javaVmArgs;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInitialHeapSize() {
        return initialHeapSize;
    }

    public void setInitialHeapSize(String initialHeapSize) {
        this.initialHeapSize = initialHeapSize;
    }

    public String getMaxHeapSize() {
        return maxHeapSize;
    }

    public void setMaxHeapSize(String maxHeapSize) {
        this.maxHeapSize = maxHeapSize;
    }

    public String getJavaVmArgs() {
        return javaVmArgs;
    }

    public void setJavaVmArgs(String javaVmArgs) {
        if (javaVmArgs != null) {
            // New-line karakterlerini temizle
            this.javaVmArgs = javaVmArgs.replaceAll("\\R", " ").trim();
        } else {
            this.javaVmArgs = null;
        }
    }
}