package br.com.tlf.shared.configuration;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.springframework.boot.data.redis.autoconfigure.ClientResourcesBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.lettuce.core.resource.NettyCustomizer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;

@Configuration
public class RedisClusterSslConfiguration {

    @Bean
    public ClientResourcesBuilderCustomizer azureRedisTlsPeerVerificationCustomizer() {
        return builder -> builder.nettyCustomizer(new NettyCustomizer() {

            @Override
            public void afterBootstrapInitialized(Bootstrap bootstrap) {
            }

            @Override
            public void afterChannelInitialized(Channel channel) {
                disableHostnameVerification(channel.pipeline().get(SslHandler.class));
            }
        });
    }

    private static void disableHostnameVerification(SslHandler sslHandler) {
        if (sslHandler == null) {
            return;
        }
        SSLEngine engine = sslHandler.engine();
        SSLParameters sslParameters = engine.getSSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm("");
        engine.setSSLParameters(sslParameters);
    }
}