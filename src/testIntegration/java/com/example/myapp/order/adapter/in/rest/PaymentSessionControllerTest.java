package com.example.myapp.order.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.myapp.order.adapter.in.rest.dto.CreateSessionRequest;
import com.example.myapp.order.adapter.in.rest.dto.InlineCardDetails;
import com.example.myapp.order.application.port.in.CreatePaymentSessionUseCase;
import com.example.myapp.order.domain.exception.CardNotFoundException;
import com.example.myapp.order.domain.exception.PaymentSessionFailedException;
import com.example.myapp.order.domain.model.PaymentSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentSessionController.class)
class PaymentSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreatePaymentSessionUseCase useCase;

    private static final PaymentSession MOCK_SESSION = PaymentSession.builder()
            .sessionId("session-web-123")
            .status("APPROVED")
            .expiresAt(Instant.parse("2030-01-01T12:00:00Z"))
            .build();

    @ParameterizedTest
    @MethodSource("provideValidRequests")
    void createSessionWithValidConfigurationsShouldReturnCreated(CreateSessionRequest request) throws Exception {
        // GIVEN
        when(useCase.createSession(any())).thenReturn(MOCK_SESSION);

        // WHEN & THEN
        mockMvc.perform(post("/createSession")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.session_id").value("session-web-123"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.expires_at").value("2030-01-01T12:00:00Z"));
    }

    private static Stream<Arguments> provideValidRequests() {
        // 1. Saved Card Request
        CreateSessionRequest savedCardReq = new CreateSessionRequest(CreateSessionRequest.CardTypeEnum.SAVED_CARD);
        savedCardReq.setCardId("card-id-abc");

        // 2. Autopay Card Request
        CreateSessionRequest autopayCardReq = new CreateSessionRequest(CreateSessionRequest.CardTypeEnum.AUTOPAY_CARD);
        autopayCardReq.setCardId("card-id-def");

        // 3. New Card Request
        CreateSessionRequest newCardReq = new CreateSessionRequest(CreateSessionRequest.CardTypeEnum.NEW_CARD);
        InlineCardDetails inline = new InlineCardDetails();
        inline.setCardNumber("1234567890123456");
        inline.setExpirationMonth("12");
        inline.setExpirationYear("28");
        inline.setCvv("123");
        inline.setCardholderName("John Doe");
        newCardReq.setInlineCardDetails(inline);

        return Stream.of(
                Arguments.of(savedCardReq),
                Arguments.of(autopayCardReq),
                Arguments.of(newCardReq)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidRequests")
    void createSessionWithInvalidConfigurationsShouldReturnBadRequest(CreateSessionRequest request) throws Exception {
        // WHEN & THEN
        mockMvc.perform(post("/createSession")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request Parameters"));
    }

    private static Stream<Arguments> provideInvalidRequests() {
        // 1. Missing Card ID for SAVED_CARD
        CreateSessionRequest req1 = new CreateSessionRequest(CreateSessionRequest.CardTypeEnum.SAVED_CARD);

        // 2. Missing Inline Card Details for NEW_CARD
        CreateSessionRequest req2 = new CreateSessionRequest(CreateSessionRequest.CardTypeEnum.NEW_CARD);

        // 3. Missing Card Type (Note: request validation ensures card_type is not null)
        CreateSessionRequest req3 = new CreateSessionRequest();
        req3.setCardId("card-id-123");

        return Stream.of(
                Arguments.of(req1),
                Arguments.of(req2),
                Arguments.of(req3)
        );
    }

    @ParameterizedTest
    @MethodSource("provideExceptionMappings")
    void createSessionHandlingExceptionScenariosShouldReturnMappedHttpStatus(
            RuntimeException thrownException, int expectedHttpStatus, String expectedTitle) throws Exception {
        // GIVEN
        CreateSessionRequest request = new CreateSessionRequest(CreateSessionRequest.CardTypeEnum.SAVED_CARD);
        request.setCardId("card-id-123");

        when(useCase.createSession(any())).thenThrow(thrownException);

        // WHEN & THEN
        mockMvc.perform(post("/createSession")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedHttpStatus))
                .andExpect(jsonPath("$.title").value(expectedTitle))
                .andExpect(jsonPath("$.detail").value("Error details"));
    }

    private static Stream<Arguments> provideExceptionMappings() {
        return Stream.of(
                Arguments.of(new CardNotFoundException("Error details"), 404, "Card Not Found"),
                Arguments.of(new PaymentSessionFailedException("Error details"), 502, "Payment Gateway Failure")
        );
    }
}
