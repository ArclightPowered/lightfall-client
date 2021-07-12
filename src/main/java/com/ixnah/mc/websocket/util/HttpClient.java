package com.ixnah.mc.websocket.util;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.concurrent.Future;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2020/7/10 9:07
 */
public interface HttpClient {
    Future<HttpResponse> sendRequest(HttpRequest request);
}
