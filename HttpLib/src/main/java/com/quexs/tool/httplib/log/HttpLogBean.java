package com.quexs.tool.httplib.log;

import java.util.List;
import java.util.Map;

/**
 * http打印日志
 */
public class HttpLogBean {
    private String url;
    private String requestMethod;
    private Map<String, List<String>> requestProperties;
    private int responseCode;
    private Exception ex;
    private String responseResult;

    public HttpLogBean(String url, String requestMethod, Map<String, List<String>> requestProperties, int responseCode, Exception ex) {
        this.url = url;
        this.requestMethod = requestMethod;
        this.requestProperties = requestProperties;
        this.responseCode = responseCode;
        this.ex = ex;
    }

    public HttpLogBean(String url, String requestMethod, Map<String, List<String>> requestProperties, int responseCode, String responseResult) {
        this.url = url;
        this.requestMethod = requestMethod;
        this.requestProperties = requestProperties;
        this.responseCode = responseCode;
        this.responseResult = responseResult;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Map<String, List<String>> getRequestProperties() {
        return requestProperties;
    }

    public void setRequestProperties(Map<String, List<String>> requestProperties) {
        this.requestProperties = requestProperties;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseResult() {
        return responseResult;
    }

    public void setResponseResult(String responseResult) {
        this.responseResult = responseResult;
    }

    public Exception getEx() {
        return ex;
    }

    public void setEx(Exception ex) {
        this.ex = ex;
    }
}
