package com.example.nettydemo.devices.dtu;

import com.example.nettydemo.common.AccessType;
import com.example.nettydemo.common.FrameType;
import com.example.nettydemo.common.GlobalVariables;
import com.example.nettydemo.devices.BaseDevice;
import com.example.nettydemo.devices.business.BaseBusinessDevice;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: Qbss
 * @date: 2023/11/30
 * @desc:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ServerDtu extends BaseDevice {

    // 通道类型
    private AccessType accessType;

    private String ipAddress;

    private Integer port;

    // 设备下标序号
    private Integer deviceIndex;

    // 通道下设备集合
    private List<BaseBusinessDevice> devices;

    private ChannelHandlerContext ctxLocal;

    // 业务执行线程
    private ScheduledFuture<?> businessTaskFuture;

    private ReentrantLock deviceIndexLock;

    public void onDisconnected() {

    }

    public void setChannelHandlerContext(ChannelHandlerContext ctx) {
        // 如果当前通道已经存在，那么就关闭当前通道
        if (this.ctxLocal != null && !this.ctxLocal.isRemoved()) {
            this.ctxLocal.close();
        }

        this.ctxLocal = ctx;

        // 如果当前采集器的线程池中的任务还没有执行完毕，那么就取消当前任务
        if (this.businessTaskFuture != null) {
            this.businessTaskFuture.cancel(true);
            this.businessTaskFuture = null;
        }

        this.devices.forEach(BaseBusinessDevice::onConnected);

        this.businessTaskFuture = GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL
                .scheduleWithFixedDelay(() -> {
                    try {
                        // 调用采集周期函数
                        this.dispatch();
                    } catch (Exception e) {
                        log.warn("businessTaskFuture error", e);
                    }
                    // 60s采集一次当下
                }, 0L, 60 * 1000L, TimeUnit.MILLISECONDS);
    }

    // 数据处理函数
    public void dispatch() {

        if (this.devices.isEmpty()) {
            return;
        }

        // 获取当下需要采集到设备
        BaseBusinessDevice device = this.devices.get(this.deviceIndex);

        device.doParse();

        if (device.isHasDataToSend()) {
            while (!device.getOutputBytesBuffer().isEmpty()) {
                Pair<FrameType, byte[]> poll = device.getOutputBytesBuffer().poll();
                if (poll != null) {
                    // 帧类型
                    FrameType frameType = poll.getValue0();
                    // 需要发送的数据
                    byte[] outBuf = poll.getValue1();

                    StringBuilder hexStr = new StringBuilder();
                    for (byte b : outBuf) {
                        hexStr.append(String.format("%02X ", b));
                    }

                    log.info("ServerDtu dispatch: {}", hexStr);

                    // 考虑保存下指令记录

                    // 判断下通道依旧存在着
                    if (this.ctxLocal != null && !this.ctxLocal.isRemoved()) {

                        // 发送报文
                        ByteBuf sendBuf = this.ctxLocal.alloc().buffer(outBuf.length);
                        sendBuf.writeBytes(outBuf);

                        ChannelFuture channelFuture = this.ctxLocal.writeAndFlush(sendBuf);

                        // 添加发送报文监听
                        channelFuture.addListener(future -> {
                            if (future.isSuccess()) {
                                log.info("ServerDtu dispatch success");
                            }
                            // 记录发送信息，不管是否成功
                            device.recordSendInfo(frameType);
                        });
                    }
                }
            }
            device.setHasDataToSend(false);
        }

        // 设备业务处理
        device.doBusiness();
        // 判断是否可以切到下一个设备
        if (device.isCanSwitchToNext()) {
            // 获取锁
            lockDeviceLock();
            try {
                this.deviceIndex++;
                if (this.deviceIndex == this.devices.size()) {
                    this.deviceIndex = 0;
                }
            } finally {
                this.deviceIndexLock.unlock();
            }
        }
    }

    public void handleReceivedData(byte[] frameBytes) {
        if (this.devices.isEmpty()) {
            return;
        }
        BaseBusinessDevice device = this.devices.get(this.deviceIndex);
        if (device == null) {
            return;
        }
        device.addInputBytesBuffer(frameBytes);
        // 离线时间 暂定
        device.setLastReceivedTime(Instant.now());
    }

    // 保证切换设备的时候，只有一个线程在操作
    private void lockDeviceLock() {
        // 如果当前线程已经获取到锁，那么就直接返回
        while (!this.deviceIndexLock.tryLock()) {
            try {
                Thread.sleep(100L);
            } catch (Exception e) {
                log.error("lockDeviceLock error", e);
                break;
            }
        }
    }
}
