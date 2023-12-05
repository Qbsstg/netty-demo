package com.example.nettydemo.devices.business.modbus;

import com.example.nettydemo.common.FrameType;
import com.example.nettydemo.devices.business.BaseBusinessDevice;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: Qbss
 * @date: 2023/11/23
 * @desc:
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class BaseModbusUDPDevice extends BaseBusinessDevice {

    @Override
    public void recordSendInfo(FrameType paramFrameType, byte[] outBuf) {

    }

    @Override
    public void doBusiness() {

    }

    @Override
    public void doParse() {
        super.doParse();
    }
}
