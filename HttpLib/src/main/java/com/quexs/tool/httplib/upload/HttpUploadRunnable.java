package com.quexs.tool.httplib.upload;

import android.text.TextUtils;

import com.quexs.tool.httplib.log.HttpLogBean;
import com.quexs.tool.httplib.log.HttpPrintLog;
import com.quexs.tool.httplib.ssl.HttpSSLVerify;
import com.quexs.tool.httplib.ssl.SSLSocketFactoryTool;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * 文件上传
 */
public class HttpUploadRunnable implements Runnable, Comparable<HttpUploadRunnable>{
    //防重复Key
    private final Object objKey;
    //优先级
    private final long priority;
    //排序默认升序
    private final String sort;

    //下载链接
    private String url;
    //上传文件
    private File uploadFile;
    //上传参数
    private Map<String, Object> params;
    //题头属性
    private Map<String, String> properties;
    //SSL验证编码
    private @HttpSSLVerify.VerifyCode int verifyCode = HttpSSLVerify.VerifyCode.DEFAULT_VERIFY;
    //上传监听
    private HttpUploadListener httpUploadListener;
    //上传结束监听
    private HttpUploadRunnableEndListener httpUploadRunnableEndListener;
    //取消上传
    private boolean isCancelUpload;

    // 换行，或者说是回车
    private final String newLine = "\r\n";
    // 固定的前缀
    private final String preFix = "--";
    // 分界线，就是上面提到的boundary，可以是任意字符串，建议写长一点，这里简单的写了一个#
    private final String bounDary = "----WebKitFormBoundaryCXRtmcVNK0H70msG";

    public HttpUploadRunnable(Object objKey, long priority, String sort) {
        this.objKey = objKey;
        this.priority = priority;
        this.sort = sort;
    }

    /**
     * 请求上传配置
     * @param url
     * @param uploadFile
     * @param params
     * @param properties
     */
    public void setUploadParams(String url, File uploadFile, Map<String, Object> params, Map<String, String> properties){
        this.url = url;
        this.uploadFile = uploadFile;
        this.params = params;
        this.properties = properties;
    }

    /**
     * 上传监听
     * @param httpUploadListener
     */
    public void setHttpUploadListener(HttpUploadListener httpUploadListener) {
        this.httpUploadListener = httpUploadListener;
    }

    /**
     * 上传结束监听
     * @param httpUploadRunnableEndListener
     */
    public void setHttpUploadRunnableEndListener(HttpUploadRunnableEndListener httpUploadRunnableEndListener) {
        this.httpUploadRunnableEndListener = httpUploadRunnableEndListener;
    }

    /**
     * 设置SSL验证编码
     * @param verifyCode
     */
    public void setVerifyCode(@HttpSSLVerify.VerifyCode int verifyCode) {
        this.verifyCode = verifyCode;
    }

    /**
     * 取消上传
     */
    public void cancelUpload(){
        isCancelUpload = true;
    }

    public Object getObjKey() {
        return objKey;
    }

    @Override
    public int compareTo(HttpUploadRunnable o) {
        //降序
        if(!TextUtils.isEmpty(sort) && "DESC".equalsIgnoreCase(sort)) return Long.compare(o.priority, priority);
        //升序
        return Long.compare(priority, o.priority);
    }

    @Override
    public void run() {
        if(isCancelUpload){
            if(httpUploadRunnableEndListener != null){
                httpUploadRunnableEndListener.endUpload(objKey);
            }
            return;
        }
        if(httpUploadListener != null){
            httpUploadListener.uploadStart(objKey);
        }
        HttpsURLConnection conn = null;
        int responseCode = -1;
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
            //设置是否向httpURLConnection输出
            conn.setDoOutput(true);
            //设置请求方法默认为POST
            conn.setRequestMethod("POST");
            //Post请求不能使用缓存
            conn.setUseCaches(false);
            //配置属性
            //文件上传固定属性
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + bounDary);
            conn.setRequestProperty("User-Agent", "(Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.84 Safari/537.36)");
            //默认表单请求
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            //添加属性
            addRequestProperty(conn,properties);
            //写入数据
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            //添加请求参数
            addRequestParams(dos, params);
            //写入上传文件
            writeUploadFile(dos, uploadFile);
            //结束流
            dos.writeBytes(preFix + bounDary + preFix + newLine);
            //请求完成后关闭流
            dos.flush();
            //请求结果回调编码
            responseCode = conn.getResponseCode();
            if(responseCode != HttpsURLConnection.HTTP_OK) throw new IOException("responseCode=" + responseCode);
            //读取主机返回的数据
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                builder.append(line);
            }
            in.close();
            //回调消息体
            String body = builder.toString();
            //打印日志
            HttpPrintLog.getInstance().addPrintLog(new HttpLogBean(url, "POST", conn.getRequestProperties(), responseCode, body));
            if (httpUploadListener != null) {
                httpUploadListener.uploadComplete(body, objKey);
            }
        }catch (Exception e){
            Map<String, List<String>> requestProperties = null;
            if(conn != null){
                requestProperties = conn.getRequestProperties();
                try {
                    responseCode = conn.getResponseCode();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            //打印日志
            HttpPrintLog.getInstance().addPrintLog(new HttpLogBean(url, "POST", requestProperties, responseCode, e));
            if (httpUploadListener != null) {
                httpUploadListener.uploadError(e, objKey);
            }
        }finally {
            if (conn != null) {
                conn.disconnect();
            }
            if(httpUploadRunnableEndListener != null){
                httpUploadRunnableEndListener.endUpload(objKey);
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
     * 设置RequestProperty
     * @param properties
     */
    private void addRequestProperty(HttpsURLConnection conn, Map<String, String> properties){
        if(properties == null || properties.isEmpty()) return;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            if(key.equals("connectTimeout") || key.equals("readTimeout")){
                conn.setConnectTimeout(Integer.parseInt(entry.getValue()));
                continue;
            }
            conn.addRequestProperty(key, entry.getValue());
        }
    }

    /**
     * 添加请求参数
     */
    private void addRequestParams(DataOutputStream dos, Map<String, Object> params) throws IOException {
        if(params == null || params.isEmpty()) return;
        for(Map.Entry<String, Object> entry : params.entrySet()) {
            //获取参数名称和值
            String key = entry.getKey();
            Object value = params.get(key);
            //向请求中写分割线
            dos.writeBytes(preFix + bounDary + newLine);
            //向请求拼接参数
            dos.writeBytes("Content-Disposition: form-data; " + "name=\"" + URLEncoder.encode(key, "utf-8") + "\"" + newLine);
            //向请求中拼接空格
            dos.writeBytes(newLine);
            //写入值
            dos.writeBytes(URLEncoder.encode(String.valueOf(value), "utf-8"));
            //向请求中拼接空格
            dos.writeBytes(newLine);
        }
    }

    /**
     * 写入上传文件
     * @param dos
     * @param file
     */
    private void writeUploadFile(DataOutputStream dos, File file) throws IOException {
        if(file == null) return;
        String filePrams = "file";
        String fileName = file.getName();
        //向请求中加入分隔符号
        dos.write((preFix + bounDary + newLine).getBytes());
        //将byte写入
        dos.writeBytes("Content-Disposition: form-data; " + "name=\"" + URLEncoder.encode(filePrams, "utf-8") + "\"" + ";filename=\"" + URLEncoder.encode(fileName, "utf-8") + "\"" + newLine);
        dos.writeBytes(newLine);
        //把file装换成byte
        File upLoadFile = new File(file.toURI());
        InputStream in = new FileInputStream(upLoadFile);
        byte[] buff = new byte[100];
        while (in.read(buff, 0, 100) != -1){
            //写入流文件
            dos.write(buff);
        }
        //向请求中拼接空格
        dos.writeBytes(newLine);
        //关闭文件流文件
        in.close();
    }


    public interface HttpUploadRunnableEndListener{
        void endUpload(Object objKey);
    }

}
