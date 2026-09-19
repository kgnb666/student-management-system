#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/www/wwwroot/student_management"
JAR_PATH="$APP_DIR/backend/target/student-score-1.0.0.jar"
FRONTEND_DIR="$APP_DIR/frontend/dist"
DB_CONTAINER="campus-ledger-mysql"
DB_NAME="student_score"
DB_USER="student_score"
DB_PASSWORD="$(openssl rand -hex 16)"
JWT_SECRET="$(openssl rand -hex 32)"

if [[ ! -f "$JAR_PATH" ]]; then
    echo "后端 JAR 不存在：$JAR_PATH"
    exit 1
fi

if [[ ! -f "$FRONTEND_DIR/index.html" ]]; then
    echo "前端构建文件不存在：$FRONTEND_DIR/index.html"
    exit 1
fi

DB_EXISTS="$(docker exec "$DB_CONTAINER" sh -lc \
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -B -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='"'"'student_score'"'"';"' \
    2>/dev/null || true)"

if [[ "$DB_EXISTS" != "1" ]]; then
    docker exec -i "$DB_CONTAINER" sh -lc \
        'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4' \
        < "$APP_DIR/sql/student_score.sql"
fi

USER_SQL="$(mktemp)"
cat > "$USER_SQL" <<EOF
CREATE USER IF NOT EXISTS '${DB_USER}'@'172.%' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER '${DB_USER}'@'172.%' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'172.%';
FLUSH PRIVILEGES;
EOF
docker exec -i "$DB_CONTAINER" sh -lc \
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < "$USER_SQL"
rm -f "$USER_SQL"

cat > "$APP_DIR/backend.env" <<EOF
SPRING_DATASOURCE_URL=jdbc:mysql://campus-ledger-mysql:3306/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
SERVER_PORT=8080
EOF
chmod 600 "$APP_DIR/backend.env"

docker rm -f student-management-backend >/dev/null 2>&1 || true
docker run -d \
    --name student-management-backend \
    --restart unless-stopped \
    --network campus-ledger_default \
    --memory 320m \
    --cpus 0.50 \
    -p 127.0.0.1:18081:8080 \
    --env-file "$APP_DIR/backend.env" \
    -v "$JAR_PATH:/app/student-management.jar:ro" \
    --entrypoint java \
    campus-ledger-backend:latest \
    -Xms96m \
    -Xmx192m \
    -XX:MaxMetaspaceSize=96m \
    -XX:+UseSerialGC \
    -jar /app/student-management.jar >/dev/null

cat > /www/server/panel/vhost/nginx/student_management.conf <<'EOF'
server {
    listen 80;
    listen 889;
    server_name 8.138.161.154;
    root /www/wwwroot/student_management/frontend/dist;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:18081/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF

ufw allow 889/tcp >/dev/null 2>&1 || true
nginx -t
systemctl reload nginx

for _ in $(seq 1 60); do
    if curl -fsS -H 'Host: 8.138.161.154' http://127.0.0.1/ >/dev/null; then
        echo "部署完成：http://8.138.161.154/"
        exit 0
    fi
    sleep 1
done

echo "服务未能在规定时间内启动，请检查："
docker logs --tail 80 student-management-backend
exit 1
