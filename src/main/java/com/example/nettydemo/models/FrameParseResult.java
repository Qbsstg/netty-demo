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

    private Boolean result;

    private FrameType type;

    private Pair<String, byte[]> pairData;

    public FrameParseResult(Boolean result) {
        this.result = result;
    }
}
