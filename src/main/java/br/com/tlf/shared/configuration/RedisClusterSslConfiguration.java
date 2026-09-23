package br.com.tlf.shared.configuration;

import java.net.InetAddress;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.ClientResourcesBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.MappingSocketAddressResolver;

@Configuration
@ConditionalOnProperty(name = "spring.data.redis.cluster.nodes")
public class RedisClusterSslConfiguration {

    private final String redisHost;

    public RedisClusterSslConfiguration(@Value("${REDIS_HOST:}") String redisHost) {
        this.redisHost = redisHost;
    }

    @Bean
    public ClientResourcesBuilderCustomizer azureRedisSocketAddressResolver() {
        return builder -> builder.socketAddressResolver(MappingSocketAddressResolver.create(
                host -> InetAddress.getAllByName(host),
                endpoint -> cacheIp(redisHost).equals(endpoint.getHostText())
                        ? HostAndPort.of(redisHost, endpoint.getPort())
                        : endpoint));
    }

    private String cacheIp(String host) {
        try {
            return InetAddress.getAllByName(host)[0].getHostAddress();
        } catch (Exception e) {
            return host;
        }
    }
}