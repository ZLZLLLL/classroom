package com.classroom.service;

import com.classroom.config.TencentCosProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(COSClient.class)
public class CosService {

    private final COSClient cosClient;
    private final TencentCosProperties props;

    public String normalizeKey(String key) {
        String prefix = StringUtils.defaultString(props.getPrefix(), "");
        if (StringUtils.isBlank(prefix)) {
            return key;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        if (StringUtils.startsWith(key, "/")) {
            key = key.substring(1);
        }
        if (StringUtils.startsWith(key, prefix)) {
            return key;
        }
        return prefix + key;
    }

    public void putObject(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        String key = normalizeKey(objectKey);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        if (StringUtils.isNotBlank(contentType)) {
            metadata.setContentType(contentType);
        }

        PutObjectRequest request = new PutObjectRequest(props.getBucket(), key, inputStream, metadata);
        cosClient.putObject(request);
    }

    public URL generatePresignedDownloadUrl(String objectKey, String downloadFileName) {
        String key = normalizeKey(objectKey);

        Date expiration = Date.from(Instant.now().plusSeconds(Math.max(1, props.getPresignExpireSeconds())));
        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(props.getBucket(), key, HttpMethodName.GET);
        req.setExpiration(expiration);

        // 设置下载文件名（Content-Disposition）
        if (StringUtils.isNotBlank(downloadFileName)) {
            // 注意：COS 会原样使用该 header 值。这里用最常见的 attachment; filename="..."
            // 中文名在不同浏览器表现不一致；如需更强兼容可扩展 RFC5987 filename*.
            req.addRequestParameter("response-content-disposition",
                    "attachment; filename=\"" + downloadFileName.replace("\"", "") + "\"");
        }

        return cosClient.generatePresignedUrl(req);
    }

    public URL generatePresignedPreviewUrl(String objectKey) {
        String key = normalizeKey(objectKey);
        Date expiration = Date.from(Instant.now().plusSeconds(Math.max(1, props.getPresignExpireSeconds())));
        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(props.getBucket(), key, HttpMethodName.GET);
        req.setExpiration(expiration);
        return cosClient.generatePresignedUrl(req);
    }

    public void deleteObjectQuietly(String objectKey) {
        try {
            String key = normalizeKey(objectKey);
            cosClient.deleteObject(props.getBucket(), key);
        } catch (Exception ignored) {
            // 幂等删除：忽略异常
        }
    }
}
