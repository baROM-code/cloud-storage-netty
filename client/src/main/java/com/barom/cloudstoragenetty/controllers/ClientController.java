package com.barom.cloudstoragenetty.controllers;

import com.barom.cloudstoragenetty.FileInfo;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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

    @FXML
    VBox userinfo;
    @FXML
    TextArea txtStorage;

    @FXML
    VBox authreg;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;

    @FXML
    Label copyLbl;
    @FXML
    ProgressBar copyBar;

    private Stage stage;

    // n.i.o
    private SocketChannel clinetSocket;
    private boolean authenticated = false;

    // для панели просмотра файлов
    private final String textfiles = "txt,java,html,cpp,pas,ini,sh,log";
    private final String imgfiles = "png,jpg,jpeg,gif,bmp";
    private boolean isviewfilemode = false;
    private String viewfilename = "";
    // Общий размер файлов пользователя
    float userstoragetotalsize;
    // Размер хранилища
    long userstoragesize;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //действия при закрытии окна приложения
        Platform.runLater(() -> {
            stage = (Stage) pathClient.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                sendCommand("disconnect");
                Platform.exit();
                System.exit(0);
            });
        });

        // панель просмотра файлов
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
        fileDateColumnL.setPrefWidth(105);

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
        fileDateColumnR.setPrefWidth(105);

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
                    if (filesize > (userstoragesize - userstoragetotalsize)) {
                        errorWindow("Ошибка копирования файла – недостаточно места на диске!");
                        return;
                    }
                    sendCommand("download;" + fname + ";" + filesize);
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

        long filesize = Files.size(Paths.get(pathClient.getText(), fname));
        File f = new File(pathClient.getText() + File.separator + fname);
        if (!f.exists()) {
            return;
        }

        Task copyTask = new Task() {
            @Override
            protected Object call() throws Exception {
                try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
                    int pos = 0;
                    int endpos;
                    int buflength = 8 * 1024;
                    byte[] buffer = new byte[buflength];
                    ByteBuffer bb = ByteBuffer.allocate(buflength);
                    FileChannel inChannel = raf.getChannel();

                    // copyLbl.setText("Upload:\n" + fname); // TODO: так выдает ошибку Not on FX application thread

                    copyBar.setVisible(true);

                    while ((endpos = inChannel.read(bb)) != -1) {   // raf.read(buffer)
                        String filedata = "file;" + fname + "#" + filesize + "#" + pos;
                        int toend = (int) (raf.length() - pos);
                        bb.flip();
                        if (toend < buflength) {
                            // byte[] minbuffer = Arrays.copyOf(bb, toend);
                            // filedata += "#" + byteArrayToHex(minbuffer, toend);
                            filedata += "#" + byteArrayToHex(bb, toend);
                        } else {
                            // filedata += "#" + byteArrayToHex(buffer);
                            filedata += "#" + byteArrayToHex(bb, buflength);
                        }
                        pos += endpos;
                        bb.clear();
                        sendCommand(filedata);
                        double progress = (double) pos / (double) filesize;
                        Platform.runLater(() -> {
                            copyLbl.setText("Upload:\n" + fname); // TODO: так ошибки нет
                            copyBar.setProgress(progress);
                        });
                    }
                    raf.close();
                    inChannel.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        copyTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                new EventHandler() {
                    @Override
                    public void handle(Event event) {
                        copyLbl.setText("");
                        copyBar.setVisible(false);
                    }
                });
        new Thread(copyTask).start();
    }

    // Конвертирует байтовый массив в hex представление byte[] a
    public static String byteArrayToHex(ByteBuffer a, int length) {
        // StringBuilder sb = new StringBuilder(a.length * 2);
        StringBuilder sb = new StringBuilder(length * 2);

        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", a.get(i)));
        }

        /*
        for (byte b : a) {
            sb.append(String.format("%02x", b));
        }

         */
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
                    clientBuffer.clear();
                    String[] instr = inmsg.split(";");
                    //System.out.println("Ответ " + inmsg);

                    // Аутентификация или регистрация
                    if ("auth_ok".startsWith(instr[0]) || "reg_ok".startsWith(instr[0])) {
                        authenticated = true;
                        userstoragesize = Long.parseLong(instr[1]);
                        authreg.setVisible(false);
                        //txtStorage.appendText("Пользоватетель:\n" + loginField.getText());
                        sendCommand("curpathls");
                    } else if ("auth_error".startsWith(instr[0])) {
                        errorWindow("Ошибка аутентификации!\nНеверный логин или пароль.");
                    } else if ("reg_error".startsWith(instr[0])) {
                        errorWindow("Ошибка регистрации!\nЛогин занят.");
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
                        } else if ("fileexist".startsWith(instr[0])) {
                            if (confirmWindow("Файл: '" + instr[1] + "' уже существует на сервере!", "Отправить по новой?") == ButtonType.OK) {
                                sendFileToServer(instr[1]);
                            }
                        } else if ("ready_dowmload".startsWith(instr[0])) {
                            sendFileToServer(instr[1]);
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
        // str содержит текущий путь, общий размер файлов и если есть список файлов/каталогов + емкость + дата изменения
        pathServer.setText(str[1]);
        userstoragetotalsize = Long.parseLong(str[2]);

        txtStorage.clear();
        txtStorage.appendText("Пользоватетель:\n" + loginField.getText());
        txtStorage.appendText(String.format("\n\nХранилище:\nСвободно %.2f Мб \nиз %d Мб", (userstoragesize - userstoragetotalsize) / 1048576, userstoragesize / 1048576));

        if (str.length < 4) {
            serverTable.getItems().clear();
            serverTable.getItems().add(new FileInfo("..", -1L, LocalDateTime.now()));
            return;
        }
        String[] filesinfo = str[3].split("#");
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

    @FXML
    private void authBtnAction(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        passwordField.clear();
        sendCommand("auth;" + login + ";" + password);
    }

    public void regBtnAction(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        passwordField.clear();
        sendCommand("reg;" + login + ";" + password);
    }

    public void cmRelogin(ActionEvent actionEvent) {
        pathServer.setText("");
        serverTable.getItems().clear();
        authreg.setVisible(true);
    }
}