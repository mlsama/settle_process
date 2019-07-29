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
        exceptionTradeMapper.insert("'20121029'", "'XF8015000120121029.ZIP'","T_MCARD_CONSUME_NOBUS","T_MCARD_CONSUME_ERROR_NOBUS");

    }
}
