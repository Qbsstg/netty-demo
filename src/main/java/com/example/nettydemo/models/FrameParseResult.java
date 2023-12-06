package com.example.nettydemo.models;

import com.example.nettydemo.common.FrameType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.javatuples.Pair;

/**
 * @author: Qbss
 * @date: 2023/11/29
 * @desc:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FrameParseResult {

    // 解析结果
    private Boolean result;

    // 帧类型
    private FrameType type;

    // 帧数据 <类型，数据>
    private Pair<String, byte[]> pairData;

    public FrameParseResult(Boolean result) {
        this.result = result;
    }
}
