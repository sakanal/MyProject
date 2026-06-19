package com.sakanal.web.util;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * SSL工具类
 * 用于忽略SSL证书验证，解决访问HTTPS网站时的证书问题
 * 注意：使用此工具会降低安全性，仅建议在开发测试环境使用
 *
 * @author sakanal
 */
public class MySslUtils {
    /**
     * 配置信任所有HTTPS证书
     * 创建一个信任所有证书的TrustManager，并设置为默认的SSLSocketFactory
     *
     * @throws Exception 配置过程中可能抛出的异常
     */
    private static void trustAllHttpsCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[1];
        TrustManager tm = new MyTrustManager();
        trustAllCerts[0] = tm;
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }
    /**
     * 自定义信任管理器，信任所有证书
     * 用于绕过SSL证书验证
     */
    static class MyTrustManager implements TrustManager,X509TrustManager {
        /**
         * 获取受信任的CA证书数组
         * 返回null，表示不使用任何CA证书
         *
         * @return null
         */
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        public boolean isServerTrusted(X509Certificate[] certs) {
            return true;
        }
        public boolean isClientTrusted(X509Certificate[] certs) {
            return true;
        }
        /**
         * 检查服务器证书信任
         * 空实现，表示信任所有服务器证书
         *
         * @param certs    证书数组
         * @param authType 认证类型
         */
        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
            return;
        }
        /**
         * 检查客户端证书信任
         * 空实现，表示信任所有客户端证书
         *
         * @param certs    证书数组
         * @param authType 认证类型
         */
        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
            return;
        }
    }
    /**
     * 忽略HTTPS请求的SSL证书验证
     * 配置信任所有证书并跳过主机名验证
     * 必须在openConnection之前调用
     * 
     * @throws Exception 配置过程中可能抛出的异常
     */
    public static void ignoreSsl() throws Exception{
        HostnameVerifier hv = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostName, SSLSession session) {
                System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                return true;
            }
        };
        trustAllHttpsCertificates();
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }
}