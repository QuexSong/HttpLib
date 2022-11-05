package com.quexs.tool.httplib.upload;

public interface HttpUploadListener {
    void uploadStart(Object objKey);
    void uploadComplete(String body,Object objKey);
    void uploadError(Exception e,Object objKey);

}
