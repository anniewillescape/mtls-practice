#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Generating PEM files for mTLS demo ==="

# CA
echo "--- Generating CA ---"
openssl genrsa -out ca.pem 4096
openssl req -new -x509 -days 3650 -key ca.pem -out ca.crt \
    -subj "/C=JP/O=Demo/CN=Demo CA"

# Server cert with SAN (hostname verification に必要)
echo "--- Generating server certificate ---"
openssl genrsa -out server.pem 2048
openssl req -new -key server.pem -out server.csr \
    -subj "/C=JP/O=Demo/CN=localhost"

cat > server-ext.cnf << 'EOF'
[v3_req]
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
IP.1 = 127.0.0.1
EOF

openssl x509 -req -days 365 -in server.csr -CA ca.crt -CAkey ca.pem \
    -CAcreateserial -out server.crt -extfile server-ext.cnf -extensions v3_req

# Client cert
echo "--- Generating client certificate ---"
openssl genrsa -out client.pem 2048
openssl req -new -key client.pem -out client.csr \
    -subj "/C=JP/O=Demo/CN=batch-client"
openssl x509 -req -days 365 -in client.csr -CA ca.crt -CAkey ca.pem \
    -CAcreateserial -out client.crt

# Cleanup temp files
rm -f *.csr *.srl server-ext.cnf

echo ""
echo "=== Done! Generated PEM files ==="
ls -la *.crt *.pem
echo ""
echo "=== Next: manually convert to .p12 ==="
echo "See README or run the following commands:"
echo ""
echo "  # APIサーバ用キーストア"
echo "  openssl pkcs12 -export \\"
echo "    -in server.crt -inkey server.pem \\"
echo "    -out server-keystore.p12 -name server \\"
echo "    -passout pass:changeit"
echo ""
echo "  # バッチ用クライアントキーストア"
echo "  openssl pkcs12 -export \\"
echo "    -in client.crt -inkey client.pem \\"
echo "    -out client-keystore.p12 -name client \\"
echo "    -passout pass:changeit"
echo ""
echo "  # トラストストア（CA証明書のみ）"
echo "  keytool -import -alias ca -file ca.crt \\"
echo "    -keystore truststore.p12 -storetype PKCS12 \\"
echo "    -storepass changeit -noprompt"
