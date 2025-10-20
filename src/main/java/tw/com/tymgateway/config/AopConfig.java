package tw.com.tymgateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AOP 配置類
 *
 * 啟用 AspectJ AOP 支援，讓 common 模組中的 AOP 類別能夠生效
 * 適用於 Spring Cloud Gateway (WebFlux) 環境
 */
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {
    // AOP 會自動掃描並應用 @Aspect 註解的類別，包括 common 模組中的類別
    // 在 WebFlux 環境中，需要確保 AOP 正確配置以支援異步處理
}
