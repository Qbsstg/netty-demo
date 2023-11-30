package com.example.nettydemo.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: Qbss
 * @date: 2023/11/30
 * @desc:
 */
@AllArgsConstructor
@Getter
public enum AccessType {

    TCP_CLIENT(1, "TCP客户端"),

    KAFKA(3, "Kafka");

    private final Integer code;

    private final String name;

}
