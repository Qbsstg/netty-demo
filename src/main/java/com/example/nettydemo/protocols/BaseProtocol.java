package com.example.nettydemo.protocols;

import com.example.nettydemo.models.FrameParseResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: Qbss
 * @date: 2023/11/23
 * @desc:
 */
@Slf4j
public abstract class BaseProtocol {

    // Protocol缓冲区中可以存放数据的起始位置
    protected int BytesBufferIndex;

    // 缓冲区中等待被解析的数据长度
    protected int toBeParsedBytesLength;

    // 单一个报文的缓冲数组
    protected byte[] frameBytesBuffer;

    // 解析过程中的index
    protected int pos;

    // 报文头中携带的接下来的数据长度
    protected int length;

    // 报文长度
    protected int bodyLength;

    //
    /*
    * 当前解析状态:解析头、解析长度、解析内容
    *            0      1       2
    * */
    protected int status;

    public List<FrameParseResult> doBytesParse(byte[] inBuffer, Map<String, Object> params) {
        return new ArrayList<>();
    }
}
