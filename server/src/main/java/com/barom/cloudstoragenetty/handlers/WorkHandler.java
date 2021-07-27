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
        System.setProperty("java.util.logging.FileHandler.append", "true");
        try {
            logger.setUseParentHandlers(false);
            FileHandler filehandler = new FileHandler("cloud-storage.log");
            filehandler.setFormatter(new SimpleFormatter());
            filehandler.setLevel(Level.INFO);
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.WARNING);
            logger.addHandler(filehandler);
            logger.addHandler(consoleHandler);
            logger.info("Server started");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String serverrootdir = "storage";
    private static Map<ChannelId, User> clients = new HashMap<>();
    private DataBaseAuthService dataBaseAuthService = new DataBaseAuthService();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client connected: " + ctx.channel());
        User newuser = new User();
        clients.put(ctx.channel().id(), newuser);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client disconnected: " + ctx.channel() + "\n");
        clients.remove(ctx.channel().id());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Path currentpath;
        String[] command = String.valueOf(msg)
                .replace("\n", "")
                .replace("\r", "")
                .split(";");
        logger.info("in message command: " + msg + "  from client:" + clients.get(ctx.channel().id()).getUsername());

        if ("auth".startsWith(command[0])) {
            System.out.println("****");
            if (dataBaseAuthService.isUserLoginPasswordRight(command[1], command[2])) {
                clients.get(ctx.channel().id()).setUsername(command[1]);
                clients.get(ctx.channel().id()).setRootpath(Paths.get(serverrootdir, dataBaseAuthService.getUserRootDir(command[1])));
                clients.get(ctx.channel().id()).setCurrentpath(Paths.get(serverrootdir, dataBaseAuthService.getUserRootDir(command[1])));
                clients.get(ctx.channel().id()).setAuthorized(true);
                logger.info("Client:" + command[1] + " - authorized");
                ctx.writeAndFlush("auth_ok\n");
            } else {
                clients.get(ctx.channel().id()).setAuthorized(false);
                ctx.writeAndFlush("auth_error\n");
            }
        }

        if (clients.get(ctx.channel().id()).isAuthorized()) {

            currentpath = clients.get(ctx.channel().id()).getCurrentpath(); // Получим текущий путь для клиента

            // StringBuilder strbuilder = new StringBuilder();
            /*
            while (buf.isReadable()) {
                strbuilder.append((char)buf.readByte());
            } */
            // String str = msg.toString();
            // ctx.fireChannelRead(builder.toString()); // отправка дальше по очереди

            // String s= ((ByteBuf) msg).toString(CharsetUtil.UTF_8);
            String outmsg = "";

            // отправляем клиенту текущий путь и список файлов/каталогов
            if ("curpathls".startsWith(command[0])) { // возвращает текущий путь клиента и список файлов/каталогов
                ctx.writeAndFlush("curpathls;" + getCurPath(ctx.channel().id()) + ";" + IOCmd.getFilesList(currentpath) + "\n");
            } else if ("mkdir".startsWith(command[0])) {
                outmsg = IOCmd.mkDir(currentpath, command[1]);
                if ("OK".startsWith(outmsg)) {
                    ctx.writeAndFlush("curpathls;" + getCurPath(ctx.channel().id()) + ";" + IOCmd.getFilesList(currentpath) + "\n");
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
                    ctx.writeAndFlush("curpathls;" + getCurPath(ctx.channel().id()) + ";" + IOCmd.getFilesList(currentpath) + "\n");
                }
            } else if ("delete".startsWith(command[0])) {
                outmsg = IOCmd.delFileOrDir(currentpath, command[1]);
                if ("removed".startsWith(outmsg)) {
                    ctx.writeAndFlush("curpathls;" + getCurPath(ctx.channel().id()) + ";" + IOCmd.getFilesList(currentpath) + "\n");
                } else {
                    ctx.writeAndFlush(outmsg + "\n");
                }
            } else if ("download".startsWith(command[0])) {
                /*
                try (FileChannel fileChannel = FileChannel.open(Paths.get(String.valueOf(currentpath), command[1]), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    fileChannel.transferFrom((ReadableByteChannel) ctx, 0, Long.parseLong(command[2]));
                }
                */
            } else if ("file".startsWith(command[0])) {
                saveFileSegment(String.valueOf(currentpath), command[1], ctx);
            } else if ("disconnect".startsWith(command[0])) {
                ctx.disconnect();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // cause.printStackTrace();
        ctx.close();
        logger.log(Level.WARNING, "WorkHandler exception", cause);
    }

    private boolean cdCommand(String newpath, ChannelId client) {
        Path path = Paths.get(clients.get(client).getCurrentpath() + File.separator + newpath).normalize();
        // Current path is root! (path no change)
        if ("..".equals(newpath) & clients.get(client).getCurrentpath() == clients.get(client).getRootpath()) {
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
                System.out.println("File: " + filename + "  downloaded to server");
                ctx.writeAndFlush("curpathls;" + getCurPath(ctx.channel().id()) + ";" + IOCmd.getFilesList(Paths.get(path)) + "\n");
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
