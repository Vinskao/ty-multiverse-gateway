package tw.com.tymgateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tw.com.ty.common.response.GatewayResponse;

import java.util.Map;

/**
 * Gateway API 文档控制器
 * 手动定义 Gateway 路由的 API 规范
 *
 * @author TY Backend Team
 * @version 1.0
 * @since 2025
 */
@RestController
@RequestMapping("/api-docs")
@Tag(name = "Gateway API", description = "TY Multiverse Gateway 路由文档")
public class ApiDocsController {

    @Value("${PUBLIC_TYMB_URL:http://localhost:8080}")
    private String backendServiceUrl;

    @GetMapping("/routes")
    @Operation(summary = "获取所有路由信息",
               description = "返回 Gateway 配置的所有路由信息和转发规则")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取路由信息",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(type = "object")))
    })
    public ResponseEntity<GatewayResponse<Map<String, Object>>> getRoutes() {
        Map<String, Object> routes = Map.of(
            "description", "TY Multiverse Gateway Routes",
            "version", "1.0.0",
            "routes", Map.of(
                "people", Map.of(
                    "path", "/tymg/people/**",
                    "target", backendServiceUrl + "/tymb/people/**",
                    "description", "People 管理模块"
                ),
                "weapons", Map.of(
                    "path", "/tymg/weapons/**",
                    "target", backendServiceUrl + "/tymb/weapons/**",
                    "description", "武器管理模块"
                ),
                "gallery", Map.of(
                    "path", "/tymg/gallery/**",
                    "target", backendServiceUrl + "/tymb/gallery/**",
                    "description", "图片管理模块"
                ),
                "async", Map.of(
                    "path", "/tymg/api/**",
                    "target", backendServiceUrl + "/tymb/api/**",
                    "description", "异步请求状态管理"
                ),
                "health", Map.of(
                    "path", "/tymg/health/**",
                    "target", backendServiceUrl + "/tymb/health/**",
                    "description", "健康检查和监控"
                )
            )
        );
        return ResponseEntity.ok(GatewayResponse.success("路由信息获取成功", routes));
    }

    @GetMapping("/health")
    @Operation(summary = "Gateway 健康检查",
               description = "检查 Gateway 服务状态")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Gateway 运行正常"),
        @ApiResponse(responseCode = "503", description = "Gateway 服务不可用")
    })
    public ResponseEntity<GatewayResponse<Map<String, String>>> health() {
        Map<String, String> healthData = Map.of(
            "status", "UP",
            "service", "TY Multiverse Gateway",
            "version", "1.0.0"
        );
        return ResponseEntity.ok(GatewayResponse.success("Gateway 运行正常", healthData));
    }

    @GetMapping(value = "/ui", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "API 文档页面",
               description = "返回简单的 HTML API 文档页面")
    public ResponseEntity<GatewayResponse<String>> getApiDocsPage() {
        String html = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>TY Multiverse Gateway API 文档</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
                    .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }
                    .endpoint { background: #f8f9fa; margin: 10px 0; padding: 15px; border-left: 4px solid #3498db; border-radius: 5px; }
                    .method { font-weight: bold; color: #27ae60; }
                    .path { font-family: 'Courier New', monospace; color: #e74c3c; }
                    .description { margin-top: 5px; color: #7f8c8d; }
                    .test-btn { background: #3498db; color: white; padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; margin-top: 10px; }
                    .test-btn:hover { background: #2980b9; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🚀 TY Multiverse Gateway API 文档</h1>
                    <p>欢迎使用 TY Multiverse 统一 API Gateway！所有请求都通过 <code>/tymg</code> 前缀。</p>

                    <h2>📋 可用端点</h2>

                    <div class="endpoint">
                        <div><span class="method">POST</span> <span class="path">/tymg/people/get-all</span></div>
                        <div class="description">获取所有人物信息（异步模式）</div>
                        <button class="test-btn" onclick="testEndpoint('POST', '/tymg/people/get-all')">测试</button>
                    </div>

                    <div class="endpoint">
                        <div><span class="method">POST</span> <span class="path">/tymg/people/insert</span></div>
                        <div class="description">插入新人物</div>
                        <button class="test-btn" onclick="testEndpoint('POST', '/tymg/people/insert', {'name':'Test','nameOriginal':'测试'})">测试</button>
                    </div>

                    <div class="endpoint">
                        <div><span class="method">GET</span> <span class="path">/tymg/people/names</span></div>
                        <div class="description">获取所有人物名称</div>
                        <button class="test-btn" onclick="testEndpoint('GET', '/tymg/people/names')">测试</button>
                    </div>

                    <div class="endpoint">
                        <div><span class="method">GET</span> <span class="path">/tymg/weapons</span></div>
                        <div class="description">获取所有武器信息</div>
                        <button class="test-btn" onclick="testEndpoint('GET', '/tymg/weapons')">测试</button>
                    </div>

                    <div class="endpoint">
                        <div><span class="method">POST</span> <span class="path">/tymg/gallery/getAll</span></div>
                        <div class="description">获取所有图片</div>
                        <button class="test-btn" onclick="testEndpoint('POST', '/tymg/gallery/getAll')">测试</button>
                    </div>

                    <div class="endpoint">
                        <div><span class="method">GET</span> <span class="path">/tymg/health</span></div>
                        <div class="description">健康检查</div>
                        <button class="test-btn" onclick="testEndpoint('GET', '/tymg/health')">测试</button>
                    </div>

                    <h2>📊 测试结果</h2>
                    <div id="results" style="background: #f8f9fa; padding: 15px; border-radius: 5px; min-height: 50px;"></div>
                </div>

                <script>
                    async function testEndpoint(method, path, body = null) {
                        const resultsDiv = document.getElementById('results');
                        resultsDiv.innerHTML = '<p>🔄 正在测试...</p>';

                        try {
                            const options = {
                                method: method,
                                headers: {
                                    'Content-Type': 'application/json',
                                }
                            };

                            if (body && method !== 'GET') {
                                options.body = JSON.stringify(body);
                            }

                            const response = await fetch('http://localhost:8082' + path, options);
                            const status = response.status;
                            let result = `✅ ${method} ${path} - Status: ${status}`;

                            if (response.ok) {
                                try {
                                    const data = await response.json();
                                    result += `<br>📦 响应数据: ${JSON.stringify(data, null, 2)}`;
                                } catch (e) {
                                    const text = await response.text();
                                    result += `<br>📄 响应内容: ${text.substring(0, 200)}...`;
                                }
                            } else {
                                const errorText = await response.text();
                                result += `<br>❌ 错误: ${errorText}`;
                            }

                            resultsDiv.innerHTML = `<div style="color: ${status >= 200 && status < 300 ? 'green' : 'red'}">${result}</div>`;
                        } catch (error) {
                            resultsDiv.innerHTML = `<div style="color: red">❌ 网络错误: ${error.message}</div>`;
                        }
                    }
                </script>
            </body>
            </html>
            """;
        return ResponseEntity.ok(GatewayResponse.success("API 文档页面获取成功", html));
    }
}
