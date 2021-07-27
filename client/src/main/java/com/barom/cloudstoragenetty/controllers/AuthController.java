package com.barom.cloudstoragenetty.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;


public class AuthController {
    private ClientController clientController;

    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    Label lblInfo;

    @FXML
    public void authBtnAction(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        lblInfo.setText("");
        clientController.sendCommand("auth;" + login + ";" + password);
    }

    @FXML
    public void regBtnAction(ActionEvent actionEvent) {

    }

    public void setController(ClientController clientController) {
        this.clientController = clientController;
    }
}
