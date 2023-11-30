package com.example.nettydemo.devices.business;

import com.example.nettydemo.common.FrameType;
import com.example.nettydemo.devices.BaseDevice;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;
import org.javatuples.Quartet;

import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;

/**
 * @author: Qbss
 * @date: 2023/11/23
 * @desc:
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public abstract class BaseBusinessDevice extends BaseDevice {

    protected  String dtuSn;

    // 当下缓冲区中的数据长度
    protected int inputBytesBufferLength;

    // 接收到的数据集
    protected byte[] inputBytesBuffer;

    // 缓冲区中可以存放数据的起始位置
    protected int inputBytesBufferStart;

    protected Lock lock;

    protected boolean canSwitchToNext;

    // 当下是否需要发送数据
    protected boolean hasDataToSend;

    // 协议段，创造一个逻辑空间数组，用来存放协议段
    protected Map<String, Quartet<Integer, Integer, String, byte[]>> dataSectionMap;

    // 待发送的数据集
    protected LinkedBlockingDeque<Pair<FrameType,byte[]>> outputBytesBuffer;

    public abstract void recordSendInfo(FrameType paramFrameType);

    public abstract void doBusiness();

    // 连接构建成功，开始初始化设备信息
    public void onConnected() {
        log.info("onConnected");
    }

    public void doParse() {
        log.info("doParse");
    }

    // 添加通道中的数据到缓冲区中去
    public void addInputBytesBuffer(byte[] bytes) {
        // 加锁,保证一次只能一个访问
        this.lock.lock();
        // 判断缓冲区是否已满，如果超过缓存区大小的数据，直接开始丢弃
        try {

            if (this.inputBytesBufferStart + bytes.length < 10000) {
                // 将接受到的数据保存在缓冲区中，以便之后的解析
                System.arraycopy(bytes, 0, this.inputBytesBuffer, this.inputBytesBufferStart, bytes.length);
                // 修改缓冲区中可以存放数据的起始位置
                this.inputBytesBufferStart += bytes.length;
                // 修改缓冲区中的数据长度
                this.inputBytesBufferLength += bytes.length;
            }
        } finally {
            // 保证锁的释放
            this.lock.unlock();
        }
    }

    // 获取待解析的字节流
    protected byte[] getBytesBuffer() {

        int len = Math.min(2048, this.inputBytesBufferLength);
        if (len == 0) {
            return null;
        }

        byte[] bytes;
        // 加锁
        this.lock.lock();
        try {
            bytes = new byte[len];
            // 将从缓冲区中从0开始的到len长度的字节复制到bytes中，同时这部分空间应该腾出来，以便给后续的数据填充
            System.arraycopy(this.inputBytesBuffer, 0, bytes, 0, len);
            // 更新当前缓冲区中的数据长度
            this.inputBytesBufferLength -= len;
            // 因为可能存在缓冲区中当前可用数据长度大于len，所以需要将后续的数据往前移动len个位置（数据前移，以便后续取数据方便）
            if (this.inputBytesBufferLength > 0)
                System.arraycopy(this.inputBytesBuffer, len, this.inputBytesBuffer, 0, this.inputBytesBufferLength);
        } finally {
            // 保证锁的释放
            this.lock.unlock();
        }
        return bytes;
    }
}
