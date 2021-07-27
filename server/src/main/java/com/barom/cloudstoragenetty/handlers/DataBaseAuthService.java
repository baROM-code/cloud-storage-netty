package com.barom.cloudstoragenetty.handlers;

import java.sql.*;

public class DataBaseAuthService {
    /*
        Сервис авторизации пользователя с использованием БД
     */

    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement psInsert;

    // подключение к БД
    private static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:users.db");
        stmt = connection.createStatement();
    }

    // отключение от БД
    private static void disconnect() {
        try {
            stmt.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    // Проверка соотвествия пары Логин, Пароль
    public boolean isUserLoginPasswordRight(String login, String password) {
        try {
            connect();
            ResultSet rs = stmt.executeQuery("SELECT password FROM users WHERE login='" + login + "'");
            if (rs == null) {
                return false;
            }
            String passwordDB = rs.getString("password");
            System.out.println(passwordDB);
            if (passwordDB.equals(password)) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } finally {
            disconnect();
        }
        return false;
    }

    // Возвращает корневую директорию пользователя
    public String getUserRootDir(String login) throws SQLException {
        try {
            connect();
            ResultSet rs = stmt.executeQuery("SELECT rootdir FROM users WHERE login='" + login + "'");
            if (rs == null) {
                return "";
            }
            return rs.getString("rootdir");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return "";
        } finally {
            disconnect();
        }
    }

    /* Добавление нового сользователя в БД
        вернет true - если логин свобен и пользователь добавлен в БД
     */
    public boolean addNewUser(String login, String password) {
        try {
            connect();
            ResultSet rs = stmt.executeQuery("SELECT login FROM users WHERE login='" + login + "'");
            if (rs == null) {
                return false;
            }
            psInsert = connection.prepareStatement("INSERT INTO users (login, password, nickname, rootdir) VALUES ( ? , ? , ?);");
            psInsert.setString(1, login);
            psInsert.setString(2, password);
            psInsert.setString(3, login);
            psInsert.setString(4, login);
            psInsert.executeUpdate();
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } finally {
            disconnect();
        }
    }
}
