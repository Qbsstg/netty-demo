package com.example.nettydemo.channel.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DtuServiceHandler extends ChannelInboundHandlerAdapter {

    // 保存
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    // 保存客户端channelId与注册包信息
    private static Map<String, String> channelIdMap = new ConcurrentHashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        ByteBuf byteBuf = (ByteBuf) msg;
        byte[] frameBytes = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(0, frameBytes);

        StringBuilder hexString = new StringBuilder();
        for (byte b : frameBytes) {
            hexString.append(String.format("0x%02X ", b));
        }

        System.out.println("收到的数据（16进制）：" + hexString);
        System.out.println("收到的数据（10进制）：" + Arrays.toString(frameBytes));
        System.out.println("链接数：" + channelGroup.size());
        System.out.println("注册包数：" + channelIdMap.size());
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ChannelId channelId = channel.id();
        channelIdMap.remove(channelId.asLongText());
        System.out.println(channelId.asLongText() + " channelInactive客户端关闭连接");
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // 连接建立
        Channel channel = ctx.channel();
        channelGroup.add(channel);
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ChannelId channelId = channel.id();
        channelIdMap.remove(channelId.asLongText());
        System.out.println(channelId.asLongText() + " ChannelHandlerContext客户端关闭连接");
        super.handlerRemoved(ctx);
    }
}
