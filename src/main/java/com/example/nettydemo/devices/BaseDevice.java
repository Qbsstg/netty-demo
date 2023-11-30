package com.example.nettydemo.devices;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * @author: Qbss
 * @date: 2023/11/23
 * @desc:
 */
@Slf4j
@Data
public abstract class BaseDevice {

    protected String sn;

    protected Instant lastReceivedTime;

}
