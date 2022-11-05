package com.quexs.tool.httplib.download;

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
public class HttpDownload {

    private final ThreadPoolExecutor threadPool;
    private final ArrayMap<Object, HttpDownloadRunnable> keyMap = new ArrayMap<>();
    private final ReentrantLock lock;

    public HttpDownload(){
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
     * @param httpDownloadRunnable
     * @param httpDownloadListener
     */
    public void addDownload(HttpDownloadRunnable httpDownloadRunnable, HttpDownloadListener httpDownloadListener){
        lock.lock();
        HttpDownloadRunnable exitHttpDownloadRunnable = keyMap.get(httpDownloadRunnable.getObjKey());
        if(exitHttpDownloadRunnable != null){
            exitHttpDownloadRunnable.setHttpDownloadListener(httpDownloadListener);
        }else {
            httpDownloadRunnable.setHttpDownloadRunnableEndListener(new HttpDownloadRunnable.HttpDownloadRunnableEndListener() {
                @Override
                public void endDownload(Object objKey) {
                    keyMap.remove(objKey);
                }
            });
            httpDownloadRunnable.setHttpDownloadListener(httpDownloadListener);
            keyMap.put(httpDownloadRunnable.getObjKey(),httpDownloadRunnable);
            threadPool.execute(httpDownloadRunnable);
        }
        lock.unlock();
    }

    /**
     * 取消下载
     * @param objKey null 移除所有线程
     */
    public void cancelDownload(Object objKey){
        lock.lock();
        if(objKey == null){
            Iterator<Map.Entry<Object, HttpDownloadRunnable>> it = keyMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<Object,HttpDownloadRunnable> entry = it.next();
                HttpDownloadRunnable httpDownloadRunnable = entry.getValue();
                //取消下载请求
                httpDownloadRunnable.cancelDownload();
                //移出key
                it.remove();
            }
        }else {
            HttpDownloadRunnable httpDownloadRunnable = keyMap.remove(objKey);
            if(httpDownloadRunnable != null){
                httpDownloadRunnable.cancelDownload();
            }
        }
        lock.unlock();
    }

    /**
     * 关闭线程池
     */
    public void close(){
        threadPool.shutdown();
        cancelDownload(null);
    }

}
