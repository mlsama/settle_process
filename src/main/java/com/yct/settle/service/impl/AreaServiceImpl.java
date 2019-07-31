package com.yct.settle.service.impl;

import com.yct.settle.mapper.CardAreaMapper;
import com.yct.settle.mapper.MerchantAreaMapper;
import com.yct.settle.service.AreaService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * DESC:获取卡的发行地和使用地
 * AUTHOR:mlsama
 * 2019/7/31 14:28
 */
@Service
public class AreaServiceImpl implements AreaService {
    @Resource
    private CardAreaMapper cardAreaMapper;
    @Resource
    private MerchantAreaMapper merchantAreaMapper;
    @Override
    public String getIssuesByCardNo(String cardNo) {
        String areaCode = cardAreaMapper.getIssuesByCardNo(cardNo.substring(6, 15));
        if (areaCode == null){
            areaCode = "01";
        }
        return areaCode;
    }


    @Override
    public String getUseAreaByMerchant(String zipFileName) {
        String merchantNo = zipFileName.substring(2,6);
        String areaNo = merchantAreaMapper.getUseAreaByMerchant(merchantNo);
        if (areaNo == null){
            merchantNo = zipFileName.substring(2,5);
            areaNo = merchantAreaMapper.getUseAreaByMerchant(merchantNo);
        }
        return areaNo;
    }
}
