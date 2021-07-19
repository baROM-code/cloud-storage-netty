package com.barom.cloudstoragenetty.controllers;

import com.barom.cloudstoragenetty.FileInfo;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientController implements Initializable {
    @FXML
    TableView<FileInfo> serverTable;
    @FXML
    ComboBox<String> disksBoxServer;
    @FXML
    TextField pathServer;
    @FXML
    TableView<FileInfo> clientTable;
    @FXML
    ComboBox<String> disksBoxClient;
    @FXML
    TextField pathClient;
    @FXML
    FlowPane fileView;

    // n.i.o
    private SocketChannel clinetSocket;
    private boolean authenticated = true;
    // для панели просмотра файлов
    private boolean isviewfilemode = false;
    private String viewfilename = "";


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // просмотр файлов
        fileView.setVisible(false);

        // Cервернная панель
        TableColumn<FileInfo, String> fileTypeColumnL = new TableColumn<>();
        fileTypeColumnL.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumnL.setPrefWidth(20);
        TableColumn<FileInfo, String> filenameColumnL = new TableColumn<>("Имя");
        filenameColumnL.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumnL.setPrefWidth(210);
        TableColumn<FileInfo, Long> fileSizeColumnL = new TableColumn<>("Размер");
        fileSizeColumnL.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumnL.setCellFactory(column -> new TableCell<FileInfo, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    String text = String.format("%,d байт", item);
                    if (item == -1L) {
                        text = "[ПАПКА]";
                    }
                    setText(text);
                }
            }
        });
        fileSizeColumnL.setPrefWidth(100);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.YYYY HH:mm");
        TableColumn<FileInfo, String> fileDateColumnL = new TableColumn<>("Дата изменения");
        fileDateColumnL.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumnL.setPrefWidth(110);

        serverTable.getColumns().addAll(fileTypeColumnL, filenameColumnL, fileSizeColumnL, fileDateColumnL);
        serverTable.getSortOrder().add(fileTypeColumnL);
        disksBoxServer.getItems().clear();
        disksBoxServer.getItems().add("Server:\\");
        disksBoxServer.getSelectionModel().select(0);
        serverTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    if (serverTable.getSelectionModel().getSelectedItem().isDirectory()) {
                        sendCommand("cd;" + serverTable.getSelectionModel().getSelectedItem().getFilename());
                     }
                }
            }
        });

        // Клиентская панель
        TableColumn<FileInfo, String> fileTypeColumnR = new TableColumn<>();
        fileTypeColumnR.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumnR.setPrefWidth(20);
        TableColumn<FileInfo, String> filenameColumnR = new TableColumn<>("Имя");
        filenameColumnR.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumnR.setPrefWidth(210);
        TableColumn<FileInfo, Long> fileSizeColumnR = new TableColumn<>("Размер");
        fileSizeColumnR.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumnR.setCellFactory(column -> new TableCell<FileInfo, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    String text = String.format("%,d байт", item);
                    if (item == -1L) {
                        text = "[ПАПКА]";
                    }
                    setText(text);
                }
            }
        });
        fileSizeColumnR.setPrefWidth(100);
        TableColumn<FileInfo, String> fileDateColumnR = new TableColumn<>("Дата изменения");
        fileDateColumnR.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumnR.setPrefWidth(110);

        clientTable.getColumns().addAll(fileTypeColumnR, filenameColumnR, fileSizeColumnR, fileDateColumnR);
        clientTable.getSortOrder().add(fileTypeColumnR);
        disksBoxClient.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBoxClient.getItems().add(p.toString());
        }
        disksBoxClient.getSelectionModel().select(0);

        clientTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Path path = Paths.get(pathClient.getText()).resolve(clientTable.getSelectionModel().getSelectedItem().getFilename());
                    if (Files.isDirectory(path)) {
                        updateClientPanel(path);
                    }
                }
            }
        });
        updateClientPanel(Paths.get("."));

        clientNIO();
        sendCommand("curpathls");
    }

    public void cmExitAction(ActionEvent actionEvent) {
        Platform.exit();
        System.exit(0);
    }

    public void copyBtnAction(ActionEvent actionEvent) {
        if (serverTable.isFocused() && serverTable.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.FILE) {
            String amsg = "Копировать '";
            amsg += serverTable.getSelectionModel().getSelectedItem().getFilename() + "' в: \n" + pathClient.getText();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Вопрос");
            alert.setHeaderText(amsg);
            Optional<ButtonType> res = alert.showAndWait();
            if (res.get() == ButtonType.OK) {
                String fname = serverTable.getSelectionModel().getSelectedItem().getFilename();
                if (Files.exists(Paths.get(pathClient.getText(), fname))) {
                    alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Вопрос");
                    alert.setHeaderText("Файл: " + fname + " существует!");
                    alert.setContentText("Скачать по новой?");
                    res = alert.showAndWait();
                    if (res.get() == ButtonType.OK) {
                        sendCommand("upload;" + fname);
                    }
                } else {
                    sendCommand("upload;" + fname);
                }
            }
        }
    }

    public void btnServerPathUpAction(ActionEvent actionEvent) throws IOException {
        sendCommand("cd;..");
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateClientPanel(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public void btnClientPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathClient.getText()).getParent();
        if (upperPath != null) {
            updateClientPanel(upperPath);
        }
    }

    public void sendCommand (String cmd) {
        ByteBuffer buffer = ByteBuffer.wrap(cmd.getBytes(StandardCharsets.UTF_8));
        try {
            if (clinetSocket.isConnected()) {
                clinetSocket.write(buffer);
                Thread.sleep(200); // задержка перед слеюдющей командой
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        buffer.clear();
    }

    private void clientNIO() {
        InetSocketAddress srvAddr = new InetSocketAddress("localhost", 5000);

        try {
            clinetSocket = SocketChannel.open(srvAddr);

        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
        //цикл работы
            while (authenticated) {
                try {
                    ByteBuffer clientBuffer = ByteBuffer.allocate(1024);
                    clinetSocket.read(clientBuffer);
                    String inmsg = new String(clientBuffer.array()).trim();
                    System.out.println("Ответ " + inmsg);
                    String[] instr = inmsg.split(";");
                    if ("curpathls".equals(instr[0])) {
                        updateServerTable(instr);
                    } else if ("sendfile".equals(instr[0])) {
                        String path = pathClient.getText();
                        String fname = instr[1];
                        if (isviewfilemode) { // получение временного файла для просмотра
                            path = ".";
                            fname = "tmp.tmp";
                            if ((instr[1]).endsWith(".txt")) { fname = "tmp.txt";}
                            if ((instr[1]).endsWith(".jpg")) { fname = "tmp.jpg";}
                            viewfilename = fname;
                            isviewfilemode = false;
                        }
                        try (FileChannel fileChannel = FileChannel.open(Paths.get(path, fname), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                            fileChannel.transferFrom(clinetSocket, 0, Long.parseLong(instr[2]));
                        }
                    } else if (instr[0].equals("ERROR")) {
                        errorWindow(instr[1]);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void errorWindow(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Ошибка");
        alert.setHeaderText(msg);
        alert.showAndWait();
    }

    private void updateServerTable(String[] str) {
        ObservableList<FileInfo> serverFilesData = FXCollections.observableArrayList();
        pathServer.setText(str[1]);
        if (str.length < 3) {
            serverTable.getItems().clear();
            serverTable.getItems().add(new FileInfo("..", -1L, LocalDateTime.now()));
            return;
        }
        String[] filesinfo = str[2].split("#");
        serverTable.getItems().clear();
        for (String files : filesinfo) {
            String[] file = files.split("!");
            String filename = file[0];
            long size = Long.parseLong(file[1]);
            LocalDateTime lastModified = LocalDateTime.parse(file[2]);
            serverFilesData.add(new FileInfo(filename, size, lastModified));
        }
        serverTable.setItems(serverFilesData);
        serverTable.sort();
    }

    private void updateClientPanel(Path path) {
        try {
            pathClient.setText(path.normalize().toAbsolutePath().toString());
            clientTable.getItems().clear();
            clientTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            clientTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }


    public void delBtnAction(ActionEvent actionEvent) {
    }

    public void mkdirBtnAction(ActionEvent actionEvent) {
        if (serverTable.isFocused()) {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("");
            dialog.setHeaderText("Создать новый каталог:");
            dialog.setContentText("");
            Optional<String> res = dialog.showAndWait();
            if (res.isPresent()) {
                sendCommand("mkdir;" + dialog.getEditor().getText() + "\n");
            }
        }
    }

    public void viewBtnAction(ActionEvent actionEvent) {
      if (serverTable.isFocused() && serverTable.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.FILE) {
          String fname = serverTable.getSelectionModel().getSelectedItem().getFilename();
          fileView.getChildren().clear();
          fileView.setMaxSize(200, 100);
          if (!fname.endsWith(".txt") & !fname.endsWith(".jpg")) {return;}

          isviewfilemode = true;
          sendCommand("upload;" + fname);
          Path path = Paths.get(".", viewfilename).normalize();
          File file = new File(viewfilename);
          if (!file.exists()) { return;}

          if (viewfilename.endsWith(".txt")) {
              TextArea txtView = new TextArea();
              txtView.setEditable(false);
              try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                  String line;
                  while ((line = reader.readLine()) != null) {

                      txtView.appendText(line + "\n\r");
                  }
              } catch (FileNotFoundException e) {
                  e.printStackTrace();
              } catch (IOException e) {
                  e.printStackTrace();
              }
              /*
              for(String line : lines) {
                  txtView.appendText(line);
              }
          */
              fileView.getChildren().add(txtView);
          }
          if (viewfilename.endsWith(".jpg")) {
              String localUrl = null;
              try {
                  localUrl = file.toURI().toURL().toString();
              } catch (MalformedURLException e) {
                  e.printStackTrace();
              }
              Image image = new Image(localUrl, true);
              ImageView imageView = new ImageView(image);
              fileView.getChildren().add(imageView);
          }
          Button btn = new Button("Закрыть просмотр");
          btn.setOnAction(new EventHandler<ActionEvent>() {
              @Override
              public void handle(ActionEvent event) {
                  fileView.getChildren().clear();
                  fileView.setVisible(false);
                  // clientTable.setVisible(true);
                  if (Files.exists(path)) {
                      try {
                          Files.delete(path);
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                  }
              }
          });
          //clientTable.setVisible(false);
          fileView.getChildren().add(btn);
          fileView.setVisible(true);
      }
    }
}