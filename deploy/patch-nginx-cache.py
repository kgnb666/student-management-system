#!/usr/bin/env python3
"""为站点 nginx 配置补上静态资源缓存策略（幂等，可重复执行）。

只插入 location 片段，不改动文件里其他内容（例如别的项目追加的反代配置）。
用法：python3 patch-nginx-cache.py /www/server/panel/vhost/nginx/student_management.conf
"""

import shutil
import sys
import time
from pathlib import Path

MARKER = "add_header Cache-Control \"public, max-age=31536000, immutable\" always;"

CACHE_BLOCK = """    # 带内容哈希的静态资源：一年强缓存，切换页面时不再重复下载
    location /assets/ {
        add_header Cache-Control "public, max-age=31536000, immutable" always;
        access_log off;
    }

    # 入口 HTML 不缓存，保证发版后立刻能拿到新资源
    location = /index.html {
        add_header Cache-Control "no-cache" always;
    }

"""


def main() -> int:
    if len(sys.argv) != 2:
        print("用法: patch-nginx-cache.py <nginx 配置文件>")
        return 2

    path = Path(sys.argv[1])
    if not path.is_file():
        print(f"配置文件不存在：{path}")
        return 1

    content = path.read_text(encoding="utf-8")
    if MARKER in content:
        print("已存在缓存配置，无需修改")
        return 0

    anchor = "    location / {"
    if anchor not in content:
        print("未找到插入位置（缺少 location / 块），请手工处理")
        return 1

    backup = path.with_name(f"{path.name}.bak-{time.strftime('%Y%m%d-%H%M%S')}")
    shutil.copy2(path, backup)

    patched = content.replace(anchor, CACHE_BLOCK + anchor, 1)
    path.write_text(patched, encoding="utf-8")
    print(f"已写入缓存配置，原文件备份为 {backup.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
