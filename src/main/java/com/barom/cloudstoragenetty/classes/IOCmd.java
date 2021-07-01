package com.barom.cloudstoragenetty.classes;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class IOCmd {

    /*
        Класс содержит методы для работы с файловой системой
     */

    // Возвращает список файлов и директорий в pathname
    public static String getFilesList(Path pathname) {
        String[] servers = new File(String.valueOf(pathname)).list();
        return String.join(" ", servers);
    }

    // Создание директории dirname в pathname
    public static String mkDir(Path pathname, String dirname) {
        Path path = Paths.get(pathname + File.separator + dirname).normalize();
        try {
            Files.createDirectory(path);
        } catch(FileAlreadyExistsException e){
            return "ERROR: Directory Already Exists\n\r";
        } catch (IOException e) {
            return "ERROR: IOException\n\r";
        }
        return "Created directory: " + path + "\n\r";
    }

    // Создание файла
    public static String crFile(Path pathname, String filename) {
        Path path = Paths.get(pathname + File.separator + filename).normalize();
        try {
            Files.createFile(path);
        } catch(FileAlreadyExistsException e){
            return "ERROR: File already exists\n\r";
        } catch (IOException e) {
            return "ERROR: IOException\n\r";
        }
        return "Created file: " + path + "\n\r";
    }

    // Удаление файла или пустой директории
    public static String delFileOrDir(Path pathname, String fileordirname) {
        Path path = Paths.get(pathname + File.separator + fileordirname).normalize();
        if (!Files.exists(path)) {
            return "ERROR: The file / directory does not exist!\n\r";
        } else try {
            Files.delete(path);
            return fileordirname + " removed\n\r";
        } catch (IOException e) {
            return "ERROR: Failed to delete the file or directory is not empty!\n\r";
        }
    }

    // Копирование файла / директории
    public static String copyFileOrDir(Path pathname, String srcPath, String dstPath) {
        Path sourcePath      = Paths.get(pathname + File.separator + srcPath).normalize();
        Path destinationPath = Paths.get(pathname + File.separator + dstPath).normalize();
        try {
            Files.copy(sourcePath, destinationPath);
            return sourcePath + " copied to " + destinationPath + "\n\r";
        } catch(FileAlreadyExistsException e) {
            return "ERROR: The destination file / directory already exist!\n\r";
        } catch (IOException e) {
            return "ERROR: IOException\n\r";
        }
    }

    // Вывод содержимого текстового файла
    public static String catFile(Path pathname, String filename) {
        Path path = Paths.get(pathname + File.separator + filename).normalize();
        if (!Files.exists(path)) {
            return "ERROR: The file does not exist!\n\r";
        }
        List<String> lines = null;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "ERROR: IOException\n\r";
        }
        lines.add("");
        return String.join("\n\r", lines);
    }

}
