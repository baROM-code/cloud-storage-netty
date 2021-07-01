package com.barom.cloudstoragenetty.controllers;

import com.barom.cloudstoragenetty.classes.FileInfo;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    @FXML
    VBox leftPanel, rightPanel;

    @FXML
    TableView<FileInfo> serverTable;

    @FXML
    ComboBox<String> disksBox;

    @FXML
    public TextField pathServer;

    private NettyClient client = new NettyClient();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // client.sendCmd("prompt");
        client.sendCmd("ls");
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setPrefWidth(24);
        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setPrefWidth(240);
        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setPrefWidth(120);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
        fileDateColumn.setPrefWidth(120);

        serverTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
        serverTable.getSortOrder().add(fileTypeColumn);

        disksBox.getItems().clear();
        disksBox.getItems().add("Server:\\");
        disksBox.getSelectionModel().select(0);



    }

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
        client.exit();
    }

    public void copyBtnAction(ActionEvent actionEvent) {

    }

    public void btnSrvPathUpAction(ActionEvent actionEvent) {
        client.sendCmd("cd ..");
    }
}