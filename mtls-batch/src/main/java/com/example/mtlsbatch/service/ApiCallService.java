package com.example.mtlsbatch.service;

import com.example.mtlsbatch.exception.TooManyRequestsException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;

// クラスレベルに置くことで callGet・callPost 両方に同じポリシーが適用される。
// @Recover アノテーションが付いたメソッドは Spring Retry によって除外されるため
// recoverGet/recoverPost にリトライは適用されない。
//
// noRetryFor = Exception.class の優先順位:
//   retryFor に直接列挙された TooManyRequestsException は true として登録され、
//   Exception.class (false) より先にマッチするためリトライ対象のまま。
//   IOException など retryFor に含まれない例外は Exception を辿って false となり、
//   リトライされずに即時伝播する。
@Service
@Retryable(
        retryFor = TooManyRequestsException.class,
        noRetryFor = Exception.class,
        maxAttemptsExpression = "${batch.api.retry.max-attempts}",
        backoff = @Backoff(
                delayExpression = "${batch.api.retry.initial-delay-ms}",
                multiplier = 2.0
        )
)
public class ApiCallService {

    private static final Logger log = LoggerFactory.getLogger(ApiCallService.class);

    private final CloseableHttpClient httpClient;

    public ApiCallService(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void callGet(String url) throws IOException {
        httpClient.execute(new HttpGet(url), (HttpContext) null, response -> {
            int code = response.getCode();
            log.info("[GET] Response status: {}", code);
            if (code == HttpStatus.SC_TOO_MANY_REQUESTS) {
                log.warn("[GET] Rate limited (429), will retry...");
                throw new TooManyRequestsException(url);
            }
            log.info("[GET] Response body: {}", EntityUtils.toString(response.getEntity()));
            return null;
        });
    }

    public void callPost(String url, String json) throws IOException {
        HttpPost post = new HttpPost(url);
        post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        httpClient.execute(post, (HttpContext) null, response -> {
            int code = response.getCode();
            log.info("[POST] Response status: {}", code);
            if (code == HttpStatus.SC_TOO_MANY_REQUESTS) {
                log.warn("[POST] Rate limited (429), will retry...");
                throw new TooManyRequestsException(url);
            }
            log.info("[POST] Response body: {}", EntityUtils.toString(response.getEntity()));
            return null;
        });
    }

    // Spring Retry の制約: @Retryable メソッドと同じ返り値型(void)が必要なため
    // void を宣言しているが、このメソッドは常に例外をスローする。
    @Recover
    public void recoverGet(TooManyRequestsException e, String url) {
        log.error("[GET] All retry attempts exhausted for {}.", url);
        throw new RuntimeException("Rate limit retry exhausted for GET " + url, e);
    }

    @Recover
    public void recoverPost(TooManyRequestsException e, String url, String json) {
        log.error("[POST] All retry attempts exhausted for {}.", url);
        throw new RuntimeException("Rate limit retry exhausted for POST " + url, e);
    }
}
