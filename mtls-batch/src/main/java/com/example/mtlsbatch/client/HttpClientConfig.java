package com.example.mtlsbatch.client;

import com.example.mtlsbatch.config.SslProperties;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;

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

        // プライベートCA（社内CA等）の場合：提供されたCA証明書からトラストストアを作成して使用する
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(loadKeyStore(ssl.truststore(), ssl.truststorePassword()));

        // パブリックCA（Let's Encrypt、DigiCert等）の場合：以下2行に置き換える
        // TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        // tmf.init((KeyStore) null); // null = JVMのデフォルトトラストストア（cacerts）を使用

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return HttpClients.custom()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(
                        SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(sslContext)
                            .build()
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
