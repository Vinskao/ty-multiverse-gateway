#!/bin/bash

# 啟動 Gateway (gRPC 已在 application-local.yml 中配置)
cd /Users/vinskao/001-project/ty-multiverse/ty-multiverse-gateway

echo "🚀 Starting Gateway with gRPC client enabled (configured in application-local.yml)..."

./mvnw spring-boot:run -Dspring-boot.run.profiles=local

