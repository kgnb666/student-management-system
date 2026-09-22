#!/usr/bin/env bash
set -euo pipefail

# ==========================================================================
# 发布前端构建产物（安全的目录替换）。
#
# 用法：publish-frontend.sh <新构建目录> [部署目录]
#   例：publish-frontend.sh /root/dist-new /www/wwwroot/student_management/frontend/dist
#
# 做三件事，避免「换目录后老用户白屏」：
#   1. 先把当前部署目录整份备份为 dist.bak-<时间戳>
#   2. 用新构建替换部署目录
#   3. 把历史备份里的 assets **合并回**新目录（文件名带内容哈希，不会冲突），
#      这样浏览器即使还缓存着旧入口页，也能找到它引用的资源
#
# 可覆盖的环境变量：WEB_USER（默认 www）
# ==========================================================================

NEW_DIST="${1:?用法: publish-frontend.sh <新构建目录> [部署目录]}"
WEB_USER="${WEB_USER:-www}"

if [[ ! -f "$NEW_DIST/index.html" ]]; then
    echo "新构建目录里没有 index.html：$NEW_DIST"
    exit 1
fi

TARGET="${2:-$(cd "$(dirname "$NEW_DIST")" && pwd)/dist}"
PARENT="$(dirname "$TARGET")"
BASE="$(basename "$TARGET")"
TS="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$PARENT"

# 护栏：新构建目录与部署目录不能是同一个，否则替换时会先删掉源目录
NEW_REAL="$(cd "$NEW_DIST" && pwd -P)"
TARGET_REAL="$(cd "$TARGET" 2>/dev/null && pwd -P || echo "$PARENT/$BASE")"
if [[ "$NEW_REAL" == "$TARGET_REAL" ]]; then
    echo "新构建目录与部署目录相同（$NEW_REAL），拒绝执行以免误删源文件"
    exit 1
fi

if [[ -d "$TARGET" ]]; then
    BACKUP="$PARENT/$BASE.bak-$TS"
    echo "==> 备份当前版本到 $BACKUP"
    cp -a "$TARGET" "$BACKUP"
else
    BACKUP=""
fi

echo "==> 用 $NEW_DIST 替换 $TARGET"
rm -rf "$TARGET"
mkdir -p "$TARGET"
cp -a "$NEW_DIST/." "$TARGET/"

echo "==> 合并历史版本的 assets（兼容缓存了旧入口页的浏览器）"
MERGED=0
for old in "$PARENT/$BASE".bak-*/assets; do
    [[ -d "$old" ]] || continue
    before="$(find "$TARGET/assets" -type f 2>/dev/null | wc -l)"
    cp -rn "$old/." "$TARGET/assets/" 2>/dev/null || true
    after="$(find "$TARGET/assets" -type f 2>/dev/null | wc -l)"
    MERGED=$((MERGED + after - before))
done
echo "    额外并入 $MERGED 个历史资源文件"

if id "$WEB_USER" >/dev/null 2>&1; then
    chown -R "$WEB_USER:$WEB_USER" "$TARGET"
fi
find "$TARGET" -type d -exec chmod 755 {} \;
find "$TARGET" -type f -exec chmod 644 {} \;

echo
echo "发布完成：$TARGET"
echo "  文件数 $(find "$TARGET" -type f | wc -l)，入口引用：$(grep -o 'assets/[^\"]*' "$TARGET/index.html" | tr '\n' ' ')"
echo "  旧版本备份：${BACKUP:-（首次发布，无备份）}"
echo "  提示：入口 HTML 已配置 no-cache，用户普通刷新即可拿到新版本。"
