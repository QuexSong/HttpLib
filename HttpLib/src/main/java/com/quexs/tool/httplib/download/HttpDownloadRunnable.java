package com.quexs.tool.httplib.download;

import android.text.TextUtils;

import com.quexs.tool.httplib.log.HttpLogBean;
import com.quexs.tool.httplib.log.HttpPrintLog;
import com.quexs.tool.httplib.ssl.HttpSSLVerify;
import com.quexs.tool.httplib.ssl.SSLSocketFactoryTool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * 下载 Runnable
 */
public class HttpDownloadRunnable implements Runnable, Comparable<HttpDownloadRunnable>{
    //防重复Key
    private final Object objKey;
    //优先级
    private final long priority;
    //排序默认升序
    private final String sort;

    //下载链接
    private String url;
    //保存文件路径
    private String saveFilePath;

    //下载监听
    private HttpDownloadListener httpDownloadListener;
    //取消下载
    private boolean isCancelDownload;
    //是否断点续传
    private boolean isRange;
    //下载结束监听
    private HttpDownloadRunnableEndListener httpDownloadRunnableEndListener;
    //SSL验证编码
    private @HttpSSLVerify.VerifyCode int verifyCode = HttpSSLVerify.VerifyCode.DEFAULT_VERIFY;

    /**
     *
     * @param objKey 防重复下载key只
     * @param priority 优先级
     * @param sort 排序
     */
    public HttpDownloadRunnable(Object objKey, long priority, String sort){
        this.objKey = objKey;
        this.priority = priority;
        this.sort = sort;
    }

    /**
     *
     * @param url 下载链接
     * @param saveFilePath 文件保存地址
     */
    public void setDownloadParams(String url, String saveFilePath){
        this.url = url;
        this.saveFilePath = saveFilePath;
    }

    /**
     * 下载监听
     * @param httpDownloadListener
     */
    public void setHttpDownloadListener(HttpDownloadListener httpDownloadListener) {
        this.httpDownloadListener = httpDownloadListener;
    }

    /**
     * 下载结束监听
     * @param httpDownloadRunnableEndListener
     */
    public void setHttpDownloadRunnableEndListener(HttpDownloadRunnableEndListener httpDownloadRunnableEndListener) {
        this.httpDownloadRunnableEndListener = httpDownloadRunnableEndListener;
    }

    /**
     * 设置验证编码
     * @param verifyCode
     */
    public void setVerifyCode(@HttpSSLVerify.VerifyCode int verifyCode) {
        this.verifyCode = verifyCode;
    }

    /**
     * 启动断点续传
     */
    public void enableRange() {
        isRange = true;
    }

    /**
     * 取消下载
     */
    public void cancelDownload() {
        isCancelDownload = true;
    }

    public Object getObjKey() {
        return objKey;
    }

    @Override
    public int compareTo(HttpDownloadRunnable o) {
        //降序
        if(!TextUtils.isEmpty(sort) && "DESC".equalsIgnoreCase(sort)) return Long.compare(o.priority, priority);
        //升序
        return Long.compare(priority, o.priority);
    }

    @Override
    public void run() {
        if(isCancelDownload){
            if(httpDownloadRunnableEndListener != null){
                httpDownloadRunnableEndListener.endDownload(objKey);
            }
            return;
        }
        if(httpDownloadListener != null){
            httpDownloadListener.downloadStart(objKey);
        }
        HttpsURLConnection conn = null;
        try {
            URL connUrl = new URL(url);
            conn = (HttpsURLConnection) connUrl.openConnection();
            //SSL验证规则
            runVerify(conn);
            //设置连接主机超时（单位：毫秒）
            //连接主机超时时间（单位：毫秒）
            int connectTimeout = 10 * 1000;
            conn.setConnectTimeout(connectTimeout);
            //设置从主机读取数据超时（单位：毫秒）
            //设置从主机读取数据超时时间（单位：毫秒）
            int readTimeout = 10 * 1000;
            conn.setReadTimeout(readTimeout);
            //默认表单请求
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            //GET请求
            conn.setRequestMethod("GET");
            //文件下载
            boolean isDownloadComplete = isRange ? runRangeDownload(conn) : runDownload(conn);
            //打印日志
            HttpPrintLog.getInstance().addPrintLog(new HttpLogBean(url, "GET", conn.getRequestProperties(), conn.getResponseCode(), isDownloadComplete ? "Download Complete" : "Cancel Download"));
        }catch (Exception e){
            Map<String, List<String>> requestProperties = null;
            int responseCode = -1;
            if(conn != null){
                requestProperties = conn.getRequestProperties();
                try {
                    responseCode = conn.getResponseCode();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            //打印日志
            HttpPrintLog.getInstance().addPrintLog(new HttpLogBean(url, "GET", requestProperties, responseCode, e));
            if (httpDownloadListener != null) {
                httpDownloadListener.downloadError(e, objKey);
            }
        }finally {
            if (conn != null) {
                conn.disconnect();
            }
            if(httpDownloadRunnableEndListener != null){
                httpDownloadRunnableEndListener.endDownload(objKey);
            }
        }
    }

    /**
     * 执行SSL验证（目前只支持跳过验证、客户端单向验证）
     * @param conn
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    private void runVerify(HttpsURLConnection conn) throws Exception {
        if(verifyCode != HttpSSLVerify.VerifyCode.DEFAULT_VERIFY){
            if(verifyCode == HttpSSLVerify.VerifyCode.CLIENT_VERIFY){
                //使用客户端默认密钥库验证-单向验证
                conn.setSSLSocketFactory(SSLSocketFactoryTool.mGetClientVerifySSLSocketFactory());
            }else {
                //跳过所有验证
                conn.setSSLSocketFactory(SSLSocketFactoryTool.mGetSkipVerifySSLSocketFactory());
            }
            //主机验证-默认true不验证
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }
    }

    /**
     * 执行下载
     * @param conn
     * @throws Exception
     */
    private boolean runDownload(HttpsURLConnection conn) throws Exception {
        //连接
        conn.connect();
        if(conn.getResponseCode() != HttpURLConnection.HTTP_OK) throw new Exception("request result code:" + conn.getResponseCode());
        int fileLen = conn.getContentLength();
        InputStream is = conn.getInputStream();
        File file = new File(saveFilePath);
        if(file.exists() && !file.delete()) throw new Exception("old File delete error");
        if(!file.createNewFile()) throw new Exception("File create error");
        FileOutputStream fos = new FileOutputStream(file);
        byte[] b = new byte[1024];
        int dlCount = 0;//已下载量
        int len;
        while (!isCancelDownload && (len = is.read(b)) != -1){
            fos.write(b, 0, len);
            dlCount += len;
            //回调下载进度
            if(httpDownloadListener != null){
                httpDownloadListener.downloadProgress(dlCount, fileLen, objKey);
            }
        }
        fos.flush();
        fos.close();
        is.close();
        if(isCancelDownload) return false;
        if(httpDownloadListener != null){
            httpDownloadListener.downloadComplete(saveFilePath,saveFilePath);
        }
        return true;
    }

    /**
     * 断点续传下载
     */
    private boolean runRangeDownload(HttpsURLConnection conn) throws Exception {
        File file = new File(saveFilePath);
        if(file.exists()){
            // 设置断点续传的开始位置
            conn.setRequestProperty("Range", "bytes=" + file.length() + "-");
        }else {
            file.createNewFile();
        }
        //连接
        conn.connect();
        if(conn.getResponseCode() != HttpURLConnection.HTTP_OK
                && conn.getResponseCode() != HttpURLConnection.HTTP_PARTIAL) throw new Exception("request result code:" + conn.getResponseCode());
        //已下载量
        int dlCount = (int)file.length();
        int fileLen = conn.getContentLength() + dlCount;
        InputStream is = conn.getInputStream();
        //true表示将会从断点处追加。
        FileOutputStream fos = new FileOutputStream(file, true);
        byte[] b = new byte[1024];
        int len;
        while (!isCancelDownload && (len = is.read(b)) != -1){
            fos.write(b, 0, len);
            dlCount += len;
            //回调下载进度
            if(httpDownloadListener != null){
                httpDownloadListener.downloadProgress(dlCount, fileLen, objKey);
            }
        }
        fos.flush();
        fos.close();
        is.close();
        if(isCancelDownload) return false;
        if(httpDownloadListener != null){
            httpDownloadListener.downloadComplete(saveFilePath,saveFilePath);
        }
        return true;
    }

    public interface HttpDownloadRunnableEndListener{
        void endDownload(Object objKey);
    }

}
