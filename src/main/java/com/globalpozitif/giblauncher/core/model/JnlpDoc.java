package com.globalpozitif.giblauncher.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "jnlp")
@JsonIgnoreProperties(ignoreUnknown = true)
public class JnlpDoc {
    @JacksonXmlProperty(isAttribute = true)
    private String spec;

    @JacksonXmlProperty(isAttribute = true)
    private String codebase;

    @JacksonXmlProperty(isAttribute = true)
    private String href;

    @JacksonXmlProperty(localName = "information")
    private Information information;

    @JacksonXmlProperty(localName = "security")
    private Security security;

    @JacksonXmlProperty(localName = "resources")
    private Resources resources;

    @JacksonXmlProperty(localName = "application-desc")
    private ApplicationDesc applicationDesc;

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getCodebase() {
        return codebase;
    }

    public void setCodebase(String codebase) {
        this.codebase = codebase;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public Information getInformation() {
        return information;
    }

    public void setInformation(Information information) {
        this.information = information;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Resources getResources() {
        return resources;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    public ApplicationDesc getApplicationDesc() {
        return applicationDesc;
    }

    public void setApplicationDesc(ApplicationDesc applicationDesc) {
        this.applicationDesc = applicationDesc;
    }
}