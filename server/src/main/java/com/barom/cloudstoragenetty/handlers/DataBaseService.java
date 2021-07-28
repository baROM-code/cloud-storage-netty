package com.barom.cloudstoragenetty.handlers;

import java.sql.*;

public class DataBaseService {
    /*
        Сервис получения / внесения данных пользователей с использованием БД
     */

    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement prst;

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
            if (!rs.next()) {
                return false;
            }
            String passwordDB = rs.getString("password");
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
            return rs.getString("rootdir");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return "";
        } finally {
            disconnect();
        }
    }

    // Возвращает суммарный размер всех файлов пользователя
    public long getUserTotalSize(String login) throws SQLException {
        try {
            connect();
            ResultSet rs = stmt.executeQuery("SELECT totalsize FROM users WHERE login='" + login + "'");
            return rs.getLong("totalsize");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return 0L;
        } finally {
            disconnect();
        }
    }

    /* Добавление нового пользователя в БД
        вернет true - если логин свобен и пользователь добавлен в БД
     */
    public boolean addNewUser(String login, String password) {
        try {
            connect();
            /*
            ResultSet rs = stmt.executeQuery("SELECT login FROM users WHERE login='" + login + "'");
            if (!(rs == null)) {
                return false;
            }

             */
            prst = connection.prepareStatement("INSERT INTO users (login, password, nickname, rootdir) VALUES ( ? , ? , ? , ?);");
            prst.setString(1, login);
            prst.setString(2, password);
            prst.setString(3, login);
            prst.setString(4, login);
            prst.executeUpdate();
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } finally {
            disconnect();
        }
    }
}
