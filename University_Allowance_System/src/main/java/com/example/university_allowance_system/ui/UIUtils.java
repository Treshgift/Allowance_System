package com.example.university_allowance_system.ui;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.scene.text.Text;

public class UIUtils {

    public static void applyPresentationStyles(Scene scene) {
        try {
            String css = UIUtils.class.getResource("/com/example/university_allowance_system/styles/common.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("UIUtils: failed to apply presentation styles - " + e.getMessage());
        }
    }

    public static void showSimpleModal(Stage owner, String title, String message) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);

        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 18; -fx-background-color: -surface; -fx-background-radius: 8;");

        Text t = new Text(message);
        t.setStyle("-fx-fill: -muted; -fx-font-size: 13px;");

        Button ok = new Button("OK");
        ok.setOnAction(ev -> dialog.close());
        ok.getStyleClass().add("btn");

        box.getChildren().addAll(t, ok);

        Scene s = new Scene(box);
        try { s.getStylesheets().add(UIUtils.class.getResource("/com/example/university_allowance_system/styles/common.css").toExternalForm()); } catch (Exception ex) {}
        dialog.setScene(s);
        dialog.showAndWait();
    }
}

