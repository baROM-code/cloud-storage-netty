package com.barom.cloudstoragenetty;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class IOCmd {

    /*
        Класс содержит методы для работы с файловой системой
     */

    // Возвращает список файлов и директорий в pathname
    public static String getFilesList(Path pathname) throws IOException {
        String[] files = new File(String.valueOf(pathname)).list();
        ArrayList<String> res = new ArrayList<>();
        for (String file : files) {
            Path path = Paths.get(pathname + File.separator + file);
            long size = Files.isDirectory(path) ? -1L : Files.size(path);
            res.add(path.getFileName().toString() + "!" +
                    size + "!" +
                    LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3)).toString()
            );
        }
        return String.join("#", res);
    }

    // Создание директории dirname в pathname
    public static String mkDir(Path pathname, String dirname) {
        Path path = Paths.get(pathname + File.separator + dirname).normalize();
        try {
            Files.createDirectory(path);
        } catch(FileAlreadyExistsException e){
            return "ERROR; Каталог уже существует!";
        } catch (IOException e) {
            return "ERROR; IOException";
        }
        return "OK";
    }

    // Создание файла
    public static String crFile(Path pathname, String filename) {
        Path path = Paths.get(pathname + File.separator + filename).normalize();
        try {
            Files.createFile(path);
        } catch(FileAlreadyExistsException e){
            return "ERROR: File already exists";
        } catch (IOException e) {
            return "ERROR: IOException";
        }
        return "Created file: " + path;
    }

    // Удаление файла или пустой директории
    public static String delFileOrDir(Path pathname, String fileordirname) {
        Path path = Paths.get(pathname + File.separator + fileordirname).normalize();
        if (!Files.exists(path)) {
            return "ERROR; Файл/каталог не существует!";
        } else try {
            Files.delete(path);
            return "removed";
        } catch (IOException e) {
            return "ERROR; Не удалось удалить не пустой каталог!";
        }
    }

    // Копирование файла / директории
    public static String copyFileOrDir(Path pathname, String srcPath, String dstPath) {
        Path sourcePath      = Paths.get(pathname + File.separator + srcPath).normalize();
        Path destinationPath = Paths.get(pathname + File.separator + dstPath).normalize();
        try {
            Files.copy(sourcePath, destinationPath);
            return sourcePath + " copied to " + destinationPath;
        } catch(FileAlreadyExistsException e) {
            return "ERROR: The destination file / directory already exist!";
        } catch (IOException e) {
            return "ERROR: IOException";
        }
    }

    // Вывод содержимого текстового файла
    public static String catFile(Path pathname, String filename) {
        Path path = Paths.get(pathname + File.separator + filename).normalize();
        if (!Files.exists(path)) {
            return "ERROR: The file does not exist!";
        }
        List<String> lines = null;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "ERROR: IOException";
        }
        lines.add("");
        return String.join("\n\r", lines);
    }

}
