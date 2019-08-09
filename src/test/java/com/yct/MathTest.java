package com.yct;

import com.yct.settle.pojo.CustomerService;
import com.yct.settle.utils.AmountUtil;
import com.yct.settle.utils.MathUtil;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * DESC:
 * AUTHOR:mlsama
 * 2019/8/5 9:41
 */
public class MathTest {

    @Test
    public void longTest(){
        CustomerService cpuCustomerDate = new CustomerService();
        Long l = MathUtil.longMinus(null,2L);
        System.out.println(l);
    }

    @Test
    public void minusTest(){
        BigDecimal minus = AmountUtil.minus(null, new BigDecimal("1"));
        System.out.println(minus);
    }
}
