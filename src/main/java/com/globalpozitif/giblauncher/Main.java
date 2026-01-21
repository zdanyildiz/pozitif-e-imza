package com.globalpozitif.giblauncher;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
        logger.info("Pozitif E-İmza Launcher başlatılıyor...");
        
        Label label = new Label("Pozitif E-İmza Launcher");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 400, 300);
        
        primaryStage.setTitle("Pozitif E-İmza Launcher");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
