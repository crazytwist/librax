#!/bin/bash
# create-project.sh - 基于 librax 底座创建新业务项目骨架
#
# 用法:
#   ./create-project.sh --name deep-principle --group com.megarobo --output /path/to/dir
#   ./create-project.sh --name deep-principle --group com.megarobo --librax-version 2025.11-SNAPSHOT --port 48090 --output /path/to/dir
#
# 参数:
#   --name            项目名（kebab-case，如 deep-principle）
#   --group           groupId（如 com.megarobo）
#   --librax-version  librax 底座版本，默认 2025.11-SNAPSHOT
#   --port            服务端口，默认 48080
#   --output          生成目录，默认当前目录

set -e

# ── 默认值 ────────────────────────────────────────────────
LIBRAX_VERSION="2025.11-SNAPSHOT"
PORT="48080"
OUTPUT_DIR="$(pwd)"

# ── 参数解析 ──────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --name)            PROJECT_NAME="$2";       shift 2 ;;
    --group)           GROUP_ID="$2";           shift 2 ;;
    --librax-version)  LIBRAX_VERSION="$2";     shift 2 ;;
    --port)            PORT="$2";               shift 2 ;;
    --output)          OUTPUT_DIR="$2";         shift 2 ;;
    *) echo "未知参数: $1"; exit 1 ;;
  esac
done

# ── 校验 ─────────────────────────────────────────────────
if [[ -z "$PROJECT_NAME" || -z "$GROUP_ID" ]]; then
  echo "错误：--name 和 --group 为必填参数"
  echo "示例：./create-project.sh --name deep-principle --group com.megarobo --output /Users/yinan/workspace/megarobo"
  exit 1
fi

# ── 派生变量 ──────────────────────────────────────────────
# deep-principle → DeepPrinciple
to_pascal() {
  local result=""
  local IFS='-'
  for word in $1; do
    result="${result}$(echo "${word:0:1}" | tr '[:lower:]' '[:upper:]')${word:1}"
  done
  echo "$result"
}
PROJECT_PASCAL=$(to_pascal "$PROJECT_NAME")
# com.megarobo → com/megarobo
GROUP_PATH=$(echo "$GROUP_ID" | tr '.' '/')
# com.megarobo + deepprinciple → com.megarobo.deepprinciple
PACKAGE_SUFFIX=$(echo "$PROJECT_NAME" | tr -d '-')
PACKAGE_NAME="${GROUP_ID}.${PACKAGE_SUFFIX}"
PACKAGE_PATH="${GROUP_PATH}/${PACKAGE_SUFFIX}"

PROJECT_DIR="${OUTPUT_DIR}/${PROJECT_NAME}"

echo ""
echo "┌─────────────────────────────────────────┐"
echo "│  创建 librax 业务项目                    │"
echo "├─────────────────────────────────────────┤"
echo "│  项目名:    ${PROJECT_NAME}"
echo "│  GroupId:   ${GROUP_ID}"
echo "│  包名:      ${PACKAGE_NAME}"
echo "│  主类:      ${PROJECT_PASCAL}Application"
echo "│  LibraX:    ${LIBRAX_VERSION}"
echo "│  端口:      ${PORT}"
echo "│  输出目录:  ${PROJECT_DIR}"
echo "└─────────────────────────────────────────┘"
echo ""

if [[ -d "$PROJECT_DIR" ]]; then
  echo "目录已存在: $PROJECT_DIR"
  read -p "是否覆盖？[y/N] " confirm
  [[ "$confirm" =~ ^[Yy]$ ]] || { echo "已取消"; exit 0; }
fi

# ── 创建目录结构 ──────────────────────────────────────────
mkdir -p "${PROJECT_DIR}/${PROJECT_NAME}-server/src/main/java/${PACKAGE_PATH}/server/controller"
mkdir -p "${PROJECT_DIR}/${PROJECT_NAME}-server/src/main/resources"
mkdir -p "${PROJECT_DIR}/${PROJECT_NAME}-module-biz/src/main/java/${PACKAGE_PATH}/module/biz"

echo ">>> 生成父 pom.xml ..."
cat > "${PROJECT_DIR}/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>${GROUP_ID}</groupId>
    <artifactId>${PROJECT_NAME}</artifactId>
    <version>\${revision}</version>
    <packaging>pom</packaging>

    <name>\${project.artifactId}</name>

    <modules>
        <module>${PROJECT_NAME}-server</module>
        <module>${PROJECT_NAME}-module-biz</module>
    </modules>

    <properties>
        <revision>1.0.0-SNAPSHOT</revision>
        <librax.version>${LIBRAX_VERSION}</librax.version>
        <java.version>17</java.version>
        <maven.compiler.source>\${java.version}</maven.compiler.source>
        <maven.compiler.target>\${java.version}</maven.compiler.target>
        <maven-compiler-plugin.version>3.14.0</maven-compiler-plugin.version>
        <flatten-maven-plugin.version>1.7.2</flatten-maven-plugin.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- 导入 librax 底座 BOM，统一管理所有依赖版本 -->
            <dependency>
                <groupId>com.librax.boot</groupId>
                <artifactId>librax-dependencies</artifactId>
                <version>\${librax.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>\${maven-compiler-plugin.version}</version>
                    <configuration>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-configuration-processor</artifactId>
                            </path>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </path>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok-mapstruct-binding</artifactId>
                                <version>0.2.0</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                            </path>
                        </annotationProcessorPaths>
                        <compilerArgs>
                            <arg>-parameters</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>flatten-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </pluginManagement>

        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>flatten-maven-plugin</artifactId>
                <version>\${flatten-maven-plugin.version}</version>
                <configuration>
                    <flattenMode>oss</flattenMode>
                    <updatePomFile>true</updatePomFile>
                </configuration>
                <executions>
                    <execution>
                        <id>flatten</id>
                        <phase>process-resources</phase>
                        <goals><goal>flatten</goal></goals>
                    </execution>
                    <execution>
                        <id>flatten.clean</id>
                        <phase>clean</phase>
                        <goals><goal>clean</goal></goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <repositories>
        <!-- 云效私有仓库：拉取 librax 底座 -->
        <repository>
            <id>repo-rvnwk</id>
            <url>https://packages.aliyun.com/68e7bfe3f3eec50950f5589b/maven/repo-rvnwk</url>
            <releases><enabled>true</enabled></releases>
            <snapshots><enabled>true</enabled><updatePolicy>always</updatePolicy></snapshots>
        </repository>
        <!-- 阿里云公共镜像 -->
        <repository>
            <id>aliyunmaven</id>
            <url>https://maven.aliyun.com/repository/public</url>
        </repository>
    </repositories>

</project>
EOF

echo ">>> 生成 ${PROJECT_NAME}-server/pom.xml ..."
cat > "${PROJECT_DIR}/${PROJECT_NAME}-server/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>${GROUP_ID}</groupId>
        <artifactId>${PROJECT_NAME}</artifactId>
        <version>\${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>${PROJECT_NAME}-server</artifactId>
    <packaging>jar</packaging>
    <name>\${project.artifactId}</name>

    <dependencies>
        <!-- ── librax 底座模块 ── -->
        <dependency>
            <groupId>com.librax.boot</groupId>
            <artifactId>librax-module-system</artifactId>
            <version>\${librax.version}</version>
        </dependency>
        <dependency>
            <groupId>com.librax.boot</groupId>
            <artifactId>librax-module-infra</artifactId>
            <version>\${librax.version}</version>
        </dependency>
        <dependency>
            <groupId>com.librax.boot</groupId>
            <artifactId>librax-module-resource</artifactId>
            <version>\${librax.version}</version>
        </dependency>
        <dependency>
            <groupId>com.librax.boot</groupId>
            <artifactId>librax-module-flow</artifactId>
            <version>\${librax.version}</version>
        </dependency>

        <!-- ── 本项目业务模块 ── -->
        <dependency>
            <groupId>${GROUP_ID}</groupId>
            <artifactId>${PROJECT_NAME}-module-biz</artifactId>
            <version>\${revision}</version>
        </dependency>

        <!-- ── 服务保障 ── -->
        <dependency>
            <groupId>com.librax.boot</groupId>
            <artifactId>librax-spring-boot-starter-protection</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <finalName>\${project.artifactId}</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals><goal>repackage</goal></goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
EOF

echo ">>> 生成启动类 ${PROJECT_PASCAL}Application.java ..."
cat > "${PROJECT_DIR}/${PROJECT_NAME}-server/src/main/java/${PACKAGE_PATH}/server/${PROJECT_PASCAL}Application.java" << EOF
package ${PACKAGE_NAME}.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SuppressWarnings("SpringComponentScan")
@SpringBootApplication(scanBasePackages = {"\${librax.info.base-package}.server", "\${librax.info.base-package}.module"})
public class ${PROJECT_PASCAL}Application {

    public static void main(String[] args) {
        SpringApplication.run(${PROJECT_PASCAL}Application.class, args);
    }

}
EOF

echo ">>> 生成 application.yaml ..."
cat > "${PROJECT_DIR}/${PROJECT_NAME}-server/src/main/resources/application.yaml" << EOF
spring:
  application:
    name: ${PROJECT_NAME}

  profiles:
    active: local

  main:
    allow-circular-references: true

  servlet:
    multipart:
      max-file-size: 16MB
      max-request-size: 32MB

  jackson:
    serialization:
      write-dates-as-timestamps: true
      write-date-timestamps-as-nanoseconds: false
      write-durations-as-timestamps: true
      fail-on-empty-beans: false

  cache:
    type: REDIS
    redis:
      time-to-live: 1h

server:
  servlet:
    encoding:
      enabled: true
      charset: UTF-8
      force: true

--- #################### 接口文档配置 ####################

springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui
  default-flat-param-object: true

knife4j:
  enable: true
  setting:
    language: zh_cn

--- #################### librax 框架配置 ####################

librax:
  info:
    base-package: ${PACKAGE_NAME}
  captcha:
    enable: false
  security:
    mock-enable: false
  access-log:
    enable: false
  demo: false
EOF

echo ">>> 生成 application-local.yaml ..."
cat > "${PROJECT_DIR}/${PROJECT_NAME}-server/src/main/resources/application-local.yaml" << EOF
server:
  port: ${PORT}

--- #################### 数据库配置 ####################

spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration
  datasource:
    druid:
      web-stat-filter:
        enabled: true
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 100
          merge-sql: true
        wall:
          config:
            multi-statement-allow: true
    dynamic:
      druid:
        initial-size: 1
        min-idle: 1
        max-active: 20
        max-wait: 60000
        validation-query: SELECT 1 FROM DUAL
        test-while-idle: true
        test-on-borrow: false
        test-on-return: false
      primary: master
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/${PROJECT_NAME}?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true
          username: root
          password: root

--- #################### Redis 配置 ####################

  data:
    redis:
      host: localhost
      port: 6379
      database: 0

--- #################### 日志配置 ####################

logging:
  file:
    name: \${user.home}/logs/${PROJECT_NAME}/${PROJECT_NAME}.log
  level:
    com.librax.lab.module.infra.dal.mysql: info
    com.librax.lab.module.system.dal.mysql: info
EOF

echo ">>> 生成 logback-spring.xml ..."
cat > "${PROJECT_DIR}/${PROJECT_NAME}-server/src/main/resources/logback-spring.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <property name="CONSOLE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [exec:%X{executionId}] [node:%X{nodeId}] %highlight(%-5level) %cyan(%logger{50}:%L) - %msg%n"/>
    <property name="FILE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [exec:%X{executionId}] [node:%X{nodeId}] %-5level %logger{50}:%L - %msg%n"/>

    <springProperty name="LOG_FILE" source="logging.file.name" defaultValue="\${user.home}/logs/${PROJECT_NAME}/${PROJECT_NAME}.log"/>
    <springProperty name="LOG_PATH" source="logging.file.path" defaultValue="\${user.home}/logs/${PROJECT_NAME}"/>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>\${CONSOLE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>\${FILE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <file>\${LOG_FILE}</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>\${LOG_PATH}/${PROJECT_NAME}.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxHistory>7</maxHistory>
            <maxFileSize>50MB</maxFileSize>
            <totalSizeCap>2GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <discardingThreshold>0</discardingThreshold>
        <queueSize>512</queueSize>
        <appender-ref ref="FILE"/>
    </appender>

    <logger name="com.baomidou.mybatisplus" level="WARN" additivity="false"/>
    <logger name="com.zaxxer.hikari"        level="WARN" additivity="false"/>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="ASYNC"/>
    </root>

</configuration>
EOF

echo ">>> 生成 ${PROJECT_NAME}-module-biz/pom.xml ..."
cat > "${PROJECT_DIR}/${PROJECT_NAME}-module-biz/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>${GROUP_ID}</groupId>
        <artifactId>${PROJECT_NAME}</artifactId>
        <version>\${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>${PROJECT_NAME}-module-biz</artifactId>
    <packaging>jar</packaging>
    <name>\${project.artifactId}</name>

    <dependencies>
        <!-- 依赖 librax 底座的 API 模块（轻量，无具体实现） -->
        <dependency>
            <groupId>com.librax.boot</groupId>
            <artifactId>librax-module-flow-api</artifactId>
            <version>\${librax.version}</version>
        </dependency>
        <dependency>
            <groupId>com.librax.boot</groupId>
            <artifactId>librax-module-device-api</artifactId>
            <version>\${librax.version}</version>
        </dependency>
        <!-- Web / 通用能力 -->
        <dependency>
            <groupId>com.librax.boot</groupId>
            <artifactId>librax-spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.librax.boot</groupId>
            <artifactId>librax-spring-boot-starter-mybatis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.librax.boot</groupId>
            <artifactId>librax-spring-boot-starter-security</artifactId>
        </dependency>
    </dependencies>

</project>
EOF

echo ">>> 生成 .gitignore ..."
cat > "${PROJECT_DIR}/.gitignore" << EOF
target/
*.class
*.log
*.jar
.idea/
*.iml
.flattened-pom.xml
logs/
EOF

echo ""
echo "✅ 项目生成完成！"
echo ""
echo "项目结构:"
find "${PROJECT_DIR}" -not -path '*/\.*' | sed 's|[^/]*/|  |g'
echo ""
echo "下一步："
echo "  1. 用 IDEA 打开 ${PROJECT_DIR}"
echo "  2. 修改 application-local.yaml 中的数据库密码"
echo "  3. 确保 ~/.m2/settings.xml 已配置云效认证（参考 librax/settings-template.xml）"
