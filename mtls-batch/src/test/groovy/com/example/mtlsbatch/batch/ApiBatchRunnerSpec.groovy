package com.example.mtlsbatch.batch

import com.example.mtlsbatch.config.ApiProperties
import com.example.mtlsbatch.service.ApiCallService
import spock.lang.Specification
import spock.lang.Subject

class ApiBatchRunnerSpec extends Specification {

    ApiCallService apiCallService = Mock()
    ApiProperties api = new ApiProperties("https://localhost:8443", new ApiProperties.Retry(3, 1000L))

    @Subject
    ApiBatchRunner runner = new ApiBatchRunner(apiCallService, api)

    def "run() calls GET and POST exactly once"() {
        when:
        runner.run()

        then:
        1 * apiCallService.callGet("https://localhost:8443/api/hello")
        1 * apiCallService.callPost("https://localhost:8443/api/echo", _ as String)
    }

    def "GET request URL is built correctly from baseUrl"() {
        when:
        runner.run()

        then:
        1 * apiCallService.callGet("https://localhost:8443/api/hello")
        1 * apiCallService.callPost(_, _)
    }

    def "POST request URL is built correctly from baseUrl"() {
        when:
        runner.run()

        then:
        1 * apiCallService.callGet(_)
        1 * apiCallService.callPost("https://localhost:8443/api/echo", _ as String)
    }

    def "correct paths are appended regardless of baseUrl"() {
        given:
        def customApi = new ApiProperties("https://example.com:9443", new ApiProperties.Retry(3, 1000L))
        def customRunner = new ApiBatchRunner(apiCallService, customApi)

        when:
        customRunner.run()

        then:
        1 * apiCallService.callGet("https://example.com:9443/api/hello")
        1 * apiCallService.callPost("https://example.com:9443/api/echo", _ as String)
    }
}
