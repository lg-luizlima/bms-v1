package br.com.tlf.shared.configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Credit Consent BMS")
                        .description("Microsserviço de Consentimentos: atua como um roteador dinâmico de "
                                + "compliance, expondo os termos de produto pendentes de aceite para um cliente "
                                + "(GET /credit-core/v1/terms) e registrando o consentimento assinado por ele "
                                + "sobre esses termos (POST /credit-core/v1/consents). Cada consentimento "
                                + "registrado é publicado de forma assíncrona via outbox pattern/Debezium/Kafka "
                                + "para consumidores downstream.")
                        .version("v1"))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local"),
                        new Server().url("https://fintech-hml.vivo.com.br/gateway").description("Homologação"),
                        new Server().url("https://fintech.vivo.com.br/gateway").description("Produção")));
    }
}
