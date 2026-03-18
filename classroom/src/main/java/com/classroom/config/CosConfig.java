package com.classroom.config;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 说明：COS SDK 类在这里用全限定名创建，避免 IDE 依赖未刷新时出现“无法解析符号”。

@Configuration
@RequiredArgsConstructor
public class CosConfig {

    private final TencentCosProperties props;

    @Bean
    @ConditionalOnProperty(prefix = "tencent.cos", name = "enabled", havingValue = "true")
    public com.qcloud.cos.COSClient cosClient() {
        if (StringUtils.isBlank(props.getSecretId()) || StringUtils.isBlank(props.getSecretKey())) {
            throw new IllegalStateException("tencent.cos.secret-id/secret-key 未配置（或为空）");
        }
        if (StringUtils.isBlank(props.getRegion())) {
            throw new IllegalStateException("tencent.cos.region 未配置（或为空）");
        }

        com.qcloud.cos.auth.COSCredentials credentials =
                new com.qcloud.cos.auth.BasicCOSCredentials(props.getSecretId(), props.getSecretKey());
        com.qcloud.cos.ClientConfig clientConfig =
                new com.qcloud.cos.ClientConfig(new com.qcloud.cos.region.Region(props.getRegion()));
        return new com.qcloud.cos.COSClient(credentials, clientConfig);
    }
}

