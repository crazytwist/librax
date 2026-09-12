# LibraX — Maven 发布到 Nexus
#
# 用法:
#   make deploy            按 pom.xml 的 <revision> 发布（当前为 SNAPSHOT，可重复覆盖）
#   make deploy REV=1.0.0  覆盖 revision 发正式版到 maven-releases（同版本不可重复上传）
#   make deploy PL=librax-framework  只发指定模块及其依赖（-pl/-am）
#
# 默认不发布 librax-server（Spring Boot fat jar，Nexus 会 413）
# 凭据: config/maven-settings.xml（server id 须与 distributionManagement 一致）

ROOT := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
MVN  := mvn
MAVEN_SETTINGS := config/maven-settings.xml

REVISION := $(if $(REV),$(REV),$(shell sed -n 's/.*<revision>\([^<]*\)<\/revision>.*/\1/p' pom.xml | head -1))
REV_FLAG := $(if $(REV),-Drevision=$(REV),)
# 未指定 PL 时排除可执行包，避免 fat jar 触发 413
PL_FLAG  := $(if $(PL),-pl $(PL) -am,-pl '!librax-server')

.PHONY: help deploy

help:
	@echo "LibraX Nexus deploy"
	@echo ""
	@echo "  make deploy            publish libraries (skips librax-server fat jar)"
	@echo "  make deploy REV=1.0.0  publish release (overrides <revision>)"
	@echo "  make deploy PL=mod     publish one module and its reactor deps"
	@echo ""
	@echo "Settings: $(MAVEN_SETTINGS)"
	@echo "Release repo: https://nexus.labillion.cn/repository/maven-releases"
	@echo "Snapshot repo: https://nexus.labillion.cn/repository/maven-snapshots"

deploy:
	@echo "[deploy] mvn deploy -> Nexus (revision: $(REVISION)$(if $(PL), modules: $(PL), skip: librax-server))"
	@test -f "$(ROOT)$(MAVEN_SETTINGS)" || (echo "Missing $(MAVEN_SETTINGS)" && exit 1)
	@cd "$(ROOT)" && $(MVN) -s $(MAVEN_SETTINGS) deploy $(PL_FLAG) -DskipTests $(REV_FLAG)
