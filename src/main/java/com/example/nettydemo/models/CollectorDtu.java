package com.example.nettydemo.models;

import lombok.Data;

@Data
public class CollectorDtu {
    private String sn;
    private String name;
    private String ipAddress;
    private Integer port;
    private Integer accessType;
}
