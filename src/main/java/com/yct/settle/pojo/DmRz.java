package com.yct.settle.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/7/5 10:21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DmRz {
    /**
     * 文件名
     */
    private String FNAME;
    /**
     * 文件记录总数
     */
    private long FRS;

    @Override
    public String toString() {
        return FNAME + '\t' + FRS ;
    }
}
