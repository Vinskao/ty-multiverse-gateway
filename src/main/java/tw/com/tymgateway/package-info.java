/**
 * TY Multiverse API Gateway
 * 
 * <p>這是 TY Multiverse 系統的 API Gateway 套件，提供統一的入口管理。</p>
 * 
 * <h2>主要功能</h2>
 * <ul>
 *   <li><strong>路由管理</strong>：將前端請求路由到對應的後端服務</li>
 *   <li><strong>限流保護</strong>：基於 Redis 的分散式限流</li>
 *   <li><strong>熔斷降級</strong>：使用 Resilience4j 提供服務熔斷</li>
 *   <li><strong>跨域處理</strong>：統一的 CORS 配置</li>
 *   <li><strong>日誌追蹤</strong>：記錄所有請求和響應</li>
 * </ul>
 * 
 * <h2>架構設計</h2>
 * <p>Gateway 採用 Spring Cloud Gateway，基於 WebFlux 的非阻塞式反應式架構，
 * 提供高性能的 API 路由功能。</p>
 * 
 * <h2>套件結構</h2>
 * <ul>
 *   <li>{@link tw.com.tymgateway.config} - 配置類</li>
 *   <li>{@link tw.com.tymgateway.controller} - 控制器類</li>
 *   <li>{@link tw.com.tymgateway.filter} - 過濾器類</li>
 * </ul>
 * 
 * @author TY Team
 * @version 1.0
 * @since 2024
 */
package tw.com.tymgateway;

