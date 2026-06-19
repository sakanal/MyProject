package com.sakanal.web.dto;

import java.util.Map;

import lombok.Data;

/**
 * Pixiv 配置更新请求参数类
 * @author sakanal
 */
@Data
public class PixivConfigUpdateDto {
    private Map<String, String> requestHeader;
    private String charsetName;
    private String username;
    private String password;
}