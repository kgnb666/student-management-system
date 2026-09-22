#!/usr/bin/env bash
set -euo pipefail

# ==========================================================================
# 把本项目的数据库从其他项目（campus-ledger）里独立出来。
#
# 背景：最初部署时后端直接使用 campus-ledger 项目的 MySQL 容器与网络，
# 一旦该项目的容器/网络被停用或删除，本系统就会因为连不上数据库而整体不可用。
#
# 本脚本会：
#   1. 备份现有 student_score 数据到 BACKUP_DIR
#   2. 新建独立网络与 MySQL 容器（student-score-default / student-score-mysql）
#   3. 导入备份数据并创建应用账号
#   4. 更新 backend.env 并重建后端容器（挂到新网络上）
#   5. 可选：停掉旧的数据库容器（默认停掉，容器与数据卷保留以便回滚）
#
# 幂等：重复执行会复用已存在的网络/容器，不会覆盖已有数据。
#
# 可覆盖的环境变量见下方默认值。
# ==========================================================================

APP_DIR="${APP_DIR:-/www/wwwroot/student_management}"
OLD_DB_CONTAINER="${OLD_DB_CONTAINER:-campus-ledger-mysql}"
NEW_DB_CONTAINER="${NEW_DB_CONTAINER:-student-score-mysql}"
NEW_DB_NETWORK="${NEW_DB_NETWORK:-student-score_default}"
NEW_DB_VOLUME="${NEW_DB_VOLUME:-student-score_mysql_data}"
BACKEND_CONTAINER="${BACKEND_CONTAINER:-student-management-backend}"
BACKEND_IMAGE="${BACKEND_IMAGE:-campus-ledger-backend:latest}"
BACKEND_HOST_PORT="${BACKEND_HOST_PORT:-18081}"
BACKEND_MEMORY="${BACKEND_MEMORY:-448m}"
BACKEND_CPUS="${BACKEND_CPUS:-1.0}"
BACKEND_XMX="${BACKEND_XMX:-256m}"
DB_NAME="${DB_NAME:-student_score}"
DB_USER="${DB_USER:-student_score}"
JAR_PATH="${JAR_PATH:-$APP_DIR/backend/target/student-score-1.0.0.jar}"
BACKUP_DIR="${BACKUP_DIR:-/root/sms-db-backup-$(date +%Y%m%d-%H%M%S)}"
STOP_OLD_DB="${STOP_OLD_DB:-1}"

log() { echo "==> $*"; }

if ! docker inspect "$OLD_DB_CONTAINER" >/dev/null 2>&1; then
    echo "旧数据库容器不存在：$OLD_DB_CONTAINER（如果已经完成过拆分，可忽略本脚本）"
    exit 1
fi

if [[ ! -f "$JAR_PATH" ]]; then
    echo "后端 JAR 不存在：$JAR_PATH"
    exit 1
fi

# ---------------------------------------------------------------- 1. 备份
log "备份现有数据库到 $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"
docker start "$OLD_DB_CONTAINER" >/dev/null 2>&1 || true
docker exec "$OLD_DB_CONTAINER" sh -c \
    'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --databases '"$DB_NAME" \
    > "$BACKUP_DIR/$DB_NAME.sql"
if [[ ! -s "$BACKUP_DIR/$DB_NAME.sql" ]]; then
    echo "备份文件为空，停止操作以便排查"
    exit 1
fi
log "备份完成：$(du -h "$BACKUP_DIR/$DB_NAME.sql" | cut -f1)"

# ------------------------------------------------- 2. 独立网络与数据库容器
if ! docker network inspect "$NEW_DB_NETWORK" >/dev/null 2>&1; then
    log "创建独立网络 $NEW_DB_NETWORK"
    docker network create "$NEW_DB_NETWORK" >/dev/null
fi

if docker ps -a --format '{{.Names}}' | grep -qx "$NEW_DB_CONTAINER"; then
    log "容器 $NEW_DB_CONTAINER 已存在，直接复用"
    docker start "$NEW_DB_CONTAINER" >/dev/null 2>&1 || true
else
    NEW_ROOT_PW="$(openssl rand -hex 16)"
    echo "$NEW_ROOT_PW" > "$BACKUP_DIR/new-mysql-root-password.txt"
    chmod 600 "$BACKUP_DIR/new-mysql-root-password.txt"
    log "创建独立数据库容器 $NEW_DB_CONTAINER（内存 512m，关闭 performance_schema 以节省内存）"
    docker run -d \
        --name "$NEW_DB_CONTAINER" \
        --restart unless-stopped \
        --network "$NEW_DB_NETWORK" \
        --memory 512m \
        -e MYSQL_ROOT_PASSWORD="$NEW_ROOT_PW" \
        -v "$NEW_DB_VOLUME":/var/lib/mysql \
        mysql:8.0 \
        --character-set-server=utf8mb4 \
        --collation-server=utf8mb4_unicode_ci \
        --innodb-buffer-pool-size=128M \
        --performance-schema=OFF >/dev/null
fi

# 注意：不能用 mysqladmin ping 判断就绪——初始化期间它会因为权限被拒绝而报错，
# 但退出码仍然是 0，会把「还没初始化完」误判成「已就绪」。这里改用真实查询确认能登录。
log "等待数据库就绪（首次初始化通常需要 20-60 秒）"
DB_READY=0
for _ in $(seq 1 90); do
    if docker exec "$NEW_DB_CONTAINER" sh -c \
        'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 1"' >/dev/null 2>&1; then
        DB_READY=1
        break
    fi
    sleep 2
done
if [[ "$DB_READY" != "1" ]]; then
    echo "数据库未能在预期时间内就绪，请查看：docker logs --tail 50 $NEW_DB_CONTAINER"
    exit 1
fi
log "数据库已就绪"

# ------------------------------------------------------------ 3. 导入数据
DB_EXISTS="$(docker exec "$NEW_DB_CONTAINER" sh -lc \
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -B -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='"'"$DB_NAME"'"'"' \
    2>/dev/null || true)"

if [[ "$DB_EXISTS" != "1" ]]; then
    log "导入备份数据到 $NEW_DB_CONTAINER"
    docker exec -i "$NEW_DB_CONTAINER" sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < "$BACKUP_DIR/$DB_NAME.sql"
else
    log "新容器中已存在 $DB_NAME，跳过导入"
fi

APP_DB_PW="$(openssl rand -hex 16)"
echo "$APP_DB_PW" > "$BACKUP_DIR/new-app-db-password.txt"
chmod 600 "$BACKUP_DIR/new-app-db-password.txt"

log "创建应用账号 $DB_USER"
docker exec -i "$NEW_DB_CONTAINER" sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' <<SQL
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${APP_DB_PW}';
ALTER USER '${DB_USER}'@'%' IDENTIFIED BY '${APP_DB_PW}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'%';
FLUSH PRIVILEGES;
SQL

# --------------------------------------------------------- 4. 后端容器切换
JWT_SECRET_VALUE="$(grep '^JWT_SECRET=' "$APP_DIR/backend.env" | cut -d= -f2- || true)"
if [[ -z "$JWT_SECRET_VALUE" ]]; then
    JWT_SECRET_VALUE="$(openssl rand -hex 32)"
fi

log "更新 $APP_DIR/backend.env 指向独立数据库"
cat > "$APP_DIR/backend.env" <<EOF
SPRING_DATASOURCE_URL=jdbc:mysql://${NEW_DB_CONTAINER}:3306/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${APP_DB_PW}
JWT_SECRET=${JWT_SECRET_VALUE}
SERVER_PORT=8080
EOF
chmod 600 "$APP_DIR/backend.env"

log "重建后端容器（挂到 $NEW_DB_NETWORK）"
docker rm -f "$BACKEND_CONTAINER" >/dev/null 2>&1 || true
docker run -d \
    --name "$BACKEND_CONTAINER" \
    --restart unless-stopped \
    --network "$NEW_DB_NETWORK" \
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

log "等待后端就绪"
for _ in $(seq 1 60); do
    if curl -fsS -X POST "http://127.0.0.1:${BACKEND_HOST_PORT}/api/auth/login" \
        -H 'Content-Type: application/json' \
        -d '{"username":"admin","password":"admin123"}' >/dev/null 2>&1; then
        log "后端已恢复，登录接口正常"
        break
    fi
    sleep 2
done

if ! curl -fsS -X POST "http://127.0.0.1:${BACKEND_HOST_PORT}/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"admin123"}' >/dev/null 2>&1; then
    echo "后端未能正常提供登录服务，请查看：docker logs --tail 80 $BACKEND_CONTAINER"
    exit 1
fi

# --------------------------------------------------------- 5. 停掉旧的数据库
if [[ "$STOP_OLD_DB" == "1" ]]; then
    log "停掉旧数据库容器 $OLD_DB_CONTAINER（容器与数据卷保留，可随时回滚）"
    docker stop "$OLD_DB_CONTAINER" >/dev/null
fi

echo
echo "完成。数据库已独立：容器 $NEW_DB_CONTAINER，网络 $NEW_DB_NETWORK"
echo "备份与密码文件目录：$BACKUP_DIR"
echo "回滚方式：把 $APP_DIR/backend.env 改回旧地址，并用下面的命令重建后端容器即可："
echo "  docker rm -f $BACKEND_CONTAINER && docker run ... (参考 deploy-student-management.sh)"
