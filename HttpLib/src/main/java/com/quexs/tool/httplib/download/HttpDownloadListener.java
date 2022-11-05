package com.quexs.tool.httplib.download;

public interface HttpDownloadListener {
    void downloadStart(Object objKey);
    void downloadProgress(long downloadLen, int fileLen, Object objKey);
    void downloadComplete(String filePath,Object objKey);
    void downloadError(Exception e,Object objKey);

}
