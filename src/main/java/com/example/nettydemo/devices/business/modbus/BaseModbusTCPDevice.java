package com.example.nettydemo.devices.business.modbus;

import com.example.nettydemo.common.FrameType;
import com.example.nettydemo.devices.business.BaseBusinessDevice;
import com.example.nettydemo.models.FrameParseResult;
import com.example.nettydemo.protocols.BaseProtocol;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;
import org.javatuples.Quartet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private int MBAPIndex;

    @Override
    public void onConnected() {
        super.onConnected();
    }

    @Override
    public void recordSendInfo(FrameType paramFrameType) {

    }

    @Override
    public void doBusiness() {

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
        param.put("MBAPIndex", this.MBAPIndex);

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
