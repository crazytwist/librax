#!/bin/bash
# ============================================================
# librax-frontend 一键部署脚本
# 流程：本地 pnpm 构建 → 打包 dist → SCP 上传 → nginx 容器服务
# 使用：
#   bash deploy-frontend.sh
# ============================================================
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
step()  { echo -e "\n${BLUE}━━━ $* ━━━${NC}"; }

# ── 配置 ──────────────────────────────────────────────────────
SERVER_HOST="172.16.255.92"
SERVER_USER="ai"
SERVER_PASS="aifjeport"

FRONTEND_PORT=8081                             # nginx 监听端口
BACKEND_URL="http://librax-server:48080"       # 后端容器（同 docker_default 网络）
CONTAINER_NAME="librax-frontend"
REMOTE_DIR="/opt/librax-frontend"

FRONTEND_DIR="/Users/yinan/workspace/yinan/ruoyi/yudao-ui-admin-vue3"
DIST_DIR="$FRONTEND_DIR/dist"
DIST_TAR="/tmp/librax-frontend-dist.tar.gz"

info "部署目标：${SERVER_HOST}:${FRONTEND_PORT}"

# ── Step 1: 本地构建 ──────────────────────────────────────────
step "Step 1 / 4  本地 pnpm 构建"
cd "$FRONTEND_DIR"

# 直接调用 vite 并指定更大堆内存（8192MB），避免 pnpm build:local 中 4096MB 限制导致 OOM
# VITE_BASE_URL 置空：axios 使用相对路径，浏览器请求发到 nginx，由 nginx 反向代理到后端
# 若写 http://localhost:48080 则浏览器直接连本机 48080，远程访问必然失败
info "开始构建（首次约 2~3 分钟）..."
VITE_BASE_URL='' node --max_old_space_size=8192 ./node_modules/vite/bin/vite.js build

DIST_SIZE=$(du -sh "$DIST_DIR" | cut -f1)
info "构建完成：$DIST_DIR ($DIST_SIZE)"

# ── Step 2: 打包 dist ─────────────────────────────────────────
step "Step 2 / 4  打包 dist 目录"
tar -czf "$DIST_TAR" -C "$DIST_DIR" .
info "打包完成：$DIST_TAR ($(du -sh "$DIST_TAR" | cut -f1))"

# ── Step 3: 上传到服务器 ──────────────────────────────────────
step "Step 3 / 4  上传到服务器"

/usr/bin/python3 << PYEOF
import paramiko, os, sys

host, user, password = "$SERVER_HOST", "$SERVER_USER", "$SERVER_PASS"
remote_dir  = "$REMOTE_DIR"
dist_tar    = "$DIST_TAR"
backend_url = "$BACKEND_URL"
fe_port     = "$FRONTEND_PORT"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(host, username=user, password=password, timeout=15)

def run(cmd, timeout=30):
    _, out, err = client.exec_command(cmd, timeout=timeout)
    return out.read().decode() + err.read().decode()

# 创建远端目录（/opt 需要 sudo，用 run() 同步等待完成）
import time
result = run(f"echo '{password}' | sudo -S bash -c 'mkdir -p {remote_dir} && chown {user}:{user} {remote_dir} && chmod 755 {remote_dir} && echo DONE'")
if 'DONE' not in result:
    print(f"目录创建失败：{result}")
    exit(1)
print(f"远端目录已就绪：{remote_dir}")

# 上传 dist.tar.gz（显示进度）
sftp = client.open_sftp()
tar_size = os.path.getsize(dist_tar)

def progress(sent, total):
    pct = int(sent / total * 100)
    bar  = "█" * (pct // 5) + "░" * (20 - pct // 5)
    print(f"\r  [{bar}] {pct}% ({sent//1024}KB/{total//1024}KB)", end="", flush=True)

print(f"上传 dist.tar.gz ({tar_size // 1024} KB)...")
sftp.put(dist_tar, f"{remote_dir}/dist.tar.gz", callback=progress)
print(f"\n上传完成")

# 解压
print("解压中...")
out = run(f"cd {remote_dir} && rm -rf dist && mkdir dist && tar -xzf dist.tar.gz -C dist && rm dist.tar.gz")
print("解压完成")

# ── 写入 nginx.conf ──────────────────────────────────────────
nginx_conf = f"""
server {{
    listen {fe_port};
    root   /app/dist;
    index  index.html;
    charset utf-8;
    gzip on;
    gzip_types text/plain text/css application/javascript application/json image/svg+xml;

    # SPA 路由：404 回退到 index.html
    location / {{
        try_files \$uri \$uri/ /index.html;
    }}

    # 反向代理 API（解决前端打包时 localhost:48080 问题）
    location /admin-api/ {{
        proxy_pass         {backend_url}/admin-api/;
        proxy_http_version 1.1;
        proxy_set_header   Host              \$http_host;
        proxy_set_header   X-Real-IP         \$remote_addr;
        proxy_set_header   X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto \$scheme;
        proxy_read_timeout 120s;
    }}

    # 静态资源长缓存
    location ~* \\.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {{
        expires 30d;
        add_header Cache-Control "public, no-transform";
    }}
}}
""".strip()

print("写入 nginx.conf...")
run(f"cat > {remote_dir}/nginx.conf << 'NGINXEOF'\n{nginx_conf}\nNGINXEOF")

sftp.close()
client.close()
print("文件上传完成")
PYEOF

# ── Step 4: 启动 nginx 容器 ───────────────────────────────────
step "Step 4 / 4  启动 nginx 容器"

/usr/bin/python3 << PYEOF
import paramiko, time

host, user, password = "$SERVER_HOST", "$SERVER_USER", "$SERVER_PASS"
container  = "$CONTAINER_NAME"
remote_dir = "$REMOTE_DIR"
fe_port    = "$FRONTEND_PORT"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(host, username=user, password=password, timeout=15)

def run(cmd, timeout=30):
    _, out, err = client.exec_command(cmd, timeout=timeout)
    return out.read().decode() + err.read().decode()

# 拉取 nginx:alpine 镜像（服务器无需预装 nginx，由 Docker 管理）
print("拉取 nginx:alpine 镜像（已有则跳过，首次约 10s）...")
transport = client.get_transport()
chan = transport.open_session()
chan.get_pty()
chan.exec_command("docker pull nginx:alpine 2>&1")
while True:
    if chan.recv_ready():
        line = chan.recv(4096).decode(errors='replace')
        for l in line.splitlines():
            if l.strip(): print(f"  {l}")
    if chan.exit_status_ready():
        break
    time.sleep(0.3)
rc = chan.recv_exit_status()
if rc != 0:
    print(f"镜像拉取失败，退出码 {rc}")
    exit(1)
print("nginx:alpine 镜像就绪")

print("停止旧容器（如有）...")
run(f"docker stop {container} 2>/dev/null || true")
run(f"docker rm   {container} 2>/dev/null || true")

docker_run = (
    f"docker run -d "
    f"--name {container} "
    f"--restart always "
    f"--network docker_default "                      # 与 librax-server 同网络
    f"-p {fe_port}:{fe_port} "
    f"-v {remote_dir}/dist:/app/dist:ro "
    f"-v {remote_dir}/nginx.conf:/etc/nginx/conf.d/default.conf:ro "
    f"-e TZ=Asia/Shanghai "
    f"nginx:alpine"
)

print("启动 nginx 容器...")
out = run(docker_run)
print(f"  容器ID: {out.strip()[:12]}")

print("等待端口就绪（最多 30s）...")
for i in range(6):
    time.sleep(5)
    result = run(f"nc -z localhost {fe_port} && echo OK || echo WAIT")
    if "OK" in result:
        print(f"  [{(i+1)*5}s] 端口 {fe_port} 已就绪 ✓")
        break
    print(f"  [{(i+1)*5}s] 等待中...")
else:
    print("\n端口未就绪，查看日志：")
    print(run(f"docker logs {container} --tail 20"))

print("\n容器状态：")
print(run(f"docker ps --filter name={container} --format 'table {{{{.Names}}}}\t{{{{.Status}}}}\t{{{{.Ports}}}}'"))

client.close()
PYEOF

SERVER_IP="$SERVER_HOST"
echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  librax-frontend 部署完成！${NC}"
echo -e "${GREEN}======================================${NC}"
echo -e "  前端地址    http://${SERVER_IP}:${FRONTEND_PORT}"
echo -e "  API 代理    /admin-api  →  ${BACKEND_URL}/admin-api"
echo -e "  查看日志    ssh ${SERVER_USER}@${SERVER_IP} 'docker logs -f ${CONTAINER_NAME}'"
echo -e "  更新前端    重新执行 bash deploy-frontend.sh"
echo -e "${GREEN}======================================${NC}"
