package com.globalpozitif.giblauncher.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Resources {
    @JacksonXmlProperty(localName = "j2se")
    private J2se j2se;

    @JacksonXmlProperty(localName = "jar")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Jar> jars;

    @JacksonXmlProperty(localName = "property")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Property> properties;

    public J2se getJ2se() {
        return j2se;
    }

    public void setJ2se(J2se j2se) {
        this.j2se = j2se;
    }

    public List<Jar> getJars() {
        return jars;
    }

    public void setJars(List<Jar> jars) {
        this.jars = jars;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }
}
