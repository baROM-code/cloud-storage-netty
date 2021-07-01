package com.barom.cloudstoragenetty.controllers;

import com.barom.cloudstoragenetty.handlers.ClientHahdler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class NettyClient {

    private Channel channel;

    public NettyClient() {
        String server = "localhost";
        int port = 5000;

        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap().group(worker)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast(
                                    new StringDecoder(), // in - 1
                                    new StringEncoder(), // out - 1
                                    new ClientHahdler() {}// in - 2
                            );
                        }
                    });

            channel = bootstrap.connect(server, port).sync().channel();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //worker.shutdownGracefully();
        }
    }

    public void sendCmd (String cmd) {
        channel.writeAndFlush(cmd);
        channel.flush();
    }

    public void exit() {
        channel.close();
    }


}
