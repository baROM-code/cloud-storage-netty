package com.barom.cloudstoragenetty;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Client extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/clientmain.fxml"));
        primaryStage.setTitle("Cloud Storage (Netty)");
        primaryStage.setScene(new Scene(root, 1024, 600));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
