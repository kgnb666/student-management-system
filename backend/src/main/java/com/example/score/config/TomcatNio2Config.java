package com.example.score.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 可选的 Tomcat NIO2 连接器开关。
 *
 * <p>默认使用 Tomcat 标准 NIO。个别 Windows + JDK 组合下，默认 NIO 的 loopback
 * 选择器会出现 `Unable to establish loopback connection`，此时把连接器切换到等价的
 * NIO2 即可绕过，启用方式：`TOMCAT_PROTOCOL=nio2` 或 `--tomcat.protocol=nio2`。</p>
 */
@Configuration
public class TomcatNio2Config {

    @Bean
    @ConditionalOnProperty(name = "tomcat.protocol", havingValue = "nio2")
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatNio2Customizer() {
        return factory -> factory.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");
    }
}
