package com.globalpozitif.giblauncher.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Security {
    @JacksonXmlProperty(localName = "all-permissions")
    private String allPermissions;

    public String getAllPermissions() {
        return allPermissions;
    }

    public void setAllPermissions(String allPermissions) {
        this.allPermissions = allPermissions;
    }
}
