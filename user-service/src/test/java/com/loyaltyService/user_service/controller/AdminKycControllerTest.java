package com.loyaltyService.user_service.controller;

import com.loyaltyService.user_service.dto.KycStatusResponse;
import com.loyaltyService.user_service.service.KycService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminKycControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KycService kycService;

    @InjectMocks
    private AdminKycController adminKycController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminKycController).build();
    }

    @Test
    void pendingRejectsNonAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/kyc/pending")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void pendingAllowsSupportRoleAndUsesAscendingSort() throws Exception {
        KycStatusResponse response = KycStatusResponse.builder()
                .kycId(11L)
                .userId(7L)
                .status("PENDING")
                .build();
        when(kycService.getPendingKyc(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/admin/kyc/pending")
                        .header("X-User-Role", "SUPPORT")
                        .param("page", "1")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(kycService).getPendingKyc(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(3);
        assertThat(pageable.getSort().getOrderFor("submittedAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("submittedAt").isAscending()).isTrue();
    }

    @Test
    void approveByKycIdDelegatesToService() throws Exception {
        when(kycService.approve(15L, "admin@test.com"))
                .thenReturn(KycStatusResponse.builder().kycId(15L).status("APPROVED").build());

        mockMvc.perform(post("/api/admin/kyc/15/approve")
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Email", "admin@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("KYC approved"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void rejectByUserIdDelegatesToService() throws Exception {
        when(kycService.rejectByUserId(8L, "mismatch", "admin@test.com"))
                .thenReturn(KycStatusResponse.builder().userId(8L).status("REJECTED").build());

        mockMvc.perform(post("/api/admin/kyc/user/8/reject")
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Email", "admin@test.com")
                        .param("reason", "mismatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("KYC rejected for userId: 8"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }
}
