package com.yct.settle.service.impl;

import com.yct.settle.mapper.CardAreaMapper;
import com.yct.settle.service.AreaService;

import javax.annotation.Resource;

/**
 * DESC:获取卡的发行地和使用地
 * AUTHOR:mlsama
 * 2019/7/31 14:28
 */
public class AreaServiceImpl implements AreaService {
    @Resource
    private CardAreaMapper cardAreaMapper;
    @Override
    public String getIssuesByCardNo(String cardNo) {
        return cardAreaMapper.getIssuesByCardNo(Long.parseLong(cardNo));
    }
}
