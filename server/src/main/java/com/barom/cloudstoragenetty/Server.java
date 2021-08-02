package com.barom.cloudstoragenetty;

import com.barom.cloudstoragenetty.handlers.WorkHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

import java.nio.file.Paths;

public class Server {

    public Server() {

        IOCmd.mkDir(Paths.get("."), "storage");

        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128) //  количество очередей подключений
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            // Разделитель
                            ByteBuf delimiter = Unpooled.copiedBuffer("$>".getBytes());
                            channel.pipeline().addLast(
                                    new DelimiterBasedFrameDecoder(102400, delimiter), // декодер-разделитель
                                    new StringDecoder(CharsetUtil.UTF_8),
                                    new StringEncoder(CharsetUtil.UTF_8),
                                    new WorkHandler()
                            );
                        }
                    });
            ChannelFuture future = bootstrap.bind(5000).sync();
            System.out.println("Server started");
            future.channel().closeFuture().sync();
            System.out.println("Server finished");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            auth.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}
