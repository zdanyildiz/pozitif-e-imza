module com.globalpozitif.giblauncher {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.xml;
    requires com.fasterxml.jackson.annotation;
    requires org.apache.hc.client5.http;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.globalpozitif.giblauncher to javafx.graphics, javafx.fxml;
    opens com.globalpozitif.giblauncher.core.model
            to com.fasterxml.jackson.databind, com.fasterxml.jackson.dataformat.xml;
    opens com.globalpozitif.giblauncher.ui to javafx.fxml;

    exports com.globalpozitif.giblauncher;
    exports com.globalpozitif.giblauncher.core;
    exports com.globalpozitif.giblauncher.core.model;
    exports com.globalpozitif.giblauncher.core.service;
    exports com.globalpozitif.giblauncher.security;
}
