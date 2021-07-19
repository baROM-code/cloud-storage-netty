package com.barom.cloudstoragenetty.handlers;

import com.barom.cloudstoragenetty.IOCmd;
import com.barom.cloudstoragenetty.User;
import io.netty.channel.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CommandHandler extends ChannelInboundHandlerAdapter {

    private static Map<ChannelId, User> clients = new HashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected: " + ctx.channel() + "   user" + clients.size());
        clients.put(ctx.channel().id(), new User("user" + clients.size()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected: " + ctx.channel());
        clients.remove(ctx.channel().id());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Path currentpath = clients.get(ctx.channel().id()).getCurrentpath(); // Получим текущий путь для клиента
        String outmsg = "";
        String[] command = String.valueOf(msg)
                .trim()
                .replace("\n", "")
                .replace("\r", "")
                .split(";");
        if ("curpathls".equals(command[0])) { // возвращает текущий путь клиента и список файлов/каталогов
            ctx.writeAndFlush("curpathls;" + getCurPath(ctx.channel().id()) + ";" + IOCmd.getFilesList(currentpath) + "\n");
        } else if ("mkdir".equals(command[0])) {
            outmsg = IOCmd.mkDir(currentpath, command[1]);
            if ("OK".startsWith(outmsg)) {
                ctx.writeAndFlush("curpathls;" + getCurPath(ctx.channel().id()) + ";" + IOCmd.getFilesList(currentpath) + "\n");
            } else {
                ctx.writeAndFlush(outmsg + "\n");
            }
        } else if ("upload".equals(command[0])) {
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
            ctx.writeAndFlush("file uploaded\n");
        } else if ("cd".equals(command[0])) {
            if (cdCommand(command[1], ctx.channel().id())) {
                currentpath = clients.get(ctx.channel().id()).getCurrentpath();
                ctx.writeAndFlush("curpathls;" + getCurPath(ctx.channel().id()) + ";" + IOCmd.getFilesList(currentpath) + "\n");
            }
        } else if ("rm".equals(command[0])) {
            // outmsg = IOCmd.delFileOrDir(currentpath, command[1]);
        } else if ("copy".equals(command[0])) {
            // outmsg = IOCmd.copyFileOrDir(currentpath, command[1], command[2]);
        } else if ("cat".equals(command[0])) {
            // outmsg = IOCmd.catFile(currentpath, command[1]);
        } else if ("changenick".equals(command[0])) {
            changenickCommand(command[1], ctx.channel().id());
        }
        System.out.println(currentpath);
        System.out.println(msg);
    }

    private boolean cdCommand(String newpath, ChannelId client) {
        Path path = Paths.get(clients.get(client).getCurrentpath() + File.separator + newpath).normalize();
        // Current path is root! (path no change)
        if ("..".equals(newpath) & clients.get(client).getCurrentpath() == clients.get(client).getRootpath())  {
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
            return false ;
        }
    }

    private String getCurPath(ChannelId client){
        // текущий путь>
        String res = String.valueOf(clients.get(client).getCurrentpath()).replace(String.valueOf(clients.get(client).getRootpath()), "~");
        return res;
    }

    private void changenickCommand(String newnick, ChannelId client) {
        clients.get(client).setNickname(newnick);
    }

}
