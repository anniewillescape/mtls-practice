package com.example.mtlsbatch.client;

import com.example.mtlsbatch.config.SslProperties;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

    private final SslProperties ssl;

    public HttpClientConfig(SslProperties ssl) {
        this.ssl = ssl;
    }

    @Bean(destroyMethod = "close")
    public CloseableHttpClient httpClient() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(
            loadKeyStore(ssl.clientKeystore(), ssl.clientKeystorePassword()),
            ssl.clientKeystorePassword().toCharArray()
        );

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        if (ssl.truststore() != null) {
            tmf.init(loadKeyStore(ssl.truststore(), ssl.truststorePassword()));
        } else {
            tmf.init((KeyStore) null); // JVMデフォルト(cacerts)を使用
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return HttpClients.custom()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setTlsSocketStrategy(
                        ClientTlsStrategyBuilder.create()
                            .setSslContext(sslContext)
                            .buildClassic()
                    )
                    .build()
            )
            .build();
    }

    private KeyStore loadKeyStore(String path, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(Paths.get(path))) {
            ks.load(is, password.toCharArray());
        }
        return ks;
    }
}
