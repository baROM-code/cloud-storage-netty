package com.barom.cloudstoragenetty;

import java.nio.file.Path;

public class User {
    /*  Класс пользователь для хранения информации о подключенном пользователе
        содержит:
        Имя пользователя, Признак аутентификации,
        Корневой каталог, Текущий каталог, Количество байт занятых файлами
     */

    private String username;
    private boolean authenticated;
    private Path rootpath;
    private Path currentpath;
    private long totalsize;

    public User() {
        this.authenticated = false;
        this.username = "";
        this.totalsize = 0L;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
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

    public long getTotalsize() {
        return totalsize;
    }

    public void setTotalsize(long totalsize) {
        this.totalsize = totalsize;
    }
}
