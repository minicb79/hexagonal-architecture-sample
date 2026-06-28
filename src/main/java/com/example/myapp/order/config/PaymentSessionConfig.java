package com.example.myapp.order.config;

import com.example.myapp.order.adapter.out.cardsservice.CardsServiceAdapter;
import com.example.myapp.order.adapter.out.paymentsservice.PaymentsServiceAdapter;
import com.example.myapp.order.application.port.in.CreatePaymentSessionUseCase;
import com.example.myapp.order.application.port.out.cardsservice.CardClientPort;
import com.example.myapp.order.application.port.out.paymentsservice.PaymentClientPort;
import com.example.myapp.order.application.service.PaymentSessionApplicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring Inversion of Control wiring configuration.
 * Instantiates core use case beans manually using constructor injection.
 */
@Configuration
public class PaymentSessionConfig {

    @Bean
    public WebClient cardsWebClient(
            WebClient.Builder builder,
            @Value("${app.cards-service.base-url:http://localhost:8085}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient paymentsWebClient(
            WebClient.Builder builder,
            @Value("${app.payments-service.base-url:http://localhost:8085}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public CardClientPort cardClientPort(WebClient cardsWebClient) {
        return new CardsServiceAdapter(cardsWebClient);
    }

    @Bean
    public PaymentClientPort paymentClientPort(WebClient paymentsWebClient) {
        return new PaymentsServiceAdapter(paymentsWebClient);
    }

    @Bean
    public CreatePaymentSessionUseCase createPaymentSessionUseCase(
            CardClientPort cardClientPort,
            PaymentClientPort paymentClientPort) {
        return new PaymentSessionApplicationService(cardClientPort, paymentClientPort);
    }
}
