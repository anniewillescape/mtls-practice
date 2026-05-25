# mtls-api-client

mTLS API と通常 API の両方を呼び出す REST API クライアント。

| 呼び出し先 | HTTPクライアント | ポート |
|---|---|---|
| mtls-api (mTLS) | Apache HttpClient 5 | 8443 |
| 外部 API (通常 HTTPS) | OkHttp | - |

## 前提条件

- `mtls-api` が port 8443 で起動済みであること
- `certs/` に以下のファイルが生成済みであること（`certs/generate-demo-certs.sh` で生成）
  - `client-keystore.p12`
  - `truststore-batch.p12`

## 起動

```bash
./gradlew bootRun
```

`http://localhost:8080` で起動する。

## エンドポイント

### mTLS 経由で mtls-api を呼び出す

```bash
# GET /api/hello
curl http://localhost:8080/client/mtls/hello

# POST /api/echo
curl -X POST http://localhost:8080/client/mtls/echo \
  -H "Content-Type: application/json" \
  -d '{"message": "hello", "from": "client"}'
```

### OkHttp 経由で外部 API を呼び出す

デフォルトの呼び出し先は `httpbin.org`。

```bash
# GET
curl http://localhost:8080/client/public/get

# POST
curl -X POST http://localhost:8080/client/public/post \
  -H "Content-Type: application/json" \
  -d '{"key": "value"}'
```

`path` クエリパラメータで呼び出しパスを変更できる（`public-base-url` からの相対パス）。

```bash
curl "http://localhost:8080/client/public/get?path=/headers"
```

## 設定（application.yml）

mTLS 用と通常 API 用の設定は別プレフィックスで管理されており、各サービスが必要な設定のみを読み込む。

```yaml
client:
  mtls-api:
    base-url: https://localhost:8443   # MtlsApiService が読み込む
  public-api:
    base-url: https://httpbin.org      # PublicApiService が読み込む
  ssl:
    client-keystore: ../certs/client-keystore.p12
    client-keystore-password: password
    truststore: ../certs/truststore-batch.p12
    truststore-password: password
```
