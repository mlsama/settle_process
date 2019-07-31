package com.yct.settle.service;

/**
 * DESC: 获取卡的发行地和使用地
 * AUTHOR:mlsama
 * 2019/7/31 14:27
 */
public interface AreaService {

    String getIssuesByCardNo(String cardNo);
}
