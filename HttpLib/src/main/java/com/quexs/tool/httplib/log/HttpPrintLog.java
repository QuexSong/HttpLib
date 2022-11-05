package com.quexs.tool.httplib.log;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public class HttpPrintLog {

    private static volatile HttpPrintLog instance;
    private final ThreadPoolExecutor threadPool;
    private boolean isEnablePrintLog;
    private String httpLogTag = "HttpLog";

    public static HttpPrintLog getInstance() {
        if (instance == null) {
            synchronized (HttpPrintLog.class) {
                if (instance == null) {
                    instance = new HttpPrintLog();
                }
            }
        }
        return instance;
    }

    public HttpPrintLog(){
        //线程空闲后的存活时长 1 秒
        long keepAliveTime = 100L;
        threadPool = new ThreadPoolExecutor(1,1, keepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        //允许核心线程超时关闭
        threadPool.allowCoreThreadTimeOut(true);
    }

    /**
     * 设置启用日志打印
     * @param isEnablePrintLog
     */
    public void setEnablePrintLog(boolean isEnablePrintLog) {
        this.isEnablePrintLog = isEnablePrintLog;
    }

    /**
     * 设置日志打印Tag
     * @param httpLogTag
     */
    public void setHttpLogTag(String httpLogTag) {
        this.httpLogTag = httpLogTag;
    }

    /**
     * 添加打印日志
     * @param httpLogBean
     */
    public void addPrintLog(HttpLogBean httpLogBean){
        if(!isEnablePrintLog) return;
        threadPool.execute(new HttpLogRunnable(httpLogBean));
    }

    /**
     * 打印常见日志
     * @param tag
     * @param msg
     */
    public void addPrintCommonLog(String tag, String msg){
        if(!isEnablePrintLog) return;
        threadPool.execute(new CommonLogRunnable(tag, msg));
    }

    /**
     * 关闭线程池
     */
    public void close(){
        threadPool.shutdown();
    }

    /**
     * http 请求 日志打印 Runnable
     */
    private class HttpLogRunnable implements Runnable{
        private final HttpLogBean httpLogBean;

        public HttpLogRunnable(HttpLogBean httpLogBean) {
            this.httpLogBean = httpLogBean;
        }

        @Override
        public void run() {
            if(httpLogBean.getResponseCode() == HttpsURLConnection.HTTP_OK){
                Log.d(httpLogTag, "╔═══════════════════════════════ REQUEST START ════════════════════════════════");
                //打印请求信息
                Log.d(httpLogTag, "║ " + httpLogBean.getUrl());
                Log.d(httpLogTag, "║ " + httpLogBean.getRequestMethod());
                Log.d(httpLogTag, "║ ╔═══════════════════ PROPERTY START ═════════════════════════════════════════");
                for (Map.Entry<String, List<String>> entry : httpLogBean.getRequestProperties().entrySet()) {
                    String key = entry.getKey();
                    List<String> properties = entry.getValue();
                    for(String property : properties){
                        Log.d(httpLogTag, "║ ║ " + key + ":" + property);
                    }
                }
                Log.d(httpLogTag, "║ ╚═══════════════════ PROPERTY END ═════════════════════════════════════════════");
                //打印回调信息
                Log.d(httpLogTag, "║ responseCode=" + httpLogBean.getResponseCode());
                //打印回调消息体
                Log.d(httpLogTag, "║ ╔════════════════════════════════ BODY START ═══════════════════════════════════");
                if(TextUtils.isEmpty(httpLogBean.getResponseResult())){
                    Log.d(httpLogTag, "║ ║ ");
                }else {
                    printJsonLog(httpLogTag,httpLogBean.getResponseResult());
                }
                Log.d(httpLogTag, "║ ╚════════════════════════════════ BODY END ═════════════════════════════════════");
                Log.d(httpLogTag, "╚═══════════════════════════════ REQUEST END ══════════════════════════════════");
            }else {
                Log.e(httpLogTag, "╔═══════════════════════════════ REQUEST START ════════════════════════════════");
                //打印请求信息
                Log.e(httpLogTag, "║ Url：" + httpLogBean.getUrl());
                Log.e(httpLogTag, "║ RequestMethod：" + httpLogBean.getRequestMethod());
                Log.e(httpLogTag, "║ ╔═══════════════════ PROPERTY START ════════════════════");
                for (Map.Entry<String, List<String>> entry : httpLogBean.getRequestProperties().entrySet()) {
                    String key = entry.getKey();
                    List<String> properties = entry.getValue();
                    for(String property : properties){
                        Log.e(httpLogTag, "║ ║ " + key + ":" + property);
                    }
                }
                Log.e(httpLogTag, "║ ╚═══════════════════ PROPERTY END ═════════════════════");
                //打印回调信息
                Log.e(httpLogTag, "║ responseCode：" + httpLogBean.getResponseCode());
                Log.e(httpLogTag, "╚═══════════════════════════════ REQUEST END ════════════════════════════════════");
                //打印请求失败异常信息
                Log.e(httpLogTag,"request error", httpLogBean.getEx());
            }
        }
    }

    private class CommonLogRunnable implements Runnable{
        private final String msg;
        private final String tag;
        public CommonLogRunnable(String tag,String msg) {
            this.tag = tag;
            this.msg = msg;
        }

        @Override
        public void run() {
            Log.d(tag, "╔═══════════════════════════════ COMMON LOG START ══════════════════════════════════════════");
            printJsonLog(tag, msg);
            Log.d(tag, "╚═══════════════════════════════ COMMON LOG END ════════════════════════════════════════════");
        }
    }

    /**
     * 打印json数据格式日志
     * @param logTag
     * @param json
     */
    private void printJsonLog(String logTag,String json){
        String msg;
        boolean isJson = false;
        try {
            JSONObject jsonObject = new JSONObject(json);
            msg = jsonObject.toString(4);
            isJson = true;
        } catch (JSONException e) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                msg = jsonArray.toString(4);
                isJson = true;
            }catch (Exception e1){
                msg = json;
            }
        }
        if(!isJson){
            Log.d(logTag, "║ ║ " + msg);
            return;
        }
        String lineSeparator = System.getProperty("line.separator");
        if(lineSeparator == null){
            lineSeparator = "\r\n|\r";
        }
        String[] lines = msg.split(lineSeparator);
        for(String line : lines){
            Log.d(logTag, "║ ║ " + line);
        }
    }

}
