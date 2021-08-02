package com.barom.cloudstoragenetty.handlers;

import com.barom.cloudstoragenetty.*;
import io.netty.channel.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

public class WorkHandler extends ChannelInboundHandlerAdapter {

    public static final Logger logger = Logger.getLogger(WorkHandler.class.getName());

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] %4$s: %5$s %n");
        try {
            logger.setUseParentHandlers(false);
            FileHandler filehandler = new FileHandler("cloud-storage.log");
            filehandler.setFormatter(new SimpleFormatter());
            filehandler.setLevel(Level.INFO);
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.WARNING);
            System.setProperty("java.util.logging.FileHandler.append", "true");
            logger.addHandler(filehandler);
            logger.addHandler(consoleHandler);
            logger.info("Server started");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String serverrootdir = "storage";
    private static final long userstoragesize = 100 * 1024 * 1024;
    private static Map<ChannelId, User> clients = new HashMap<>();
    private DataBaseService dataBaseService = new DataBaseService();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client connected: " + ctx.channel());
        clients.put(ctx.channel().id(), new User());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client disconnected: " + ctx.channel() + "\n");
        clients.remove(ctx.channel().id());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Path currentpath = null;
        String[] command = String.valueOf(msg)
                .replace("\n", "")
                .replace("\r", "")
                .split(";");
        String log = String.valueOf(msg);
        int p = log.indexOf("#");
        if (p > 0) {
            log = log.substring(0, p);
        }
        logger.info("in message command: " + log + "  from client:" + clients.get(ctx.channel().id()).getUsername());

        if ("auth".startsWith(command[0])) {
            if (dataBaseService.isUserLoginPasswordRight(command[1], command[2])) {
                clients.get(ctx.channel().id()).setUsername(command[1]);
                clients.get(ctx.channel().id()).setRootpath(Paths.get(serverrootdir, dataBaseService.getUserRootDir(command[1])));
                clients.get(ctx.channel().id()).setCurrentpath(Paths.get(serverrootdir, dataBaseService.getUserRootDir(command[1])));
                clients.get(ctx.channel().id()).setTotalsize(dataBaseService.getUserTotalSize(command[1]));
                clients.get(ctx.channel().id()).setAuthenticated(true);
                logger.info("Client:" + command[1] + " - authenticated");
                ctx.writeAndFlush("auth_ok;" + userstoragesize + "\n");
            } else {
                clients.get(ctx.channel().id()).setAuthenticated(false);
                ctx.writeAndFlush("auth_error\n");
            }
        } else if ("reg".startsWith(command[0])) {
            if ("OK".startsWith(IOCmd.mkDir(Paths.get(serverrootdir), command[1]))) {
                if (dataBaseService.addNewUser(command[1], command[2])) {
                    clients.get(ctx.channel().id()).setUsername(command[1]);
                    clients.get(ctx.channel().id()).setRootpath(Paths.get(serverrootdir, dataBaseService.getUserRootDir(command[1])));
                    clients.get(ctx.channel().id()).setCurrentpath(Paths.get(serverrootdir, dataBaseService.getUserRootDir(command[1])));
                    clients.get(ctx.channel().id()).setTotalsize(dataBaseService.getUserTotalSize(command[1]));
                    clients.get(ctx.channel().id()).setAuthenticated(true);
                    logger.info("New client:" + command[1] + " - registered");
                    ctx.writeAndFlush("reg_ok;" + userstoragesize + "\n");
                } else {
                    clients.get(ctx.channel().id()).setAuthenticated(false);
                    ctx.writeAndFlush("reg_error\n");
                }
            } else {
                clients.get(ctx.channel().id()).setAuthenticated(false);
                ctx.writeAndFlush("reg_error\n");
            }
        }

        if (clients.get(ctx.channel().id()).isAuthenticated()) {

            currentpath = clients.get(ctx.channel().id()).getCurrentpath(); // Получим текущий путь для клиента

            String outmsg = "";

            if ("curpathls".startsWith(command[0])) {
                sendCurpathLs(ctx, currentpath);
            } else if ("mkdir".startsWith(command[0])) {
                outmsg = IOCmd.mkDir(currentpath, command[1]);
                if ("OK".startsWith(outmsg)) {
                    sendCurpathLs(ctx, currentpath);
                } else {
                    ctx.writeAndFlush(outmsg + "\n");
                }
                // отправка файла клиенту
            } else if ("upload".startsWith(command[0])) {
                RandomAccessFile raf = null;
                long length = -1;
                try {
                    raf = new RandomAccessFile(currentpath + File.separator + command[1], "r");
                    length = raf.length();
                } catch (Exception e) {
                    ctx.writeAndFlush("ERROR: " + e.getClass().getSimpleName() + " " + e.getMessage() + "\n");
                    return;
                } finally {
                    if (length < 0 && raf != null) {
                        raf.close();
                    }
                }
                ctx.write("sendfile;" + command[1] + ";" + raf.length() + "\n");
                ctx.write(new DefaultFileRegion(raf.getChannel(), 0, length));
                ctx.flush();
                ctx.writeAndFlush("file_uploaded\n");
            } else if ("cd".startsWith(command[0])) {
                if (cdCommand(command[1], ctx.channel().id())) {
                    currentpath = clients.get(ctx.channel().id()).getCurrentpath();
                    sendCurpathLs(ctx, currentpath);
                }
            } else if ("delete".startsWith(command[0])) {
                long sz = Files.size(Paths.get(currentpath + File.separator + command[1]));
                outmsg = IOCmd.delFileOrDir(currentpath, command[1]);
                if ("removed".startsWith(outmsg)) {
                    clients.get(ctx.channel().id()).setTotalsize(clients.get(ctx.channel().id()).getTotalsize() - sz);
                    dataBaseService.saveTotalSize(clients.get(ctx.channel().id()).getUsername(), clients.get(ctx.channel().id()).getTotalsize());
                    sendCurpathLs(ctx, currentpath);
                } else {
                    ctx.writeAndFlush(outmsg + "\n");
                }
            } else if ("download".startsWith(command[0])) {
                if (Files.exists(Paths.get(currentpath + File.separator + command[1]))) {
                    ctx.writeAndFlush("fileexist;" + command[1] + "\n");
                } else {
                    ctx.writeAndFlush("ready_dowmload;" + command[1] + "\n");
                }
            }
                /*
                try (FileChannel fileChannel = FileChannel.open(Paths.get(String.valueOf(currentpath), command[1]), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    fileChannel.transferFrom((ReadableByteChannel) ctx, 0, Long.parseLong(command[2]));
                }
                */
            else if ("file".startsWith(command[0])) {
                saveFileSegment(String.valueOf(currentpath), command[1], ctx);
            } else if ("disconnect".startsWith(command[0])) {
                ctx.disconnect();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warning("WorkHandler exception" + cause.toString());
        ctx.close();
    }

    // отправляем клиенту текущий путь; общий размер файлов; список файлов/каталогов (если есть)
    private void sendCurpathLs(ChannelHandlerContext ctx, Path path) {
        try {
            ctx.writeAndFlush("curpathls;" + getCurPath(ctx.channel().id()) +
                    ";" + clients.get(ctx.channel().id()).getTotalsize() +
                    ";" + IOCmd.getFilesList(path) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean cdCommand(String newpath, ChannelId client) {
        Path path = Paths.get(clients.get(client).getCurrentpath() + File.separator + newpath).normalize();
        // Current path is root! (path no change)
        if ("..".equals(newpath) & (clients.get(client).getCurrentpath() == clients.get(client).getRootpath())) {
            return false;
        }
        if ("~".equals(newpath)) {
            clients.get(client).setCurrentpath(clients.get(client).getRootpath());
            return true;
        }
        if (Files.exists(path)) {
            clients.get(client).setCurrentpath(path);
            return true;
        } else {
            // ERROR: The path does not exist!
            return false;
        }
    }

    private String getCurPath(ChannelId client) {
        // текущий путь c заменой userrootpath на ~
        String res = String.valueOf(clients.get(client).getCurrentpath()).replace(String.valueOf(clients.get(client).getRootpath()), "~");
        return res;
    }

    private void saveFileSegment(String path, String data, ChannelHandlerContext ctx) {
        String[] filedata = data.split("#");
        if (filedata.length < 3) {
            return;
        }
        String filename = filedata[0];
        long filesize = Long.parseLong(filedata[1]);
        long pos = Long.parseLong(filedata[2]);

        File file = new File(path + File.separator + filename);
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(pos);
            raf.write(hexStringToByteArray(filedata[3]));
            // raf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (Files.size(Paths.get(path, filename)) == filesize) {
                logger.info("File: " + filename + " downloaded to server");
                // прибавим размер файла к тотал
                clients.get(ctx.channel().id()).setTotalsize(clients.get(ctx.channel().id()).getTotalsize() + filesize);
                dataBaseService.saveTotalSize(clients.get(ctx.channel().id()).getUsername(), clients.get(ctx.channel().id()).getTotalsize());
                sendCurpathLs(ctx, Paths.get(path));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Преобразовывает строку в шестнадцатеричном представлении в массив байтов
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
