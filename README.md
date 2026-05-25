# mTLS Practice

Spring Boot を使った mTLS（相互TLS認証）の動作確認用リポジトリである。

- **mtls-api** : クライアント証明書を要求するREST APIサーバ
- **mtls-batch** : クライアント証明書を提示してAPIを呼び出すバッチ
- **mtls-api-client** : mTLS APIと通常APIの両方を呼び出すREST APIクライアント

## アーキテクチャ

**mtls-batch → mtls-api (mTLS)**

```
mtls-batch                          mtls-api
(CommandLineRunner)                 (Spring Boot Web / port 8443)
        |                                 |
        |  TLS ClientHello                |
        |-------------------------------->|
        |  ServerHello + サーバ証明書      |
        |<--------------------------------|
        |  クライアント証明書              |
        |-------------------------------->|
        |  mTLS 確立 (双方向認証)          |
        |================================>|
        |  GET /api/hello                 |
        |<================================|
        |  { message, clientDN, ... }     |
        |                                 |
        |  POST /api/echo                 |
        |================================>|
        |  { echo: {...}, clientDN, ... } |
```

**mtls-api-client → mtls-api (mTLS / Apache HttpClient 5)**

```
mtls-api-client                     mtls-api
(Spring Boot Web / port 8080)       (Spring Boot Web / port 8443)
        |                                 |
        |  TLS ClientHello                |
        |-------------------------------->|
        |  ServerHello + サーバ証明書      |
        |<--------------------------------|
        |  クライアント証明書              |
        |-------------------------------->|
        |  mTLS 確立 (双方向認証)          |
        |================================>|
        |  GET /api/hello                 |
        |<================================|
        |  { message, clientDN, ... }     |
        |                                 |
        |  POST /api/echo                 |
        |================================>|
        |  { echo: {...}, clientDN, ... } |
```

**mtls-api-client → 外部API (通常HTTPS / OkHttp)**

```
mtls-api-client                     外部API
(Spring Boot Web / port 8080)       (例: httpbin.org)
        |                                 |
        |  HTTPS接続                      |
        |-------------------------------->|
        |  GET  /get                      |
        |<--------------------------------|
        |                                 |
        |  POST /post                     |
        |-------------------------------->|
        |<--------------------------------|
```

## 前提条件

- Java 17
- OpenSSL
- keytool（JDK付属）

## ディレクトリ構成

```
.
├── certs/                   # 証明書・キーストア
│   └── generate-demo-certs.sh
├── mtls-api/                # APIサーバ (port 8443, mTLS必須)
├── mtls-batch/              # バッチクライアント (Apache HttpClient 5)
└── mtls-api-client/         # REST APIクライアント (port 8080)
                             #   mTLS呼び出し → Apache HttpClient 5
                             #   通常API呼び出し → OkHttp
```

## セットアップ

### 1. 証明書の生成

```bash
cd certs
bash generate-demo-certs.sh
```

以下のファイルが生成されます。

| ファイル | 内容 |
|---|---|
| `ca.pem` / `ca.crt` | プライベートCA（秘密鍵 / 証明書） |
| `server.pem` / `server.crt` | APIサーバ用（秘密鍵 / 証明書） |
| `client.pem` / `client.crt` | バッチクライアント用（秘密鍵 / 証明書） |

### 2. PKCS12形式に変換

```bash
cd certs

# APIサーバ用キーストア
openssl pkcs12 -export \
  -in server.crt -inkey server.pem \
  -out server-keystore.p12 -name server \
  -passout pass:changeit

# バッチ用クライアントキーストア
openssl pkcs12 -export \
  -in client.crt -inkey client.pem \
  -out client-keystore.p12 -name client \
  -passout pass:password

# APIサーバ用トラストストア（CAのみ）
keytool -import -alias ca -file ca.crt \
  -keystore truststore.p12 -storetype PKCS12 \
  -storepass changeit -noprompt

# バッチ用トラストストア（CAのみ）
keytool -import -alias ca -file ca.crt \
  -keystore truststore-batch.p12 -storetype PKCS12 \
  -storepass password -noprompt
```

### 3. APIサーバの起動

```bash
cd mtls-api
./gradlew bootRun
```

`https://localhost:8443` で起動する。

### 4. バッチの実行

別ターミナルで実行する。

```bash
cd mtls-batch
./gradlew bootRun
```

成功すると以下のようなレスポンスがログに出力される。

```
[GET] Response body: {"message":"Hello from mTLS API!","clientDN":"CN=batch-client,O=Demo,C=JP","timestamp":"..."}
[POST] Response body: {"echo":{"message":"Hello from batch!","batchId":"batch-001"},"clientDN":"CN=batch-client,O=Demo,C=JP","timestamp":"..."}
```

### 5. APIクライアントの起動

別ターミナルで実行する。

```bash
cd mtls-api-client
./gradlew bootRun
```

`http://localhost:8080` で起動する。起動後に以下のエンドポイントへリクエストを送ることで動作確認できる。

```bash
# mTLS経由で mtls-api の GET /api/hello を呼び出す
curl http://localhost:8080/client/mtls/hello

# mTLS経由で mtls-api の POST /api/echo を呼び出す
curl -X POST http://localhost:8080/client/mtls/echo \
  -H "Content-Type: application/json" \
  -d '{"message": "hello", "from": "client"}'

# OkHttp経由で外部API（httpbin.org）を呼び出す
curl http://localhost:8080/client/public/get

# OkHttp経由で外部API（httpbin.org）に POST する
curl -X POST http://localhost:8080/client/public/post \
  -H "Content-Type: application/json" \
  -d '{"key": "value"}'
```

## 動作のポイント

### サーバ側（mtls-api）

`application.yml` で `client-auth: need` を設定することで、クライアント証明書の提示を必須にしている。
証明書なしでアクセスすると TLS ハンドシェイクが失敗する。

```yaml
server:
  ssl:
    client-auth: need
    trust-store: file:../certs/truststore.p12  # 信頼するCAを指定
```

### クライアント側（mtls-batch）

Apache HttpClient 5 で `KeyManagerFactory`（クライアント証明書）と `TrustManagerFactory`（CA検証）を明示的に設定した `SSLContext` を使っている。

トラストストアの選択は `application.yml` の `batch.ssl.truststore` の有無で自動的に切り替わる。

- **設定あり** → 指定したトラストストアを使用（プライベートCA向け）
- **設定なし** → JVM デフォルト（`cacerts`）を使用（パブリックCA向け）

### クライアント側（mtls-api-client）

呼び出し先に応じてHTTPクライアントを使い分けている。

| 用途 | HTTPクライアント | 設定クラス |
|---|---|---|
| mTLS API呼び出し | Apache HttpClient 5 | `MtlsHttpClientConfig` |
| 通常API呼び出し | OkHttp | `OkHttpClientConfig` |

Apache HttpClient 5 は `SSLConnectionSocketFactory` にクライアント証明書を含む `SSLContext` を渡すことで mTLS を実現している。
OkHttp はデフォルト設定のままで通常の HTTPS / HTTP 呼び出しに使用する。

```yaml
client:
  api:
    mtls-base-url: https://localhost:8443   # Apache HttpClient 5 で呼び出す
    public-base-url: https://httpbin.org    # OkHttp で呼び出す
  ssl:
    client-keystore: ../certs/client-keystore.p12
    truststore: ../certs/truststore-batch.p12
```

## 注意事項

`certs/` 内の証明書・秘密鍵ファイルは `.gitignore` で除外されている。
生成されたファイルをコミットしないこと。
