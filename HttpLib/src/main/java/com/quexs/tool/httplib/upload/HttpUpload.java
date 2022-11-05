package com.quexs.tool.httplib.upload;

import android.util.ArrayMap;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Http 文件下载类
 */
public class HttpUpload {

    private final ThreadPoolExecutor threadPool;
    private final ArrayMap<Object, HttpUploadRunnable> keyMap = new ArrayMap<>();
    private final ReentrantLock lock;


    public HttpUpload(){
        int cpuCount = Runtime.getRuntime().availableProcessors();
        //核心线程总数 设定为3个
        int corPoolSize = Math.min(cpuCount * 2, 3);
        //线程空闲后的存活时长 1 秒
        long keepAliveTime = 1L;
        threadPool = new ThreadPoolExecutor(corPoolSize,corPoolSize, keepAliveTime, TimeUnit.SECONDS, new PriorityBlockingQueue<>());
        //允许核心线程超时关闭
        threadPool.allowCoreThreadTimeOut(true);
        //公平锁
        lock = new ReentrantLock();
    }

    /**
     * 添加下载
     * @param httpUploadRunnable
     * @param httpUploadListener
     */
    public void addUpload(HttpUploadRunnable httpUploadRunnable, HttpUploadListener httpUploadListener){
        lock.lock();
        HttpUploadRunnable exitHttpUploadRunnable = keyMap.get(httpUploadRunnable.getObjKey());
        if(exitHttpUploadRunnable != null){
            exitHttpUploadRunnable.setHttpUploadListener(httpUploadListener);
        }else {
            httpUploadRunnable.setHttpUploadRunnableEndListener(new HttpUploadRunnable.HttpUploadRunnableEndListener() {
                @Override
                public void endUpload(Object objKey) {
                    keyMap.remove(objKey);
                }
            });
            httpUploadRunnable.setHttpUploadListener(httpUploadListener);
            keyMap.put(httpUploadRunnable.getObjKey(),httpUploadRunnable);
            threadPool.execute(httpUploadRunnable);
        }
        lock.unlock();
    }

    /**
     * 取消下载
     * @param objKey null 移除所有线程
     */
    public void cancelUpload(Object objKey){
        lock.lock();
        if(objKey == null){
            Iterator<Map.Entry<Object, HttpUploadRunnable>> it = keyMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Object,HttpUploadRunnable> entry = it.next();
                HttpUploadRunnable httpUploadRunnable = entry.getValue();
                //取消下载请求
                httpUploadRunnable.cancelUpload();
                //移出key
                it.remove();
            }
        }else {
            //下载线程
            HttpUploadRunnable httpUploadRunnable = keyMap.remove(objKey);
            if(httpUploadRunnable != null){
                httpUploadRunnable.cancelUpload();
            }
        }
    }

    /**
     * 关闭线程池
     */
    public void close(){
        threadPool.shutdown();
        cancelUpload(null);
    }

}
