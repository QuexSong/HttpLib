package com.quexs.tool.httplib.ssl;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.InputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 证书设置类
 * HttpsURLConnection
 */
public class SSLSocketFactoryTool {

    /**
     * 启动跳过所有验证
     */
    public static void enableDefaultSkipVerify(){
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(mGetSkipVerifySSLSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动默认客户端验证，跳过服务端验证
     */
    public static void enableDefaultClientVerify(){
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(mGetClientVerifySSLSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception  e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动证书客户端验证，跳过服务端验证
     * @param context
     * @param alias 别名（随便写）
     * @param crtName asset下 证书名称
     */
    public static void enableCrtClientVerify(Context context, String alias,String crtName){
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(mGetCrtClientVerifySSLSocketFactory(context,alias,crtName));
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 启动证书双向验证
     * @param context
     * @param clintKbsName 客户端证书
     * @param clintKbsPwd 客户端证书密码
     * @param serverKbsName 服务端证书
     * @param serverKbsPwd 服务端证书密码
     */
    public static void enableClientHostVerify2SSLSocketFactory(Context context, String clintKbsName, String clintKbsPwd, String serverKbsName, String serverKbsPwd){
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(mGetClientHostVerify2SSLSocketFactory(context, clintKbsName,clintKbsPwd,serverKbsName,serverKbsPwd));
            //设置ip授权认证：如果已经安装该证书，可以不设置，否则需要设置
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    // 返回true代表通过  false代表不通过  如果直接返回true。意味着不去验证主机名，可能会导致中间人攻击
                    // hostname 为客户端请求的域名，session为服务端证书。系统默认验证主要是验证hostname与证书内的域名是否一致
                    return HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname,session);
                }
            });
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取跳过所有验证的SSLSocketFactory
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static SSLSocketFactory mGetSkipVerifySSLSocketFactory() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sslContext.init(null, new TrustManager[]{mGetX509ExtendedTrustManager()}, new SecureRandom());
        }else {
            sslContext.init(null, new TrustManager[]{mGetX509TrustManager()}, new SecureRandom());
        }
        return sslContext.getSocketFactory();
    }

    /**
     * 获取-客户端单向验证的SSLSocketFactory
     * 使用系统默认的客户端密钥库验证做单向验证
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    public static SSLSocketFactory mGetClientVerifySSLSocketFactory() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        //使用系统默认的客户端密钥库验证做单向验证
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        //系统默认证书
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustManagerFactory.init(keyStore);
        //需要服务端验证客户端的证书，new SecureRandom()生成的随机数
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
        return sslContext.getSocketFactory();
    }

    /**
     * 获取-客户端单向验证的SSLSocketFactory
     * 使用服务端给的证书
     * @param context
     * @param alias
     * @param clintCrtName 服务端给的证书 例如 xx.cer
     * @return
     * @throws Exception
     */
    public static SSLSocketFactory mGetCrtClientVerifySSLSocketFactory(Context context, String alias,String clintCrtName) throws Exception{
        //获取证书
        InputStream stream = context.getAssets().open(clintCrtName);
        SSLContext sslContext = SSLContext.getInstance("SSL");
        //使用默认证书
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        //去掉系统默认证书
        keystore.load(null);
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(stream);
        //设置自己的证书
        keystore.setCertificateEntry(alias, certificate);
        //通过信任管理器获取一个默认的算法
        String algorithm = TrustManagerFactory.getDefaultAlgorithm();
        //算法工厂创建
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(algorithm);
        trustManagerFactory.init(keystore);
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
        return sslContext.getSocketFactory();
    }

    /**
     * 获取 双向验证 的SSLSocketFactory
     * @param context
     * @param clintBKSName 客服端  xx.kbs
     * @param clintKbsPwd
     * @param serverBksName 服务端 xx.kbs
     * @param serverKbsPwd
     * @return
     * @throws Exception
     */
    public static SSLSocketFactory mGetClientHostVerify2SSLSocketFactory(Context context, String clintBKSName, String clintKbsPwd, String serverBksName, String serverKbsPwd) throws Exception{
        //客户端证书
        KeyStore clientKey = KeyStore.getInstance("BKS");
        InputStream ksIn = context.getAssets().open(clintBKSName);
        clientKey.load(ksIn,clintKbsPwd.toCharArray());
        ksIn.close();
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
        keyManagerFactory.init(clientKey, clintKbsPwd.toCharArray());
        //服务器端证书
        KeyStore serverKey = KeyStore.getInstance("BKS");
        InputStream tsIn = context.getAssets().open(serverBksName);
        serverKey.load(tsIn,serverKbsPwd.toCharArray());
        tsIn.close();
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
        trustManagerFactory.init(serverKey);
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
        return sslContext.getSocketFactory();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static X509ExtendedTrustManager mGetX509ExtendedTrustManager(){
        return new X509ExtendedTrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    private static X509TrustManager mGetX509TrustManager(){
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
}
