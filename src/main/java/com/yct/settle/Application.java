package com.yct.settle;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

//SpringBoot应用的运行主类,默认扫描该类所在包下的所有包
@SpringBootApplication
//引入配置文件,是个数组,可引入多个
//@ImportResource(locations = {"classpath:threadPool-config.xml"})
public class Application {
    public static void main(String[] args) {
        /** 创建SpringApplication应用对象 */
        SpringApplication springApplication = new SpringApplication(Application.class);
        /** 设置横幅模式(设置关闭) */
        springApplication.setBannerMode(Banner.Mode.OFF);
        springApplication.run(args);
    }
}
