package com.example.nettydemo.common;

import com.example.nettydemo.devices.dtu.ServerDtu;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author: Qbss
 * @date: 2023/11/30
 * @desc:
 */
public class GlobalVariables {

    //线程池，在初始化时初始固定size的线程池
    public static ScheduledExecutorService GLOBAL_SCHEDULED_SERVICE_POOL = null; //线程池

    public static List<ServerDtu> GLOBAL_SERVER_DTU_LIST;

    public static Map<String, ServerDtu> GLOBAL_CONNECTKEY_SERVER_DTU_MAP;

    public static Map<String, ChannelHandlerContext> GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP;


    static {
        GLOBAL_SERVER_DTU_LIST = new ArrayList<>();
        GLOBAL_CONNECTKEY_SERVER_DTU_MAP = new HashMap<>();
        GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP = new HashMap<>();
    }

}
