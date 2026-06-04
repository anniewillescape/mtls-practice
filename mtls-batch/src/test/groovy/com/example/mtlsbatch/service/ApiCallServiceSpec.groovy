package com.example.mtlsbatch.service

import com.example.mtlsbatch.exception.TooManyRequestsException
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.retry.annotation.EnableRetry
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification
import spock.lang.Subject
import spock.mock.DetachedMockFactory

/**
 * @Retryable はSpring AOPプロキシ経由でのみ動作するため、
 * Springコンテキストを起動して apiCallService を @Autowired で受け取る。
 * CloseableHttpClient は DetachedMockFactory でSpockモックとしてBeanに登録する。
 * モックは Spring Bean (シングルトン) だが、spock-spring が各テスト前後に
 * インタラクション状態を自動リセットするためテスト間の汚染は発生しない。
 */
@ContextConfiguration(classes = [ApiCallServiceSpec.TestConfig])
@TestPropertySource(properties = [
        "batch.api.retry.max-attempts=3",
        "batch.api.retry.initial-delay-ms=100"   // テスト高速化のため短縮
])
class ApiCallServiceSpec extends Specification {

    @Autowired
    @Subject
    ApiCallService apiCallService

    @Autowired
    CloseableHttpClient httpClient

    // ── callGet: 成功ケース (parameterized) ──────────────────────────────────

    def "callGet: #failCount 回429の後に成功 — #failCount 回リトライ"() {
        given:
        def callCount = 0
        httpClient.execute(_ as HttpGet, null, _ as HttpClientResponseHandler) >> { args ->
            if (++callCount <= failCount) throw new TooManyRequestsException("url")
        }

        when:
        apiCallService.callGet("https://localhost:8443/api/hello")

        then:
        noExceptionThrown()
        callCount == failCount + 1

        where:
        failCount << [0, 1, 2]
    }

    // ── callGet: 失敗ケース ──────────────────────────────────────────────────

    def "callGet: 3回連続429 — maxAttempts消化後にRuntimeExceptionをスロー"() {
        given:
        def callCount = 0
        httpClient.execute(_ as HttpGet, null, _ as HttpClientResponseHandler) >> { args ->
            callCount++
            throw new TooManyRequestsException("url")
        }

        when:
        apiCallService.callGet("https://localhost:8443/api/hello")

        then:
        def e = thrown(RuntimeException)
        e.cause instanceof TooManyRequestsException
        e.message.contains("Rate limit retry exhausted")
        callCount == 3
    }

    def "callGet: IOException発生 — リトライせず即時スロー"() {
        given:
        def callCount = 0
        httpClient.execute(_ as HttpGet, null, _ as HttpClientResponseHandler) >> { args ->
            callCount++
            throw new IOException("connection refused")
        }

        when:
        apiCallService.callGet("https://localhost:8443/api/hello")

        then:
        // noRetryFor = Exception.class により IOException はリトライされない (callCount == 1)
        // @Recover にマッチするメソッドがないため ExhaustedRetryException でラップされて伝播する
        def e = thrown(RuntimeException)
        e.cause instanceof IOException
        callCount == 1
    }

    def "callGet: 指数関数バックオフで2回目の待機時間が1回目より長い"() {
        given:
        def timestamps = []
        def callCount = 0
        httpClient.execute(_ as HttpGet, null, _ as HttpClientResponseHandler) >> { args ->
            timestamps << System.currentTimeMillis()
            if (++callCount < 3) throw new TooManyRequestsException("url")
        }

        when:
        apiCallService.callGet("https://localhost:8443/api/hello")

        then:
        noExceptionThrown()
        timestamps.size() == 3
        // initial-delay-ms=100, multiplier=2.0 → delay[0→1]≈100ms, delay[1→2]≈200ms
        (timestamps[2] - timestamps[1]) > (timestamps[1] - timestamps[0])
    }

    // ── callPost: 成功ケース (parameterized) ─────────────────────────────────

    def "callPost: #failCount 回429の後に成功 — #failCount 回リトライ"() {
        given:
        def callCount = 0
        httpClient.execute(_ as HttpPost, null, _ as HttpClientResponseHandler) >> { args ->
            if (++callCount <= failCount) throw new TooManyRequestsException("url")
        }

        when:
        apiCallService.callPost("https://localhost:8443/api/echo", '{"key":"value"}')

        then:
        noExceptionThrown()
        callCount == failCount + 1

        where:
        failCount << [0, 1, 2]
    }

    // ── callPost: 失敗ケース ─────────────────────────────────────────────────

    def "callPost: 3回連続429 — maxAttempts消化後にRuntimeExceptionをスロー"() {
        given:
        def callCount = 0
        httpClient.execute(_ as HttpPost, null, _ as HttpClientResponseHandler) >> { args ->
            callCount++
            throw new TooManyRequestsException("url")
        }

        when:
        apiCallService.callPost("https://localhost:8443/api/echo", '{"key":"value"}')

        then:
        def e = thrown(RuntimeException)
        e.cause instanceof TooManyRequestsException
        e.message.contains("Rate limit retry exhausted")
        callCount == 3
    }

    // ── Spring Test設定 ───────────────────────────────────────────────────────

    @Configuration
    @EnableRetry
    static class TestConfig {
        private final DetachedMockFactory factory = new DetachedMockFactory()

        /** ${...} プレースホルダーを解決するために必要 */
        @Bean
        static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
            new PropertySourcesPlaceholderConfigurer()
        }

        @Bean
        CloseableHttpClient httpClient() {
            factory.Mock(CloseableHttpClient)
        }

        @Bean
        ApiCallService apiCallService(CloseableHttpClient httpClient) {
            new ApiCallService(httpClient)
        }
    }
}
