package com.example.university_allowance_system;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class
HelloApplication extends Application {


    @Override
    public void start(Stage stage) throws IOException {
        DatabaseInitializer.initializeDatabase();


        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Login_Screen.fxml"));

        Scene scene = new Scene(fxmlLoader.load());

       stage.setTitle("");
        stage.setScene(scene);
        stage.show();
    }

    
    public static void main(String[] args) {
        launch();
    }
}