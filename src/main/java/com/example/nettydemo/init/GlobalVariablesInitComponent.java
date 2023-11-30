package com.example.nettydemo.init;

import com.example.nettydemo.common.GlobalVariables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

/**
 * @author: Qbss
 * @date: 2023/11/30
 * @desc:
 */
@Component
@Order(1)
@Slf4j
public class GlobalVariablesInitComponent implements CommandLineRunner {


    @Override
    public void run(String... args) throws Exception {
        log.info("GlobalVariablesInitComponent init");

        // 试试虚拟线程
        GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL = Executors.newScheduledThreadPool(100);;
    }
}
