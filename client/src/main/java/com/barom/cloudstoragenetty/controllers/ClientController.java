package com.barom.cloudstoragenetty.controllers;

import com.barom.cloudstoragenetty.FileInfo;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
import java.util.*;
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
    @FXML
    Button btnView;

    private Stage stage;
    private Stage regStage;
    private AuthController authController;

    // n.i.o
    private SocketChannel clinetSocket;
    private boolean authenticated = false;

    // для панели просмотра файлов
    private final String textfiles = "txt,java,html,cpp,pas,ini,sh,log";
    private final String imgfiles = "png,jpg,jpeg,gif,bmp";
    private boolean isviewfilemode = false;
    private String viewfilename = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //действия при закрытии окна приложения
        /*
        Platform.runLater(() -> {
            stage = (Stage) pathClient.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                sendCommand("disconnect");
                Platform.exit();
                System.exit(0);
            });
        });
*/
        // панель просмотра файлов
        fileView.setVisible(false);

        // Cервернная панель
        TableColumn<FileInfo, String> fileTypeColumnL = new TableColumn<>();
        fileTypeColumnL.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumnL.setPrefWidth(20);
        TableColumn<FileInfo, String> filenameColumnL = new TableColumn<>("Имя");
        filenameColumnL.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumnL.setPrefWidth(215);
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
        filenameColumnR.setPrefWidth(215);
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
        if (regStage == null) {
            createAuthRegWindow();
        }
        Platform.runLater(() -> {
            regStage.show();
        });
    }

    public void cmExitAction(ActionEvent actionEvent) {
        sendCommand("disconnect");
        Platform.exit();
        System.exit(0);
    }

    public ButtonType confirmWindow(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("");
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait().get();
    }

    @FXML
    private void copyBtnAction(ActionEvent actionEvent) {
        if (!authenticated) {
            return;
        }
        // Загрузка файла с сервера
        if (serverTable.isFocused() && serverTable.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.FILE) {
            String amsg = "Загрузить с сервера '";
            String fname = serverTable.getSelectionModel().getSelectedItem().getFilename();
            amsg += fname + "'\nв: " + pathClient.getText();
            if (confirmWindow(amsg, "") == ButtonType.OK) {
                if (Files.exists(Paths.get(pathClient.getText(), fname))) {
                    if (confirmWindow("Файл: '" + fname + "' существует!", "Скачать по новой?") == ButtonType.OK) {
                        sendCommand("upload;" + fname);
                    }
                } else {
                    sendCommand("upload;" + fname);
                }
            }
        }
        // Отправка файла на сервер
        if (clientTable.isFocused() && clientTable.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.FILE) {
            String amsg = "Отправить на сервер '";
            String fname = clientTable.getSelectionModel().getSelectedItem().getFilename();
            amsg += fname + "'\nв: " + pathServer.getText();
            if (confirmWindow(amsg, "") == ButtonType.OK) {
                try {
                    long filesize = Files.size(Paths.get(pathClient.getText(), fname));
                    //sendCommand("download;" + fname + ";" + filesize);
                    sendFileToServer(fname);
                    /*
                    try (FileChannel fileChannel = FileChannel.open(Paths.get(pathClient.getText(), fname), StandardOpenOption.READ)) {
                        fileChannel.transferTo(0, filesize, clinetSocket);
                    */
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendFileToServer(String fname) throws IOException {
        // FilePackage uploadFile = new FilePackage();
        long filesize = Files.size(Paths.get(pathClient.getText(), fname));
        File f = new File(pathClient.getText() + File.separator + fname);
        if (!f.exists()) {
            throw new FileNotFoundException();
        }
        /*
        uploadFile.setFilename(fname);
        uploadFile.setFilesize(filesize);
        ByteArrayOutputStream bytearr = new ByteArrayOutputStream();
        ObjectOutputStream objout = new ObjectOutputStream(bytearr);
        */

        RandomAccessFile raf = new RandomAccessFile(f, "r");
        int pos = 0;
        int endpos;
        int buflength = 8 * 1024;
        byte[] buffer = new byte[buflength];

        while ((endpos = raf.read(buffer)) != -1) {
            String filedata = "file;" + fname + "#" + filesize + "#" + pos;
            int toend = (int) (raf.length() - pos);
            if (toend < buflength) {
                byte[] minbuffer = Arrays.copyOf(buffer, toend);
                filedata += "#" + byteArrayToHex(minbuffer);
            } else {
                filedata += "#" + byteArrayToHex(buffer);
            }

            /*
            uploadFile.setStarPos(pos);
            uploadFile.setEndPos(pos + endpos);
            uploadFile.setBytes(buffer);
            System.out.println(uploadFile);
            objout.writeObject(uploadFile);
            objout.flush();
            ByteBuffer bb = ByteBuffer.wrap(bytearr.toByteArray());
            clinetSocket.write(bb);
             */

            System.out.println(filedata);
            sendCommand(filedata);
            pos += endpos;
        }
        raf.close();
    }

    // Конвертирует байтовый массив в hex представление
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @FXML
    private void btnServerPathUpAction(ActionEvent actionEvent) throws IOException {
        if (authenticated) {
            sendCommand("cd;..");
        }
    }

    @FXML
    private void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateClientPanel(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    @FXML
    private void btnClientPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathClient.getText()).getParent();
        if (upperPath != null) {
            updateClientPanel(upperPath);
        }
    }

    public void sendCommand(String cmd) {
        ByteBuffer buffer = ByteBuffer.wrap((cmd + "$>").getBytes(StandardCharsets.UTF_8));
        try {
            if (clinetSocket != null && clinetSocket.isConnected()) {
                clinetSocket.write(buffer);
                Thread.sleep(200); // задержка перед следующей командой
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
            errorWindow("Сервер не найден!");
            Platform.exit();
            System.exit(777);
            return;
        }
        new Thread(() -> {
            //цикл работы
            while (true) {
                try {
                    ByteBuffer clientBuffer = ByteBuffer.allocate(1024);
                    clinetSocket.read(clientBuffer);
                    String inmsg = new String(clientBuffer.array()).trim();
                    String[] instr = inmsg.split(";");
                    System.out.println("Ответ " + inmsg);

                    // Аутентификация или регистрация
                    if ("auth_ok".startsWith(instr[0])) {
                        authenticated = true;
                        sendCommand("curpathls");
                        Platform.runLater(() -> {
                            authController.passwordField.clear();
                            regStage.close();
                        });
                    } else if ("auth_error".startsWith(instr[0])) {
                        Platform.runLater(() -> {
                            authController.passwordField.clear();
                            authController.lblInfo.setText("Ошибка авторизации!\nНеверный логин или пароль.");
                        });
                    }
                    // Работа
                    if (authenticated) {
                        if ("curpathls".startsWith(instr[0])) {
                            updateServerPanel(instr);
                        } else if ("sendfile".startsWith(instr[0])) {
                            String path = pathClient.getText();
                            String fname = instr[1];
                            if (isviewfilemode) { // получение временного файла для просмотра
                                path = ".";
                                fname = "tmp.tmp";
                                // расширение файла
                                String fext = instr[1].substring(instr[1].indexOf(".") + 1);
                                if (textfiles.contains(fext)) {
                                    fname = "tmp.txt";
                                }
                                if (imgfiles.contains(fext)) {
                                    fname = "tmp.img";
                                }
                                viewfilename = fname;
                                isviewfilemode = false;
                            }
                            try (FileChannel fileChannel = FileChannel.open(Paths.get(path, fname), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                                fileChannel.transferFrom(clinetSocket, 0, Long.parseLong(instr[2]));
                            }
                        } else if ("file_uploaded".startsWith(instr[0])) {
                            updateClientPanel(Paths.get(pathClient.getText()));
                        } else if ("ERROR".startsWith(instr[0])) {
                            errorWindow(instr[1]);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void errorWindow(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Ошибка");
            alert.setHeaderText(msg);
            alert.showAndWait();
        });
    }

    private void updateServerPanel(String[] str) {
        // str содержит текущий путь и если есть список файлов/каталогов + емкость + дата изменения
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
            serverTable.getItems().add(new FileInfo(filename, size, lastModified));
        }
        serverTable.sort();
    }

    private void updateClientPanel(Path path) {
        try {
            pathClient.setText(path.normalize().toAbsolutePath().toString());
            clientTable.getItems().clear();
            clientTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            clientTable.sort();
        } catch (IOException e) {
            errorWindow("Не удалось обновить список файлов");
        }
    }

    @FXML
    private void delBtnAction(ActionEvent actionEvent) {
        if (!authenticated) {
            return;
        }
        if (serverTable.isFocused()) {
            String fname = serverTable.getSelectionModel().getSelectedItem().getFilename();
            String amsg = "Файл/каталог: '" + fname + "'";
            if (confirmWindow(amsg, "будет удален с сервера?") == ButtonType.OK) {
                sendCommand("delete;" + fname);
            }
        }
    }

    @FXML
    private void mkdirBtnAction(ActionEvent actionEvent) {
        if (!authenticated) {
            return;
        }
        if (serverTable.isFocused()) {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("");
            dialog.setHeaderText("Создать новый каталог:");
            dialog.setContentText("");
            Optional<String> res = dialog.showAndWait();
            if (res.isPresent()) {
                sendCommand("mkdir;" + dialog.getEditor().getText());
            }
        }
    }

    @FXML
    private void viewBtnAction(ActionEvent actionEvent) {
        if (!authenticated) {
            return;
        }
        if (serverTable.isFocused() && serverTable.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.FILE) {
            String fname = serverTable.getSelectionModel().getSelectedItem().getFilename();
            fileView.getChildren().clear();
            fileView.setMaxSize(200, 100);
            // расширение файла
            String fext = fname.substring(fname.indexOf(".") + 1);
            // проверяем тип файла
            if (!textfiles.contains(fext) & !imgfiles.contains(fext)) {
                return;
            }

            isviewfilemode = true;
            sendCommand("upload;" + fname);
            Path path = Paths.get(".", viewfilename).normalize();
            File file = new File(viewfilename);
            if (!file.exists()) {
                return;
            }
            btnView.setDisable(true);

            // просмотр текстовых файлов
            if (viewfilename.endsWith(".txt")) {
                TextArea txtView = new TextArea();
                txtView.clear();
                txtView.setEditable(false);
                List<String> list = null;
                try {
                    list = Files.readAllLines(path);
                    for (String str : list) {
                        txtView.appendText(str + "\n\r");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fileView.getChildren().add(txtView);
            }
            // просмотр изображений
            if (viewfilename.endsWith(".img")) {
                String localUrl = null;
                try {
                    localUrl = file.toURI().toURL().toString();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                //Image image = new Image(localUrl, true);
                Image image = new Image(localUrl, 200, 200, true, true);
                ImageView imageView = new ImageView(image);
                fileView.getChildren().add(imageView);
            }
            Button btn = new Button("Закрыть просмотр");
            btn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    fileView.getChildren().clear();
                    fileView.setVisible(false);
                    btnView.setDisable(false);
                    if (Files.exists(path)) {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            fileView.getChildren().add(btn);
            fileView.setAlignment(Pos.CENTER);
            fileView.setVisible(true);
        }
    }

    private void createAuthRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/authorization.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("Подключение/регистрация на сервере");
            regStage.setScene(new Scene(root, 260, 180));
            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.initStyle(StageStyle.UTILITY);
            authController = fxmlLoader.getController();
            authController.setController(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}