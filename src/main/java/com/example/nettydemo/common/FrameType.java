package com.example.nettydemo.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FrameType {


    /*
     * R
     * */
    // modbus读数据-返回帧 01 02 03 04
    MODBUS_READ_DATA_FRAME("modbus_read_data_frame"),

    /*
     * W
     * */
    // modbus写单个线圈数据-请求帧 05
    MODBUS_WRITE_05_REQUEST_FRAME("modbus_write_05_request_frame"),

    // modbus写单个线圈数据-返回帧 05
    MODBUS_WRITE_05_REPOSE_FRAME("modbus_write_05_repose_frame"),

    // modbus写多个线圈数据-请求帧 0F
    MODBUS_WRITE_0F_REQUEST_FRAME("modbus_write_0F_request_frame"),

    // modbus写多个线圈数据-返回帧 0F
    MODBUS_WRITE_0F_REPOSE_FRAME("modbus_write_0F_repose_frame"),

    // modbus写单个寄存器数据-请求帧 06
    MODBUS_WRITE_06_REQUEST_FRAME("modbus_write_06_request_frame"),

    // modbus写单个寄存器数据-返回帧 06
    MODBUS_WRITE_06_REPOSE_FRAME("modbus_write_06_repose_frame"),

    // modbus写多个寄存器数据-请求帧 10
    MODBUS_WRITE_10_REQUEST_FRAME("modbus_write_10_request_frame"),

    // modbus写多个寄存器数据-返回帧 10
    MODBUS_WRITE_10_REPOSE_FRAME("modbus_write_10_repose_frame");

    private final String name;

}
