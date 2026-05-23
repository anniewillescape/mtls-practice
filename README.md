# mTLS Practice

Spring Boot を使った mTLS（相互TLS認証）の動作確認用リポジトリである。

- **mtls-api** : クライアント証明書を要求するREST APIサーバ
- **mtls-batch** : クライアント証明書を提示してAPIを呼び出すバッチ

## アーキテクチャ

```
mtls-batch                          mtls-api
(CommandLineRunner)                 (Spring Boot Web)
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

## 前提条件

- Java 17
- OpenSSL
- keytool（JDK付属）

## ディレクトリ構成

```
.
├── certs/
│   ├── generate-demo-certs.sh   # 証明書生成スクリプト
│   └── .gitkeep
├── mtls-api/                    # APIサーバ (port 8443)
└── mtls-batch/                  # バッチクライアント
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

## 注意事項

`certs/` 内の証明書・秘密鍵ファイルは `.gitignore` で除外されている。
生成されたファイルをコミットしないこと。
