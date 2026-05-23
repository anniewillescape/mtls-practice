package com.example.mtlsbatch.batch

import com.example.mtlsbatch.config.ApiProperties
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import org.apache.hc.core5.http.protocol.HttpContext
import spock.lang.Specification
import spock.lang.Subject

class ApiBatchRunnerSpec extends Specification {

    CloseableHttpClient httpClient = Mock()
    ApiProperties api = new ApiProperties("https://localhost:8443")

    @Subject
    ApiBatchRunner runner = new ApiBatchRunner(httpClient, api)

    def "run() calls GET and POST exactly once"() {
        given:
        httpClient.execute(_ as HttpGet, _ as HttpContext, _ as HttpClientResponseHandler) >> "GET response"
        httpClient.execute(_ as HttpPost, _ as HttpContext, _ as HttpClientResponseHandler) >> "POST response"

        when:
        runner.run()

        then:
        1 * httpClient.execute(_ as HttpGet, _ as HttpContext, _ as HttpClientResponseHandler)
        1 * httpClient.execute(_ as HttpPost, _ as HttpContext, _ as HttpClientResponseHandler)
    }

    def "GET request URL is built correctly from baseUrl"() {
        given:
        httpClient.execute(_ as HttpGet, _ as HttpContext, _ as HttpClientResponseHandler) >> "hello"
        httpClient.execute(_ as HttpPost, _ as HttpContext, _ as HttpClientResponseHandler) >> "echo"

        when:
        runner.run()

        then:
        1 * httpClient.execute(
            { HttpGet req -> req.uri.toString() == "https://localhost:8443/api/hello" },
            _ as HttpContext, _ as HttpClientResponseHandler
        )
    }

    def "POST request URL is built correctly from baseUrl"() {
        given:
        httpClient.execute(_ as HttpGet, _ as HttpContext, _ as HttpClientResponseHandler) >> "hello"
        httpClient.execute(_ as HttpPost, _ as HttpContext, _ as HttpClientResponseHandler) >> "echo"

        when:
        runner.run()

        then:
        1 * httpClient.execute(
            { HttpPost req -> req.uri.toString() == "https://localhost:8443/api/echo" },
            _ as HttpContext, _ as HttpClientResponseHandler
        )
    }

    def "correct paths are appended regardless of baseUrl"() {
        given:
        def customApi = new ApiProperties("https://example.com:9443")
        def customRunner = new ApiBatchRunner(httpClient, customApi)
        httpClient.execute(_ as HttpGet, _ as HttpContext, _ as HttpClientResponseHandler) >> "hello"
        httpClient.execute(_ as HttpPost, _ as HttpContext, _ as HttpClientResponseHandler) >> "echo"

        when:
        customRunner.run()

        then:
        1 * httpClient.execute(
            { HttpGet req -> req.uri.toString() == "https://example.com:9443/api/hello" },
            _ as HttpContext, _ as HttpClientResponseHandler
        )
        1 * httpClient.execute(
            { HttpPost req -> req.uri.toString() == "https://example.com:9443/api/echo" },
            _ as HttpContext, _ as HttpClientResponseHandler
        )
    }
}
