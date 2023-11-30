package com.example.nettydemo.models;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;


@Data
public class Device {
    private String sn;
    private String name;
    private String dtuSn;
    private String protocolType;
    private Byte deviceBeingUsed;
    // 设备地址，从站地址
    private short address;

}

