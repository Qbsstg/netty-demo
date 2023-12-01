package com.example.nettydemo.devices.business.modbus;

import com.example.nettydemo.common.FrameType;
import com.example.nettydemo.devices.business.BaseBusinessDevice;
import com.example.nettydemo.models.FrameParseResult;
import com.example.nettydemo.protocols.BaseProtocol;
import com.example.nettydemo.protocols.modbus.ModbusTCPProtocol;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;
import org.javatuples.Quartet;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: Qbss
 * @date: 2023/11/23
 * @desc:
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class BaseModbusTCPDevice extends BaseBusinessDevice {

    //帧解析器：基础帧解析器
    private BaseProtocol baseProtocol;

    // 这个值在并发情况下会带来一些问题
    /*
     * 根据设定的模型：
     *   doPares -> send -> doBusiness
     * 1  x      ->  x   ->  产生报文
     * 2  x      ->  发送  -> 产生报文
     * 3  解析    ->  发送   ->  产生报文
     * 问题就在于，因为是并发执行，一个线程在解析前，又有一条新的报文产生的了，导致MBAPIndex+1了，和解析的不一致了
     * 现修改为获取事务序列号，直接从报文中处理
     * */
    private int MBAPIndex;

    private Instant lastSendTime;

    // 从站地址
    private short deviceAddressShort;

    public BaseModbusTCPDevice(String deviceSn, String dtuSn, String name, String protocolType, short address) {

        this.baseProtocol = new ModbusTCPProtocol();

        this.dtuSn = dtuSn;
        this.sn = deviceSn;
        this.name = name;
        this.inputBytesBuffer = new byte[20480];
        this.inputBytesBufferStart = 0;
        this.inputBytesBufferLength = 0;
        this.lock = new ReentrantLock();
        this.canSwitchToNext = true;
        this.hasDataToSend = false;
        this.outputBytesBuffer = new LinkedBlockingDeque<>();
        this.dataSectionMap = new HashMap<>();
        this.MBAPIndex = 0;
        this.lastSendTime = Instant.now();

        this.deviceAddressShort = address;

    }

    @Override
    public void onConnected() {
        Instant now = Instant.now();
        this.lastSendTime = now.minusSeconds(2000L);
        this.MBAPIndex = 0;

    }

    @Override
    public void recordSendInfo(FrameType paramFrameType) {
        // 此处根据不同的类型帧，进行不同业务处理
        // 。。。。
        this.lastSendTime = Instant.now();
    }

    @Override
    public void doBusiness() {
        Instant now = Instant.now();

        // 判断是否需要发送数据
        Duration between = Duration.between(this.lastSendTime, now);

        // 此处，常理来说，应该按照协议段去区分单独请求，但由于时间有限，一下子请求100个寄存器所有的数据（其实最大是124个
        if (between.getSeconds() > 60) {
            // 发送信息总召
            sendReadData(3, 0, 3);
        }

    }

    private void sendReadData(int type, int start, int num) {

        if (this.MBAPIndex == 65535) {
            this.MBAPIndex = 0;
        } else {
            this.MBAPIndex++;
        }

        byte[] sendBytes = new byte[12];

        // 事务标识符
        sendBytes[0] = (byte) (this.MBAPIndex >> 8 & 0xFF);
        sendBytes[1] = (byte) (this.MBAPIndex & 0xFF);

        // Modbus协议标识符
        sendBytes[2] = 0;
        sendBytes[3] = 0;

        // 数据长度
        sendBytes[4] = 0;
        sendBytes[5] = 6;

        // RTU信息体帧
        byte[] queryBytes = new byte[6];
        queryBytes[0] = (byte) (this.deviceAddressShort & 0xFF);
        // 暂时默认 3
        queryBytes[1] = (byte) (type & 0xFF);

        // 起始地址
        queryBytes[2] = (byte) (start >> 8 & 0xFF);
        queryBytes[3] = (byte) (start & 0xFF);

        // 寄存器数量
        queryBytes[4] = (byte) (num >> 8 & 0xFF);
        queryBytes[5] = (byte) (num & 0xFF);

        // 将RTU信息体帧添加到发送帧中
        System.arraycopy(queryBytes, 0, sendBytes, 6, 6);

        this.outputBytesBuffer.offer(new Pair<>(FrameType.MODBUS_READ_DATA_FRAME, sendBytes));
        this.hasDataToSend = true;
    }

    //重写解析方法
    @Override
    public void doParse() {
        //判断解析器是否不存在
        if (this.baseProtocol == null) {
            log.error("{},baseProtocol is null", log.getName());
            return;
        }

        // 获取待解析的字节流
        byte[] bytesBuffer = getBytesBuffer();
        if (bytesBuffer == null) {
            return;
        }

        Map<String, Object> param = new HashMap<>();
        // 暂时只考虑遥测的情况
        param.put("sectionName", "YC");
        // param.put("MBAPIndex", this.MBAPIndex);

        // 解析数据
        List<FrameParseResult> maps = this.baseProtocol.doBytesParse(bytesBuffer, param);

        if (maps.isEmpty()) {
            return;
        }

        maps.forEach(m -> {
            // 获取解析结果
            if (m != null && m.getResult()) {
                FrameType frameType = m.getType();
                switch (frameType) {
                    case MODBUS_READ_DATA_FRAME ->
                        // 解析ModbusTCP报文
                            handleModbusReadDataFrame(m);
                    case MODBUS_WRITE_05_REPOSE_FRAME -> {
                    }
                    // 解析写单线圈报文 - 遥控
                    case MODBUS_WRITE_0F_REPOSE_FRAME -> {
                    }
                    // 解析写多寄存器报文 - 遥调
                    default -> {
                    }
                }
            }
        });

    }

    private void handleModbusReadDataFrame(FrameParseResult f) {
        Pair<String, byte[]> pairData = f.getPairData();
        String sectionName = pairData.getValue0();
        byte[] datas = pairData.getValue1();

        log.info("sectionName: {}, datas: {}", sectionName, Arrays.toString(datas));

        Quartet<Integer, Integer, String, byte[]> quartet = this.dataSectionMap.get(sectionName);
        if (quartet == null) {
            log.error("{},sectionName is not exist", log.getName());
            return;
        }

        for (Quartet<Integer, Integer, String, byte[]> value : this.dataSectionMap.values()) {
            if (!value.getValue2().equals(sectionName)) {
                continue;
            }

            // 起始位置
            Integer start = value.getValue0();
            // 长度
            Integer length = value.getValue1();


        }
    }


}
