package com.quexs.tool.httplib.ssl;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class HttpSSLVerify {

    @IntDef({VerifyCode.DEFAULT_VERIFY,
            VerifyCode.SKIP_VERIFY,
            VerifyCode.CLIENT_VERIFY,
            VerifyCode.CLIENT_CLINT_VERIFY,
            VerifyCode.CLIENT_HOST_CERTIFICATE_VERIFY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface VerifyCode{
        //默认
        int DEFAULT_VERIFY = 0;
        //跳过验证
        int SKIP_VERIFY = 1;
        //客户端单向验证-客户端使用默认验证
        int CLIENT_VERIFY = 2;
        //客户端单向验证-客户端使用服务端给的证书
        int CLIENT_CLINT_VERIFY = 3;
        //客户端和主机双向验证-客户端使用证书验证
        int CLIENT_HOST_CERTIFICATE_VERIFY = 4;

    }
}
