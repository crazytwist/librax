#!/bin/bash
# ============================================================
# librax-server 一键部署脚本
# 流程：本地 Maven 打包 → SCP jar → 服务器构建镜像 → 启动容器
# 使用：
#   bash deploy-to-server.sh       # 部署到测试服务器（dev）
#   bash deploy-to-server.sh prod  # 部署到客户生产服务器（prod）
# ============================================================
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
step()  { echo -e "\n${BLUE}━━━ $* ━━━${NC}"; }

# ── Profile ───────────────────────────────────────────────────
PROFILE="${1:-dev}"

# ═══════════════════════════════════════════════════════════════
# 测试服务器配置（dev）
# ═══════════════════════════════════════════════════════════════
DEV_SERVER_HOST="192.168.42.243"
DEV_SERVER_USER="yinan"
DEV_SERVER_PASS="yinan0302."
# dev 环境 DB/Redis 连接 deploy-ubuntu.sh 创建的容器，无需额外配置
# 已在 application-dev.yaml 中指定 mysql-local / redis-local

# ═══════════════════════════════════════════════════════════════
# 客户生产服务器配置（prod）
# 部署前请先在客户服务器运行 deploy-ubuntu.sh 初始化基础设施
# ═══════════════════════════════════════════════════════════════
PROD_SERVER_HOST="172.16.255.92"           # ← 填写客户服务器 IP
PROD_SERVER_USER="ai"               # ← 填写客户服务器账号
PROD_SERVER_PASS="aifjeport"           # ← 填写客户服务器密码

PROD_DB_HOST="mysql-local"            # ← 默认连接同服务器的 mysql-local 容器
PROD_DB_PORT="3306"
PROD_DB_NAME="librax-sz"             # ← 数据库名（按需修改）
PROD_DB_USER="root"                   # ← 数据库账号（建议改为非 root）
PROD_DB_PASS="root"                   # ← 数据库密码（务必修改！）

PROD_REDIS_HOST="redis-local"         # ← 默认连接同服务器的 redis-local 容器
PROD_REDIS_PORT="6379"
PROD_REDIS_PASS=""                    # ← Redis 密码（无密码留空）

# ═══════════════════════════════════════════════════════════════
# 根据 profile 选择目标服务器和启动参数
# ═══════════════════════════════════════════════════════════════
if [[ "$PROFILE" == "prod" ]]; then
    SERVER_HOST="$PROD_SERVER_HOST"
    SERVER_USER="$PROD_SERVER_USER"
    SERVER_PASS="$PROD_SERVER_PASS"
    JAVA_MEM="-Xms1g -Xmx1g"
    # prod 环境通过 -e 环境变量注入，由 application-prod.yaml 读取 ${DB_HOST} 等占位符
    PROFILE_ENV="-e DB_HOST=${PROD_DB_HOST} -e DB_PORT=${PROD_DB_PORT} -e DB_NAME=${PROD_DB_NAME} -e DB_USER=${PROD_DB_USER} -e DB_PASS=${PROD_DB_PASS} -e REDIS_HOST=${PROD_REDIS_HOST} -e REDIS_PORT=${PROD_REDIS_PORT}"
    [[ -n "$PROD_REDIS_PASS" ]] && PROFILE_ENV="${PROFILE_ENV} -e REDIS_PASS=${PROD_REDIS_PASS}"
else
    SERVER_HOST="$DEV_SERVER_HOST"
    SERVER_USER="$DEV_SERVER_USER"
    SERVER_PASS="$DEV_SERVER_PASS"
    JAVA_MEM="-Xms512m -Xmx512m"
    # dev 环境 DB/Redis 已在 application-dev.yaml 写死容器名，无需额外注入
    PROFILE_ENV=""
fi

# ── 固定参数 ──────────────────────────────────────────────────
IMAGE_NAME="librax-server"
CONTAINER_NAME="librax-server"
REMOTE_DIR="/opt/librax"
APP_PORT=48080

# 本地项目根目录（脚本所在目录）
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_SOURCE="$PROJECT_DIR/librax-server/target/librax-server.jar"
DOCKERFILE="$PROJECT_DIR/librax-server/Dockerfile"

info "部署目标：${PROFILE} → ${SERVER_HOST}"

# ── Step 1: 本地 Maven 打包 ───────────────────────────────────
step "Step 1 / 4  本地 Maven 打包"
cd "$PROJECT_DIR"
/Users/yinan/workspace/apache-maven-3.9.12/bin/mvn clean package -DskipTests -q
info "打包完成：$JAR_SOURCE ($(du -sh "$JAR_SOURCE" | cut -f1))"

# ── Step 2: 上传到服务器 ──────────────────────────────────────
step "Step 2 / 4  上传 jar 到服务器"

/usr/bin/python3 << PYEOF
import paramiko, os, sys

host, user, password = "$SERVER_HOST", "$SERVER_USER", "$SERVER_PASS"
remote_dir = "$REMOTE_DIR"
jar_src = "$JAR_SOURCE"
dockerfile_src = "$DOCKERFILE"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(host, username=user, password=password, timeout=15)

# 创建远端目录并授权（单次 sudo bash -c 确保 chown 也在 sudo 下执行，并等待完成）
_, out, err = client.exec_command(f"echo '{password}' | sudo -S bash -c 'mkdir -p {remote_dir} && chown {user}:{user} {remote_dir}'")
out.channel.recv_exit_status()
import time; time.sleep(0.5)

sftp = client.open_sftp()

# 上传 Dockerfile
print(f"上传 Dockerfile...")
sftp.put(dockerfile_src, f"{remote_dir}/Dockerfile")

# 上传 jar（显示进度）
jar_size = os.path.getsize(jar_src)
def progress(sent, total):
    pct = int(sent / total * 100)
    bar = "█" * (pct // 5) + "░" * (20 - pct // 5)
    print(f"\r  [{bar}] {pct}% ({sent//1024//1024}MB/{total//1024//1024}MB)", end="", flush=True)

print(f"上传 librax-server.jar ({jar_size//1024//1024} MB)...")
sftp.put(jar_src, f"{remote_dir}/librax-server.jar", callback=progress)
print(f"\n上传完成")

sftp.close()
client.close()
PYEOF

# ── Step 3: 服务器上构建 Docker 镜像 ─────────────────────────
step "Step 3 / 4  服务器构建 Docker 镜像"

/usr/bin/python3 << PYEOF
import paramiko, time

host, user, password = "$SERVER_HOST", "$SERVER_USER", "$SERVER_PASS"
remote_dir = "$REMOTE_DIR"
image = "$IMAGE_NAME"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(host, username=user, password=password, timeout=15)

_, out, err = client.exec_command(f"mkdir -p {remote_dir}/target && cp {remote_dir}/librax-server.jar {remote_dir}/target/librax-server.jar")
out.read(); err.read()

print("构建 Docker 镜像（首次需拉取基础镜像，约 1-3 分钟）...")
transport = client.get_transport()
chan = transport.open_session()
chan.get_pty()
chan.exec_command(f"cd {remote_dir} && docker build -t {image}:latest . 2>&1")
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
    print(f"镜像构建失败，退出码 {rc}")
    exit(1)
print("镜像构建成功")
client.close()
PYEOF

# ── Step 4: 启动容器 ─────────────────────────────────────────
step "Step 4 / 4  启动容器"

/usr/bin/python3 << PYEOF
import paramiko, time

host, user, password = "$SERVER_HOST", "$SERVER_USER", "$SERVER_PASS"
container   = "$CONTAINER_NAME"
image       = "$IMAGE_NAME"
profile     = "$PROFILE"
app_port    = "$APP_PORT"
java_mem    = "$JAVA_MEM"
profile_env = "$PROFILE_ENV"   # dev 为空；prod 含 DB_HOST/DB_PASS/REDIS_HOST 等

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(host, username=user, password=password, timeout=15)

def run(cmd, timeout=30):
    _, out, err = client.exec_command(cmd, timeout=timeout)
    return out.read().decode() + err.read().decode()

print("停止旧容器（如有）...")
run(f"docker stop {container} 2>/dev/null || true")
run(f"docker rm   {container} 2>/dev/null || true")

docker_run = (
    f"docker run -d "
    f"--name {container} "
    f"--restart always "
    f"--network docker_default "                    # 与 mysql-local redis-local 同网络
    f"-p {app_port}:{app_port} "
    f"-v /opt/logs:/librax-server/logs "            # 日志挂载到 /opt/logs，供 Promtail 采集
    f"-e TZ=Asia/Shanghai "
    f"-e SPRING_PROFILES_ACTIVE={profile} "
    f"-e JAVA_OPTS='{java_mem} -Djava.security.egd=file:/dev/./urandom' "
    f"{profile_env} "                               # prod 额外注入 DB/Redis 环境变量
    f"{image}:latest"
)

print("启动容器...")
out = run(docker_run)
print(f"  容器ID: {out.strip()[:12]}")

print("等待端口就绪（最多 90s）...")
for i in range(18):
    time.sleep(5)
    result = run(f"nc -z localhost {app_port} && echo OK || echo WAIT")
    if "OK" in result:
        print(f"  [{(i+1)*5}s] 端口 {app_port} 已就绪 ✓")
        break
    print(f"  [{(i+1)*5}s] 等待中...")
else:
    print("\n端口未就绪，查看日志：")
    print(run(f"docker logs {container} --tail 30"))

print("\n容器状态：")
print(run(f"docker ps --filter name={container} --format 'table {{{{.Names}}}}\t{{{{.Status}}}}\t{{{{.Ports}}}}'"))

client.close()
PYEOF

SERVER_IP="$SERVER_HOST"
echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  librax-server 部署完成！${NC}"
echo -e "${GREEN}======================================${NC}"
echo -e "  Profile     ${PROFILE}"
echo -e "  应用地址    http://${SERVER_IP}:${APP_PORT}"
echo -e "  健康检查    http://${SERVER_IP}:${APP_PORT}/actuator/health"
echo -e "  日志目录    /opt/logs  (Promtail 自动采集 → Loki → Grafana)"
echo -e "  查看日志    ssh ${SERVER_USER}@${SERVER_IP} 'docker logs -f ${CONTAINER_NAME}'"
echo -e "${GREEN}======================================${NC}"
