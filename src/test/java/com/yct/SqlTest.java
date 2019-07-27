package com.yct;

import com.yct.settle.mapper.ExceptionTradeMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {com.yct.settle.Application.class})
public class SqlTest {
    @Resource
    private ExceptionTradeMapper exceptionTradeMapper;
    @Test
    public void insertTest(){
        exceptionTradeMapper.insert("20120610", "XF2017000120120610.ZIP");

    }
}
