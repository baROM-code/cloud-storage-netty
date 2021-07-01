package com.barom.cloudstoragenetty.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientHahdler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // TODO 01.07.2021
        // Никак не могу записать полученный ответ от сервера на форму
        // т.е. получил  prompt а записать в @FXML public TextField pathServer не могу (((
        System.out.println(msg);
        String[] command = String.valueOf(msg).split(";");
        if ("prompt".equals(command[0])) {
            // controller.pathServer.setText(command[1]);
        } else if ("ls".equals(command[0])) {

        }
    }
}
