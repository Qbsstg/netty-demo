package com.example.nettydemo.init;

import com.example.nettydemo.channel.netty.NettyClient;
import com.example.nettydemo.common.AccessType;
import com.example.nettydemo.common.GlobalVariables;
import com.example.nettydemo.devices.business.BaseBusinessDevice;
import com.example.nettydemo.devices.business.modbus.BaseModbusTCPDevice;
import com.example.nettydemo.devices.dtu.ServerDtu;
import com.example.nettydemo.models.CollectorDtu;
import com.example.nettydemo.models.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(3)
@Slf4j
public class NettyComponent implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {

        String ipAddress = "localhost";

        List<CollectorDtu> dtus = new ArrayList<>();

        CollectorDtu dtu = new CollectorDtu();
        dtu.setSn("A");
        dtu.setName("A_name");
        dtu.setIpAddress(ipAddress);
        dtu.setPort(502);
        // TCP模式
        dtu.setAccessType(1);

        dtus.add(dtu);

        List<Device> devices = new ArrayList<>();

        Device device = new Device();
        device.setSn("A_device");
        device.setDtuSn("A");
        device.setName("A_name_device");
        device.setProtocolType("Modbus_TCP");
        device.setAddress((short) 1);

        devices.add(device);

        Map<String, List<Device>> collect = devices.stream().collect(Collectors.groupingBy(Device::getDtuSn));

        dtus.forEach(collectorDtu -> {
            if (collectorDtu.getAccessType().equals(AccessType.TCP_CLIENT.getCode())) {
                ServerDtu serverDtu = new ServerDtu(
                        collectorDtu.getSn(),
                        collectorDtu.getName(),
                        collectorDtu.getIpAddress(),
                        collectorDtu.getPort(),
                        AccessType.TCP_CLIENT
                );
                List<Device> deviceList = collect.get(collectorDtu.getSn());

                if (!deviceList.isEmpty()) {
                    // 创建设备
                    List<BaseBusinessDevice> result = new ArrayList<>();
                    deviceList.forEach(device1 -> {
                        if (device1.getProtocolType().equals("Modbus_TCP")) {
                            // 创建modbus设备
                            result.add(new BaseModbusTCPDevice(
                                    device1.getSn(),
                                    device1.getDtuSn(),
                                    device1.getName(),
                                    device1.getProtocolType(),
                                    device1.getAddress()
                            ));
                        }
                    });
                    serverDtu.setDevices(result);
                    GlobalVariables.GLOBAL_SERVER_DTU_LIST.add(serverDtu);
                }
            }
        });

        dtus.forEach(d -> {
            log.info("dtu:{}", d);
            NettyClient nettyClient = new NettyClient(dtu.getIpAddress(), dtu.getPort());
            nettyClient.connect();
        });
    }
}
