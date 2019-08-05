package com.yct;

import com.yct.settle.pojo.CustomerService;
import com.yct.settle.utils.MathUtil;
import org.junit.Test;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/8/5 9:41
 */
public class MathTest {

    @Test
    public void longTest(){
        CustomerService cpuCustomerDate = new CustomerService();
        Long l = MathUtil.longAdd(cpuCustomerDate.getCpuCustomerInvestNotes(),cpuCustomerDate.getCpuCustomerInvestNotes());
        System.out.println(l);
    }
}
