package com.example.nettydemo.channel.netty;

import com.example.nettydemo.channel.handler.ClientSocketHandler;
import com.example.nettydemo.channel.handler.DtuServiceHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
public class NettyClient {


    private EventLoopGroup eventLoopGroup;

    private Bootstrap bootstrap;

    private boolean stop;

    private String serverIp;

    private int serverPort;

    public NettyClient(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new IdleStateHandler(15, 0, 0, TimeUnit.MINUTES));
                    ch.pipeline().addLast(new DtuServiceHandler());
                }
            });

            // 连接到服务器
            ChannelFuture channelFuture = bootstrap.connect("localhost", 502).sync();

            // 获取Channel
            Channel channel = channelFuture.channel();

            byte[] message = new byte[]{0x00, 0x01, // 事务标识符
                    0x00, 0x00, // 协议标识符
                    0x00, 0x06, // 长度
                    0x01,       // 单元标识符
                    0x03,       // 功能码
                    0x00, 0x00, // 起始地址
                    0x00, 0x0A  // 寄存器数量
            };

            for (int i = 0; i < 10; i++) {
                channel.writeAndFlush(Unpooled.copiedBuffer(message));
                // Thread.sleep(1000);
            }
            // 发送数据
            // String data = "Hello, Netty!";
            // channel.writeAndFlush(Unpooled.copiedBuffer(message));

            // 等待连接关闭
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    // public static void main(String[] args) {
    //     NettyClient nettyClient = new NettyClient("localhost", 502);
    //     nettyClient.connect();
    // }

    public void stop() {
        this.stop = true;
    }

    public void connect() {
        this.eventLoopGroup = new NioEventLoopGroup();
        if (this.stop) {
            return;
        }

        try {
            this.bootstrap = new Bootstrap();
            this.bootstrap.group(this.eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    // 禁用Nagle算法
                    .option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline().addLast(new ClientSocketHandler());
                        }
                    });
            ChannelFuture future = this.bootstrap.connect(this.serverIp, this.serverPort).sync();
            future.addListener(f -> {
                if (f.isSuccess()) {
                    log.info("NettyClient successfully connected with server = {} and port = {}", NettyClient.this.serverIp, NettyClient.this.serverPort);
                } else {
                    log.error("NettyClient connecting to server = {} and port = {} failed.", NettyClient.this.serverIp, NettyClient.this.serverPort);
                }
            });
            // 等待客户端通知信息->关闭连接
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("NettyClient connect error", e);
        } finally {
            this.eventLoopGroup.shutdownGracefully();
            reconnect();
        }
    }

    private void reconnect() {
        try {
            this.eventLoopGroup = null;
            this.bootstrap = null;
            log.warn("NettyClient trying to connect to server");
            Thread.sleep(5 * 1000L);
            connect();
        } catch (Exception e) {
            log.error("NettyClient reconnect error", e);
        }
    }


}
