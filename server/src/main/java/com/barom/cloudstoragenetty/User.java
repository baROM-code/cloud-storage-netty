package com.barom.cloudstoragenetty;

import java.nio.file.Path;

public class User {
    /*  Класс пользователь для хранения информации о подключенном пользователе
        содержит:
        Имя пользователя, Признак авторизации, Корневой каталог, Текущий каталог
     */

    private String username;
    private boolean authorized;
    private Path rootpath;
    private Path currentpath;

    public User() {
        this.authorized = false;
        this.username = "";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public Path getRootpath() {
        return rootpath;
    }

    public void setRootpath(Path rootpath) {
        this.rootpath = rootpath;
    }

    public Path getCurrentpath() {
        return currentpath;
    }

    public void setCurrentpath(Path currentpath) {
        this.currentpath = currentpath;
    }
}
