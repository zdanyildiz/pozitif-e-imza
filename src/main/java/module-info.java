module com.globalpozitif.giblauncher {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.xml;
    requires org.apache.hc.client5.httpclient.jakarta; // HttpClient 5 modular name might vary, checking
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.globalpozitif.giblauncher to javafx.graphics, javafx.fxml;
    opens com.globalpozitif.giblauncher.ui to javafx.fxml;
    
    exports com.globalpozitif.giblauncher;
    exports com.globalpozitif.giblauncher.core;
    exports com.globalpozitif.giblauncher.ui;
    exports com.globalpozitif.giblauncher.security;
}
