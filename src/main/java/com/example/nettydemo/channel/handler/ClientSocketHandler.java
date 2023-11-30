package com.example.nettydemo.channel.handler;

import com.example.nettydemo.common.AccessType;
import com.example.nettydemo.common.GlobalVariables;
import com.example.nettydemo.devices.dtu.ServerDtu;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * @author: Qbss
 * @date: 2023/11/30
 * @desc:
 */
@Component
@Slf4j
public class ClientSocketHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String ip = socketAddress.getHostString();
        int port = socketAddress.getPort();

        log.info("Socket handler " + ip + ":" + port + " connected.");

        String connectKey = ip + ":" + port;
        ServerDtu dtu = GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.get(connectKey);

        if ((dtu != null)
                && (dtu.getAccessType().equals(AccessType.TCP_CLIENT))
                && (dtu.getIpAddress().equals(ip) && dtu.getPort().equals(port))) {
            // 如果是TCP客户端，那么就需要将原来的连接关闭,避免连接重复
            ChannelHandlerContext origCtx = GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.get(dtu.getSn());
            if (origCtx != null) {
                if (!origCtx.isRemoved()) {
                    origCtx.close();
                }
                disconnectDevice(origCtx);
            }
            dtu.setIpAddress(ip);
            dtu.setPort(port);
            GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.put(connectKey, dtu);
            GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.put(dtu.getSn(), ctx);
            dtu.setChannelHandlerContext(ctx);

        } else {
            log.warn("Socket handler can't find dtu configuration of ip = " + ip + " and port = " + port);
            ctx.close();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buff = (ByteBuf) msg;
        byte[] bytes = new byte[buff.readableBytes()];
        buff.getBytes(0, bytes);
        buff.release();

        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String ip = socketAddress.getHostString();
        int port = socketAddress.getPort();
        String connectKey = ip + ":" + port;

        ServerDtu serverDtu = GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.get(connectKey);
        serverDtu.handleReceivedData(bytes);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.error("Socket handler " + ctx.channel().remoteAddress() + " disconnected.");
        disconnectDevice(ctx);
        ctx.close();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Socket handler " + ctx.channel().remoteAddress() + " exception: " + cause.getMessage());
        disconnectDevice(ctx);
        ctx.close();
    }

    // 关闭连接的同时，将对应采集器下所有的设备在线状态置为 离线
    private void disconnectDevice(ChannelHandlerContext ctx) {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String ip = socketAddress.getHostString();
        int port = socketAddress.getPort();

        String connectKey = ip + ":" + port;
        ServerDtu dtu = GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.get(connectKey);
        if (dtu != null) {
            GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.remove(dtu.getSn());
            dtu.onDisconnected();
        }
        GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.remove(connectKey);
    }
}
