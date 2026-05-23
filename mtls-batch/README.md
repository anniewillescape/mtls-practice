# mTLS認証のAPIとの通信

```text
.crt + .pem（受け取ったファイル）
↓ openssl
.p12（PKCS#12形式）
↓
Javaコードで読み込み
```
.p12から.jksに変換してJava Keystoreに入れることも可能だけど、 汎用性を考えると変換は.p12までに留めるのが良さそう。

Java9移行はデフォルトのKey Store TypeがJKSからPKCS12に変更されている。

ref: [JEP 229: Create PKCS12 Keystores by Default](https://openjdk.org/jeps/229)



## Step 1: 受け取った.crt, .pemをもとにバッチ用クライアントキーストアをPKCS#12（.p12）で生成する

```bash
openssl pkcs12 -export \
 -in client.crt \               # 受け取った証明書
 -inkey client.pem \            # 受け取った秘密鍵
 -out client-keystore.p12 \     # 出力ファイル名 (application.ymlのclient-keystoreに指定するfile)
 -name client-cert \            # エイリアス名（任意）
 -passout pass:changeit         # パスワード (application.ymlのclient-keystore-passwordに指定する)
 #-passout file:{filename} のように、passwordはfileから読み込むことも可能
```

## Step 2: ドメインの認証がプライベートCAの場合、trustkeystore.p12を生成する

APIの提供者からCA証明書 (ca.crt) を受け取り、
```bash
keytool -import -alias ca -file ca.crt \
    -keystore truststore.p12 \
    -storetype PKCS12 \
    -storepass changeit 
    -noprompt
```