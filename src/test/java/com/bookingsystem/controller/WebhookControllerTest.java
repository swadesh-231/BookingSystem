package com.bookingsystem.controller;

import com.bookingsystem.service.BookingService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private WebhookController webhookController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webhookController, "endpointSecret", "whsec_test_secret");
        ReflectionTestUtils.setField(webhookController, "meterRegistry", new SimpleMeterRegistry());
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController).build();
    }

    @Test
    void handleStripeWebhook_MissingSignature_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/webhooks/payment"))
                .andExpect(status().isBadRequest());
    }
}
