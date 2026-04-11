package com.loyaltyService.user_service.controller;

import com.loyaltyService.user_service.dto.KycStatusResponse;
import com.loyaltyService.user_service.entity.KycDetail;
import com.loyaltyService.user_service.service.KycService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KycControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KycService kycService;

    @InjectMocks
    private KycController kycController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(kycController).build();
    }

    @Test
    void submitDelegatesMultipartRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("docFile", "id.png", "image/png", "abc".getBytes());
        when(kycService.submitKyc(eq(5L), eq(KycDetail.DocType.PASSPORT), eq("P12345"), eq(file)))
                .thenReturn(KycStatusResponse.builder().status("PENDING").build());

        mockMvc.perform(multipart("/api/kyc/submit")
                        .file(file)
                        .header("X-User-Id", "5")
                        .param("docType", "PASSPORT")
                        .param("docNumber", "P12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("KYC submitted successfully"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void statusReturnsCurrentKycState() throws Exception {
        when(kycService.getStatus(5L))
                .thenReturn(KycStatusResponse.builder().status("APPROVED").build());

        mockMvc.perform(get("/api/kyc/status")
                        .header("X-User-Id", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("KYC status fetched"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }
}
