package com.example.nettydemo.channel.handler;

import com.example.nettydemo.channel.netty.NettyClient;
import com.example.nettydemo.common.AccessType;
import com.example.nettydemo.common.GlobalVariables;
import com.example.nettydemo.devices.dtu.ServerDtu;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

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

        boolean foundDtu = false;
        for (ServerDtu serverDtu : GlobalVariables.GLOBAL_SERVER_DTU_LIST) {
            log.info("Socket handler begin check server dtu ip = " + serverDtu.getIpAddress() + " port = " + serverDtu.getPort());
            if (serverDtu.getAccessType() != null && serverDtu.getAccessType().equals(AccessType.TCP_CLIENT) && serverDtu
                    .getIpAddress().equals(ip) && serverDtu.getPort().equals(port)) {
                foundDtu = true;
                // 如果是TCP客户端，那么就需要将原来的连接关闭,避免连接重复
                ChannelHandlerContext origCtx = GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.get(serverDtu.getSn());
                if (origCtx != null) {
                    if (!origCtx.isRemoved()) {
                        log.info("开始关闭原来的连接，通道信息为,ctx id :{}", origCtx.channel().id());
                        origCtx.close();
                    }
                    disconnectDevice(origCtx);
                }
                serverDtu.setIpAddress(ip);
                serverDtu.setPort(port);
                GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.put(connectKey, serverDtu);
                GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.put(serverDtu.getSn(), ctx);
                log.info("创建了新的链接，当前的链接信息为：ctx:{},{}", ctx.channel().id(),ctx.channel().isActive());
                serverDtu.setChannelHandlerContext(ctx);
                break;
            }
        }
        if (!foundDtu) {
            log.warn("Socket handler can't find serverDtu configuration of ip = " + ip + " and port = " + port);
            log.info("未找到dtu，开始关闭原来的连接，通道信息为,ctx id :{}", ctx.channel().id());
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
        log.error("Channel id: " + ctx.channel().id());
        log.error("Channel active: " + ctx.channel().isActive());
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
            ChannelHandlerContext ctxLocal = dtu.getCtxLocal();
            if (!ctxLocal.channel().isActive()){
                GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.remove(dtu.getSn());
                dtu.onDisconnected();
            }
        }
        GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.remove(connectKey);
    }
}
