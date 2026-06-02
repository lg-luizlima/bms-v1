package br.com.tlf.configuration.common.rest.webclient;

import br.com.tlf.configuration.common.rest.dto.v1.error.ExceptionErrorDetailsDTO;
import br.com.tlf.configuration.common.rest.dto.v1.error.ExceptionErrorDetailsMapper;
import br.com.tlf.configuration.common.rest.exceptionhandler.error.NotificationException;
import br.com.tlf.domain.util.LogUtils;
import br.com.tlf.logging.webclient.WebClientLoggingFilter;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static br.com.tlf.application.ApplicationConstants.DEFAULT_CODECS_SIZE;

@Slf4j
@Configuration
public class WebClientConfiguration {

    public static  <S> S createFacade(String url, Class<S> interfaceFacade,
                                      Consumer<List<ExchangeFilterFunction>> filters, Consumer<HttpHeaders> defaultHeaders)
            throws NotificationException {

        LogUtils.log("url: {}, class {}", url, interfaceFacade.getSimpleName());

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60))
                .option         (ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
                .doOnConnected  (conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(60))
                        .addHandlerLast(new WriteTimeoutHandler(60))
                );
        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> clientDefaultCodecsConfigurer.defaultCodecs().maxInMemorySize(DEFAULT_CODECS_SIZE))
                .build();

        var webClientBuilder = WebClient.builder()
                .clientConnector     (new ReactorClientHttpConnector(httpClient))
                .baseUrl             (url)
                .defaultHeaders      (defaultHeaders)
                .defaultStatusHandler(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(ExceptionErrorDetailsDTO.class)
                            .flatMap(body -> {
                                LogUtils.error("Code: {}, Message: {}, Erros: {}",
                                resp.statusCode().value(), body.getMessage(),body.getErrors());

                                return Mono.error(createNotificationException(body, resp));
                            })
                )
                .filters(filters)
                .exchangeStrategies(strategies);

        WebClient client = webClientBuilder.build();

        var proxyFactory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(client)).build();

        return proxyFactory.createClient(interfaceFacade);
    }

    public static  <S> S createFacade(String url, Class<S> interfaceFacade,
                                      Consumer<List<ExchangeFilterFunction>> filters, Consumer<HttpHeaders> defaultHeaders, int timeoutSeconds)
            throws NotificationException {

        LogUtils.log("url: {}, class {}", url, interfaceFacade.getSimpleName());

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .option         (ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutSeconds * 1000)
                .doOnConnected  (conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeoutSeconds))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds))
                );

        var webClientBuilder = WebClient.builder()
                .clientConnector     (new ReactorClientHttpConnector(httpClient))
                .baseUrl             (url)
                .defaultHeaders      (defaultHeaders)
                .defaultStatusHandler(HttpStatusCode::isError, resp -> resp.bodyToMono(ExceptionErrorDetailsDTO.class)
                        .flatMap(body -> {
                            LogUtils.error("Code: {}, Message: {}, Erros: {}",
                                    resp.statusCode().value(), body.getMessage(),body.getErrors());

                            return Mono.error(createNotificationException(body, resp));
                        }))
                .filters(filters);

        WebClient client = webClientBuilder.build();

        var proxyFactory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(client)).build();

        return proxyFactory.createClient(interfaceFacade);
    }

    private static NotificationException createNotificationException(ExceptionErrorDetailsDTO body,
                                                                     ClientResponse resp) {
        var exceptionErrorDetailsMapper = Mappers.getMapper(ExceptionErrorDetailsMapper.class);
        return exceptionErrorDetailsMapper.toNotificationException(body, resp);
    }

    public static WebClient createRawWebClient(String url, WebClientLoggingFilter webClientLoggingFilter) throws NotificationException {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(60))
                        .addHandlerLast(new WriteTimeoutHandler(60))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(url)
                .filter(webClientLoggingFilter)
                .build();
    }

    public static HttpHeaders getDefaultHeaders() {
        HttpHeaders defaultHeaders = new HttpHeaders();
        defaultHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        defaultHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return defaultHeaders;
    }

    public static HttpHeaders getDefaultHeadersWithMultipartContentType() {
        HttpHeaders defaultHeaders = new HttpHeaders();
        defaultHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);

        return defaultHeaders;
    }
}
