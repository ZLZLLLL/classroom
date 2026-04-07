package com.classroom;

import com.classroom.config.TencentCosProperties;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@MapperScan(basePackages = "com.classroom.repository", annotationClass = Mapper.class)
@EnableConfigurationProperties(TencentCosProperties.class)
@EnableScheduling
public class ClassroomApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClassroomApplication.class, args);
    }
}
