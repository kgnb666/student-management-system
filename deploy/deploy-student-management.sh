#!/usr/bin/env bash
set -euo pipefail

# ==========================================================================
# 学生成绩管理系统部署脚本
#
# 前置条件：
#   1. 服务器已安装 Docker、nginx，并已存在一个可用的 MySQL 容器；
#   2. 本机（或服务器）已构建出 backend/target/student-score-1.0.0.jar；
#      JAR 不存在时可先在 backend 目录执行：mvn -DskipTests package
#   3. frontend/dist/index.html 已构建：在 frontend 目录执行 npm run build
#
# 所有环境相关取值都可通过环境变量覆盖，脚本内不再写死服务器地址。
# ==========================================================================

APP_DIR="${APP_DIR:-/www/wwwroot/student_management}"

# 站点对外域名或 IP；默认取本机第一个 IP
SERVER_NAME="${SERVER_NAME:-$(hostname -I 2>/dev/null | awk '{print $1}')}"
SITE_PORT="${SITE_PORT:-80}"
EXTRA_SITE_PORT="${EXTRA_SITE_PORT:-889}"

# nginx 配置目录与文件名（宝塔面板默认路径可通过 NGINX_CONF_DIR 覆盖）
NGINX_CONF_DIR="${NGINX_CONF_DIR:-/www/server/panel/vhost/nginx}"
NGINX_CONF_NAME="${NGINX_CONF_NAME:-student_management.conf}"

# 数据库容器与网络
DB_CONTAINER="${DB_CONTAINER:-campus-ledger-mysql}"
DB_NETWORK="${DB_NETWORK:-campus-ledger_default}"
DB_NAME="${DB_NAME:-student_score}"
DB_USER="${DB_USER:-student_score}"

# 后端容器
BACKEND_CONTAINER="${BACKEND_CONTAINER:-student-management-backend}"
BACKEND_IMAGE="${BACKEND_IMAGE:-campus-ledger-backend:latest}"
BACKEND_HOST_PORT="${BACKEND_HOST_PORT:-18081}"
BACKEND_MEMORY="${BACKEND_MEMORY:-448m}"
BACKEND_CPUS="${BACKEND_CPUS:-1.0}"
BACKEND_XMX="${BACKEND_XMX:-256m}"

JAR_PATH="$APP_DIR/backend/target/student-score-1.0.0.jar"
FRONTEND_DIR="$APP_DIR/frontend/dist"

DB_PASSWORD="$(openssl rand -hex 16)"
JWT_SECRET="$(openssl rand -hex 32)"

if [[ ! -f "$JAR_PATH" ]]; then
    echo "后端 JAR 不存在：$JAR_PATH"
    echo "请先在 backend 目录执行：mvn -DskipTests package"
    exit 1
fi

if [[ ! -f "$FRONTEND_DIR/index.html" ]]; then
    echo "前端构建文件不存在：$FRONTEND_DIR/index.html"
    echo "请先在 frontend 目录执行：npm run build"
    exit 1
fi

echo "==> 初始化数据库（容器：$DB_CONTAINER）"
DB_EXISTS="$(docker exec "$DB_CONTAINER" sh -lc \
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -B -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='"'"'student_score'"'"';"' \
    2>/dev/null || true)"

if [[ "$DB_EXISTS" != "1" ]]; then
    docker exec -i "$DB_CONTAINER" sh -lc \
        'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4' \
        < "$APP_DIR/sql/student_score.sql"
fi

echo "==> 创建应用数据库账号"
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

echo "==> 写入后端环境变量文件"
cat > "$APP_DIR/backend.env" <<EOF
SPRING_DATASOURCE_URL=jdbc:mysql://${DB_CONTAINER}:3306/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
SERVER_PORT=8080
EOF
chmod 600 "$APP_DIR/backend.env"

echo "==> 重启后端容器"
docker rm -f "$BACKEND_CONTAINER" >/dev/null 2>&1 || true
docker run -d \
    --name "$BACKEND_CONTAINER" \
    --restart unless-stopped \
    --network "$DB_NETWORK" \
    --memory "$BACKEND_MEMORY" \
    --cpus "$BACKEND_CPUS" \
    -p "127.0.0.1:${BACKEND_HOST_PORT}:8080" \
    --env-file "$APP_DIR/backend.env" \
    -v "$JAR_PATH:/app/student-management.jar:ro" \
    --entrypoint java \
    "$BACKEND_IMAGE" \
    -Xms128m \
    -Xmx"$BACKEND_XMX" \
    -XX:MaxMetaspaceSize=128m \
    -XX:+UseSerialGC \
    -jar /app/student-management.jar >/dev/null

echo "==> 写入 nginx 配置：$NGINX_CONF_DIR/$NGINX_CONF_NAME"
mkdir -p "$NGINX_CONF_DIR"
cat > "$NGINX_CONF_DIR/$NGINX_CONF_NAME" <<EOF
server {
    listen ${SITE_PORT};
    listen ${EXTRA_SITE_PORT};
    server_name ${SERVER_NAME};
    root ${FRONTEND_DIR};
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:${BACKEND_HOST_PORT}/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }
}
EOF

nginx -t
systemctl reload nginx

echo "==> 等待服务就绪"
for _ in $(seq 1 60); do
    if curl -fsS -X POST "http://127.0.0.1:${BACKEND_HOST_PORT}/api/auth/login" \
        -H 'Content-Type: application/json' \
        -d '{"username":"admin","password":"admin123"}' >/dev/null; then
        echo "部署完成：http://${SERVER_NAME}:${SITE_PORT}/"
        exit 0
    fi
    sleep 1
done

echo "服务未能在规定时间内启动，请检查："
docker logs --tail 80 "$BACKEND_CONTAINER"
exit 1
