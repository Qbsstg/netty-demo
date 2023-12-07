package com.example.nettydemo.protocols.modbus;

import com.example.nettydemo.common.FrameType;
import com.example.nettydemo.models.FrameParseResult;
import com.example.nettydemo.protocols.BaseProtocol;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: Qbss
 * @date: 2023/11/28
 * @desc:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ModbusTCPProtocol extends BaseProtocol {

    // 设备地址
    private byte deviceAddress;

    // 功能码
    private byte functionCode;

    private byte[] toBeParsedBytes = new byte[2048];

    @Override
    public List<FrameParseResult> doBytesParse(byte[] inBuffer, Map<String, Object> params) {
        List<FrameParseResult> resultList = new ArrayList<>();
        FrameParseResult parseResult = new FrameParseResult();

        // 如果数据集为空，直接返回错误结果
        if (inBuffer == null || inBuffer.length == 0) {
            resultList.add(new FrameParseResult(false));
            return resultList;
        }

        // 获取 最后一次（最新）的 ModbusTCP中的MBAP中的事务处理标识，以便等下获取报文的序列号，以匹配对应的请求报文
        // int MBAPIndex = (Integer) params.get("MBAPIndex");
        /*
         * 修改：
         *   获取当前序号的方式由于并发的存在，除非将MBAPIndex设置为线程安全的，否则会出现问题，改为从报文中直接获取
         *
         * 我觉得这个解决方案不太好，因为我没办法保证inBuffer的数据是完整的标准格式，假如inBuffer的数据是不完整的，那么我就没办法获取到正确的index
         * */
        int MBAPIndex = (inBuffer[0] & 0xFF) << 8 | (inBuffer[1] & 0xFF);

        // 将接受到的数据保存在缓冲区中，以便之后的解析
        System.arraycopy(inBuffer, 0, this.toBeParsedBytes, this.BytesBufferIndex, inBuffer.length);
        // 更新缓冲区中可以存放数据的起始位置
        this.BytesBufferIndex += inBuffer.length;
        // 更新缓冲区中等待解析的数据长度
        this.toBeParsedBytesLength += inBuffer.length;

        this.pos = 0;
        // 是否停止解析
        boolean stop = false;
        while (true) {
            // 剩下需要解析的字节长度
            int remainLen = this.toBeParsedBytesLength - this.pos;
            // 当前解析状态为解析头
            if (this.status == 0) {
                if (remainLen < 2) {
                    stop = true;
                    break;
                }
                while (this.pos + 1 < this.toBeParsedBytesLength) {
                    // 判断接受到的报文是否对应之前的请求，与当下的报文序列号是否一一对应，将不明的报文丢弃
                    if (this.toBeParsedBytes[this.pos] == (byte) (MBAPIndex >> 8 & 0xFF) && this.toBeParsedBytes[this.pos + 1] == (byte) (MBAPIndex & 0xFF)) {
                        break;
                    }
                    this.pos++;
                }
                // 当剩下的字节长度已经不满足解析的条件时，直接跳出循环，等待下一次的解析（回溯剪枝!!!，剪！）
                if (this.pos == this.toBeParsedBytesLength || this.pos + 1 == this.toBeParsedBytesLength) {
                    // 更新当前可解析的数据长度，直接置为0
                    this.toBeParsedBytesLength = 0;
                    // 更新当前可解析的数据起始位置，直接置为0
                    this.BytesBufferIndex = 0;
                    break;
                }
                // 更新当前解析状态，进入下一步解析
                this.status = 1;
            }

            // 更新剩下需要解析的字节长度
            remainLen = this.toBeParsedBytesLength - this.pos;
            // 当前解析状态为解析长度
            if (this.status == 1) {
                /*
                 * 按照最小的回复的单一个线圈信息的报文长度来算
                 * 对应：
                 *   ModbusTCP标识：00 00 --- 2个字节
                 *   报文长度：00 06 --- 2个字节
                 *   设备地址：01    --- 1个字节
                 *   功能码：03      --- 1个字节
                 *   字节数：01      --- 1个字节
                 *   线圈数据：FF 00 --- 2个字节
                 *   一共：9个字节
                 * */
                if (remainLen < 9) {
                    // 剩下的字节长度不满足解析的条件，直接跳出循环
                    stop = true;
                    break;
                }
                // 获取数据长度
                this.length = this.toBeParsedBytes[this.pos + 5] & 0xFF;
                // 获取设备地址
                this.deviceAddress = this.toBeParsedBytes[this.pos + 6];
                // 获取功能码
                this.functionCode = this.toBeParsedBytes[this.pos + 7];
                // 更新当前报文的总长度，序列号2 + 协议标识2 + 数据长度 2
                this.bodyLength = this.length + 6;
                // 更新当前解析状态，进入下一步解析
                this.status = 2;
            }

            remainLen = this.toBeParsedBytesLength - this.pos;
            // 当前解析状态为解析内容
            if (this.status == 2) {
                if (remainLen < this.bodyLength) {
                    // 剩下的字节长度不满足解析的条件，直接跳出循环
                    stop = true;
                    break;
                }
                // 将数据保存到单一个报文的缓冲区中，开始单独解析,此时的pos逻辑位置应该是在0，在报文的开始
                System.arraycopy(this.toBeParsedBytes, this.pos, this.frameBytesBuffer, 0, this.bodyLength);
                try {
                    resultList.add(parseFrame(this.frameBytesBuffer, params));
                } catch (Exception e) {
                    log.error("ModbusTCPProtocol doBytesParse error: {}", e.getMessage());
                }
                // 更新当前解析位置，跳过当前报文
                this.pos += this.bodyLength;
                // 更新当前解析状态，进入下一步解析
                this.status = 0;
            }
        }

        // 当解析index虽然还不为0，但是需要停止解析时，将剩下的数据保存到缓冲区中，以便下一次解析
        if (this.pos != 0 && stop) {
            // 更新当前可解析的数据长度,减去已经解析掉的数据
            this.toBeParsedBytesLength -= this.pos;
            if (this.toBeParsedBytesLength > 0) {
                // 将剩下的数据前移，保证下次解析按照顺序执行
                System.arraycopy(this.toBeParsedBytes, this.pos, this.toBeParsedBytes, 0, this.toBeParsedBytesLength);
            }
            // 更新当前可解析的数据起始位置，减去已经解析掉的数据
            this.BytesBufferIndex -= this.pos;
            // 更新当前解析状态，进入下一步解析
            this.status = 0;
        }

        parseResult.setResult(false);
        resultList.add(parseResult);
        return resultList;
    }

    private static FrameParseResult parseFrame(byte[] frameBytesBuffer, Map<String, Object> params) {

        FrameParseResult parseResult = new FrameParseResult();

        StringBuilder hexStr = new StringBuilder();
        for (byte b : frameBytesBuffer) {
            hexStr.append(String.format("%02X ", b));
        }

        /*
         *  0x00 0x00 | 0x00 0x00 | 0x00 0x06 | 0x01 | 0x04 | 0x02 | 0x00 0x20
         *  0     1     2    3      4    5      6      7      8      9    10
         * */
        log.debug("ModbusTCPProtocol parseFrame: {}", hexStr);
        short type = (short) (frameBytesBuffer[7] & 0xFF);
        log.debug("ModbusTCPProtocol parseFrame type: {}", type);

        byte[] dataSection = null;
        switch (type) {
            case 1, 2, 3, 4 -> {
                // 功能码 0x01 ：读线圈 - 状态量 可连续多个
                // 功能码 ：读保持寄存器 - 模拟量 可连续多个
                // 数据中字节数 理应为2的倍数
                int dataLength = frameBytesBuffer[8] & 0xFF;
                dataSection = new byte[dataLength];
                System.arraycopy(frameBytesBuffer, 9, dataSection, 0, dataLength);
                parseResult.setType(FrameType.MODBUS_READ_DATA_FRAME);
            }
            case 5, 6 -> {
                // 写单个线圈或单个寄存器

            }
            case 10, 16 -> {
                // 写多个线圈或多个寄存器
            }
            default -> {
                log.error("ModbusTCPProtocol parseFrame error: {}", hexStr);
            }
        }
        String sectionName = (String) params.get("sectionName");
        // 当下只考虑单线圈和单寄存器的读取
        Pair<String, byte[]> pair = new Pair<>(sectionName, dataSection);
        parseResult.setResult(true);
        parseResult.setPairData(pair);
        return parseResult;
    }

    public static void main(String[] args) {
        byte[] buf = {0x00, 0x00, 0x00, 0x00, 0x00, 0x06,
                0x01, 0x04, 0x01, 0x11};
        // FrameParseResult parseResult = parseFrame(buf, null);
        // System.out.println(parseResult);
        byte[] buf1 = {
                0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x01, 0x04, 0x01, 0x11,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x01, 0x04, 0x01, 0x11,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x01, 0x04, 0x01, 0x11};

        byte[] buf2 = {
                0x06, 0x01, 0x04, 0x01, 0x11,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x01, 0x04, 0x01, 0x11,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x01, 0x04, 0x01, 0x11};

        System.out.println(extractModbusFrames(buf2));
    }

    public static boolean isValidModbusFrame(byte[] frame) {
        if (frame.length < 12) {
            return false;
        }

        int transactionId = (frame[0] & 0xFF) << 8 | (frame[1] & 0xFF);
        if (transactionId != 0x0000) {
            return false;
        }

        int protocolId = (frame[2] & 0xFF) << 8 | (frame[3] & 0xFF);
        if (protocolId != 0x0000) {
            return false;
        }

        int length = (frame[4] & 0xFF) << 8 | (frame[5] & 0xFF);
        if (length != frame.length - 6) {
            return false;
        }

        byte unitId = frame[6];
        if (unitId != 0x01) {
            return false;
        }

        byte functionCode = frame[7];
        return functionCode >= 1 && functionCode <= 16;
    }

    public static List<byte[]> extractModbusFrames(byte[] inBuffer) {
        List<byte[]> frames = new ArrayList<>();
        ByteArrayOutputStream tempFrame = new ByteArrayOutputStream();
        int frameStartIndex = 0;

        for (int i = 0; i < inBuffer.length; i++) {
            tempFrame.write(inBuffer[i]);
            byte[] frameBytes = tempFrame.toByteArray();

            // 检查是否有完整的Modbus报文
            if (frameBytes.length >= 12 && isValidModbusFrame(frameBytes)) {
                frames.add(frameBytes);
                tempFrame.reset();
                frameStartIndex = i;
            }
        }

        // 检查是否有未完成的报文
        if (frameStartIndex < inBuffer.length) {
            byte[] remainingBytes = new byte[inBuffer.length - frameStartIndex];
            System.arraycopy(inBuffer, frameStartIndex, remainingBytes, 0, remainingBytes.length);
            if (isValidModbusFrame(remainingBytes)) {
                frames.add(remainingBytes);
            }
        }

        return frames;
    }
}
