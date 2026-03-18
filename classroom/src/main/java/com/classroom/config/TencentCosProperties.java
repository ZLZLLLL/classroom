package com.classroom.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 腾讯云 COS 配置。
 */
@Data
@ConfigurationProperties(prefix = "tencent.cos")
public class TencentCosProperties {

    /**
     * 是否启用 COS 存储。
     */
    private boolean enabled = false;

    private String secretId;

    private String secretKey;

    private String region;

    private String bucket;

    /**
     * 可选：用于拼接展示/下载使用的固定域名（可为 COS 默认域名或自定义 CDN 域名）。
     */
    private String domain;

    /**
     * 对象 key 前缀。
     */
    private String prefix = "classroom/";

    /**
     * 预签名下载 URL 有效期（秒）。
     */
    private long presignExpireSeconds = 300;
}
