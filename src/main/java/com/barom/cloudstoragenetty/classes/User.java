package com.barom.cloudstoragenetty.classes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class User {
    /* Класс пользователь содержит:
        Ник, Корневой каталог, Текущий каталог
        и методы для работы
     */

    private static final String serverrootdir = "server";

    private String nickname;
    private Path rootpath;
    private Path currentpath;

    public User(String nickname) {
        this.nickname = nickname;
        Path path = Paths.get(serverrootdir + File.separator + nickname).normalize();
        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
            this.rootpath = path;
            this.currentpath = path;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Path getCurrentpath() {
        return currentpath;
    }

    public String getNickname() {
        return nickname;
    }

    public Path getRootpath() {
        return rootpath;
    }

    public void setCurrentpath(Path currentpath) {
        this.currentpath = currentpath;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
