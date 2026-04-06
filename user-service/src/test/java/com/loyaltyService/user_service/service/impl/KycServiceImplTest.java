package com.loyaltyService.user_service.service.impl;

import com.loyaltyService.user_service.client.WalletServiceClient;
import com.loyaltyService.user_service.dto.KycStatusResponse;
import com.loyaltyService.user_service.entity.AuditLog;
import com.loyaltyService.user_service.entity.KycDetail;
import com.loyaltyService.user_service.entity.User;
import com.loyaltyService.user_service.exception.DuplicateKycException;
import com.loyaltyService.user_service.mapper.KycMapper;
import com.loyaltyService.user_service.repository.AuditLogRepository;
import com.loyaltyService.user_service.repository.KycRepository;
import com.loyaltyService.user_service.repository.UserRepository;
import com.loyaltyService.user_service.service.CloudinaryService;
import com.loyaltyService.user_service.service.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceImplTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private KycRepository kycRepo;
    @Mock
    private AuditLogRepository auditRepo;
    @Mock
    private WalletServiceClient walletServiceClient;
    @Mock
    private KafkaProducerService kafkaProducer;
    @Mock
    private KycMapper kycMapper;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private KycServiceImpl kycService;

    private User testUser;
    private KycDetail testKyc;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("test@example.com").build();
        testKyc = KycDetail.builder().id(100L).user(testUser).status(KycDetail.KycStatus.PENDING).build();
        ReflectionTestUtils.setField(kycService, "uploadDir", "target/test-uploads");
    }

    @Test
    void testSubmitKyc_Success() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.empty());
        when(cloudinaryService.uploadFile(any(), eq(1L), eq(KycDetail.DocType.PAN.name())))
                .thenReturn("cloudinary://kyc/doc");
        when(kycRepo.save(any(KycDetail.class))).thenReturn(testKyc);
        
        KycStatusResponse resMock = new KycStatusResponse();
        resMock.setStatus("PENDING");
        when(kycMapper.toResponse(any())).thenReturn(resMock);

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy".getBytes());

        KycStatusResponse res = kycService.submitKyc(1L, KycDetail.DocType.PAN, "ID12345", file);

        assertNotNull(res);
        assertEquals("PENDING", res.getStatus());
        verify(auditRepo, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testSubmitKyc_AlreadyPending() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(1L))
                .thenReturn(Optional.of(KycDetail.builder()
                        .id(99L)
                        .user(testUser)
                        .status(KycDetail.KycStatus.PENDING)
                        .build()));

        assertThrows(DuplicateKycException.class, () ->
            kycService.submitKyc(1L, KycDetail.DocType.PAN, "ID123", null));
    }

    @Test
    void testSubmitKyc_AlreadyApproved() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(1L))
                .thenReturn(Optional.of(KycDetail.builder()
                        .id(99L)
                        .user(testUser)
                        .status(KycDetail.KycStatus.APPROVED)
                        .build()));

        assertThrows(DuplicateKycException.class, () ->
            kycService.submitKyc(1L, KycDetail.DocType.PAN, "ID123", null));
    }

    @Test
    void testSubmitKyc_AfterRejected_AllowsResubmission() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(1L))
                .thenReturn(Optional.of(KycDetail.builder()
                        .id(99L)
                        .user(testUser)
                        .status(KycDetail.KycStatus.REJECTED)
                        .build()));
        when(kycRepo.save(any(KycDetail.class))).thenReturn(testKyc);

        KycStatusResponse resMock = new KycStatusResponse();
        resMock.setStatus("PENDING");
        when(kycMapper.toResponse(any())).thenReturn(resMock);

        KycStatusResponse res = kycService.submitKyc(1L, KycDetail.DocType.PAN, "ID12345", null);

        assertNotNull(res);
        assertEquals("PENDING", res.getStatus());
        verify(kycRepo).save(any(KycDetail.class));
    }

    @Test
    void testGetStatus_Success() {
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.of(testKyc));
        KycStatusResponse resMock = new KycStatusResponse();
        resMock.setStatus("PENDING");
        when(kycMapper.toResponse(testKyc)).thenReturn(resMock);

        KycStatusResponse res = kycService.getStatus(1L);
        assertNotNull(res);
        assertEquals("PENDING", res.getStatus());
    }

    @Test
    void testGetStatus_NotFound() {
        when(kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.empty());

        KycStatusResponse res = kycService.getStatus(1L);

        assertNotNull(res);
        assertEquals("NOT_SUBMITTED", res.getStatus());
    }
}
