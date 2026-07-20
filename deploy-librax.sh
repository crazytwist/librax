#!/bin/bash
# ============================================================
# 发布 librax 底座库到云效制品仓库
# 用法：
#   ./deploy-librax.sh          # 发布当前 SNAPSHOT 版本
#   ./deploy-librax.sh 2025.11  # 发布指定 Release 版本
# ============================================================
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
step()  { echo -e "\n${BLUE}━━━ $* ━━━${NC}"; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VERSION=${1:-""}

# 自定义 Maven 的本地仓库（librax 项目使用）
MAVEN_BIN="/Users/yinan/workspace/apache-maven-3.9.12/bin/mvn"
CUSTOM_REPO="/Users/yinan/workspace/apache-maven-3.9.12/repo"
# IDE 默认本地仓库（IntelliJ 给派生项目如 deep-principle 使用）
DEFAULT_REPO="$HOME/.m2/repository"

# ── Step 1: 设置版本号 ────────────────────────────────────────
if [ -n "$VERSION" ]; then
    step "Step 1 / 4  设置 Release 版本：$VERSION"
    $MAVEN_BIN versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false -q
    DEPLOY_VERSION="$VERSION"
else
    step "Step 1 / 4  使用当前 SNAPSHOT 版本"
    DEPLOY_VERSION=$($MAVEN_BIN help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
    info "当前版本：$DEPLOY_VERSION"
fi

# ── Step 2: 构建并发布到云效 ──────────────────────────────────
step "Step 2 / 4  构建并发布到云效（跳过测试）"

# librax-framework 是多模块父工程，列出后 Maven 会递归发布其所有子模块
# 包含：librax-spring-boot-starter-protection/web/security/redis/mybatis/mq 等
MODULES="librax-dependencies,\
librax-framework,\
librax-module-system,\
librax-module-infra,\
librax-module-resource,\
librax-module-flow,\
librax-module-flow-api,\
librax-module-device,\
librax-module-device-api,\
librax-module-lab,\
librax-module-task"

$MAVEN_BIN deploy \
    --projects "$MODULES" \
    --also-make \
    -DskipTests \
    -T 4 \
    -f "$SCRIPT_DIR/pom.xml"

# ── Step 3: 同步到 ~/.m2/repository ──────────────────────────
step "Step 3 / 4  同步到 ~/.m2/repository（供 IntelliJ 派生项目使用）"

# 问题根因：
#   librax 项目使用自定义 Maven，本地仓库 = $CUSTOM_REPO
#   deep-principle 等派生项目在 IntelliJ 中使用默认 Maven，本地仓库 = ~/.m2/repository
#   两套仓库路径不同，导致 IDE 报 "Unresolved dependency"
# 解决：将 librax 相关 artifact 从自定义仓库同步到 ~/.m2/repository
SRC="$CUSTOM_REPO/com/librax/boot"
DST="$DEFAULT_REPO/com/librax/boot"

if [ -d "$SRC" ]; then
    mkdir -p "$DST"
    rsync -a --exclude="*.lastUpdated" "$SRC/" "$DST/"
    info "已同步 librax artifacts → $DST"
else
    warn "自定义仓库路径不存在：$SRC，跳过同步"
fi

# ── Step 4: 清除 ~/.m2 中的失败缓存 ──────────────────────────
step "Step 4 / 4  清除 .lastUpdated 失败缓存"

# 清除两个仓库中历次解析失败留下的缓存，避免 IDE 使用旧的失败记录
for CACHE_DIR in "$CUSTOM_REPO/com/librax/boot" "$DEFAULT_REPO/com/librax/boot"; do
    if [ -d "$CACHE_DIR" ]; then
        cd "$CACHE_DIR"
        COUNT=$(find . -name "*.lastUpdated" | wc -l | tr -d ' ')
        if [ "$COUNT" -gt 0 ]; then
            find . -name "*.lastUpdated" -delete
            info "已清除 $COUNT 个失败缓存  ← $CACHE_DIR"
        fi
        cd "$SCRIPT_DIR"
    fi
done

# ── 恢复 SNAPSHOT 版本（Release 流程专用）────────────────────
if [ -n "$VERSION" ]; then
    info "恢复 SNAPSHOT 版本..."
    $MAVEN_BIN versions:set -DnewVersion="2025.11-SNAPSHOT" -DgenerateBackupPoms=false -q
fi

# ── 完成 ──────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  librax 发布完成！${NC}"
echo -e "${GREEN}======================================${NC}"
echo -e "  版本         $DEPLOY_VERSION"
echo -e "  云效仓库     已推送"
echo -e "  自定义仓库   $CUSTOM_REPO"
echo -e "  默认仓库     $DEFAULT_REPO  ← 已同步，IDE 可直接使用"
echo -e ""
echo -e "  完成后在 IntelliJ 中对 deep-principle 执行："
echo -e "  ${BLUE}Maven 工具栏 → Reload All Maven Projects${NC}"
echo -e "${GREEN}======================================${NC}"
