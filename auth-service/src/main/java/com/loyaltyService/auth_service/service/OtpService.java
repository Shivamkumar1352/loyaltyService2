package com.loyaltyService.auth_service.service;

import com.loyaltyService.auth_service.exception.AuthException;
import com.loyaltyService.auth_service.model.OtpStore;
import com.loyaltyService.auth_service.repository.OtpStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpStoreRepository otpStoreRepository;

    @Value("${otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${rate-limit.otp.capacity:3}")
    private int otpRateLimitCapacity;

    @Value("${rate-limit.otp.refill-minutes:10}")
    private int otpRateLimitWindow;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public String generateAndSaveOtpForPhone(String phone, OtpStore.OtpType type) {
        enforceRateLimitForPhone(phone, type);

        String otp = generateOtp();

        OtpStore otpStore = OtpStore.builder()
                .phone(phone)
                .otp(otp)
                .otpType(type)
                .expiryTime(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)  // ✅ was .isUsed(false)
                .build();

        otpStoreRepository.save(otpStore);

        log.info("OTP for phone {}: {} (expires in {} min)", maskPhone(phone), otp, otpExpiryMinutes);

        return otp;
    }

    @Transactional
    public String generateAndSaveOtpForEmail(String email, OtpStore.OtpType type) {
        enforceRateLimitForEmail(email, type);

        String otp = generateOtp();

        OtpStore otpStore = OtpStore.builder()
                .email(email)
                .otp(otp)
                .otpType(type)
                .expiryTime(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)  // ✅ was .isUsed(false)
                .build();

        otpStoreRepository.save(otpStore);

        log.info("OTP for email {}: {} (expires in {} min)", maskEmail(email), otp, otpExpiryMinutes);

        return otp;
    }

    @Transactional
    public void verifyPhoneOtp(String phone, String otp, OtpStore.OtpType type) {
        OtpStore otpStore = otpStoreRepository
                .findTopByPhoneAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(phone, type)
                .orElseThrow(() -> new AuthException("No active OTP found for this phone number", HttpStatus.BAD_REQUEST));

        validateOtp(otpStore, otp);

        otpStore.setUsed(true);
        otpStoreRepository.save(otpStore);
    }

    @Transactional
    public void verifyEmailOtp(String email, String otp, OtpStore.OtpType type) {
        OtpStore otpStore = otpStoreRepository
        		.findTopByEmailAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(email, type)
                .orElseThrow(() -> new AuthException("No active OTP found for this email", HttpStatus.BAD_REQUEST));

        validateOtp(otpStore, otp);

        otpStore.setUsed(true);
        otpStoreRepository.save(otpStore);
    }

    private void validateOtp(OtpStore otpStore, String providedOtp) {
        if (otpStore.isUsed()) {
            throw new AuthException("OTP has already been used.", HttpStatus.BAD_REQUEST);
        }

        if (otpStore.isExpired()) {
            throw new AuthException("OTP has expired. Please request a new one.", HttpStatus.GONE);
        }

        otpStore.setAttemptCount(otpStore.getAttemptCount() + 1);
        otpStoreRepository.save(otpStore);

        if (!otpStore.getOtp().equals(providedOtp)) {
            throw new AuthException("Invalid OTP. Please try again.", HttpStatus.BAD_REQUEST);
        }
    }

    private void enforceRateLimitForPhone(String phone, OtpStore.OtpType type) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(otpRateLimitWindow);
        long recentCount = otpStoreRepository.countRecentOtpsByPhone(phone, type, windowStart);

        if (recentCount >= otpRateLimitCapacity) {
            throw new AuthException(
                    String.format("Too many OTP requests. Please try again after %d minutes.", otpRateLimitWindow),
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }

    private void enforceRateLimitForEmail(String email, OtpStore.OtpType type) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(otpRateLimitWindow);
        long recentCount = otpStoreRepository.countRecentOtpsByEmail(email, type, windowStart);

        if (recentCount >= otpRateLimitCapacity) {
            throw new AuthException(
                    String.format("Too many OTP requests. Please try again after %d minutes.", otpRateLimitWindow),
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }

    private String generateOtp() {
        int max = (int) Math.pow(10, otpLength);
        int min = (int) Math.pow(10, otpLength - 1);
        int otp = min + SECURE_RANDOM.nextInt(max - min);
        return String.valueOf(otp);
    }

    public int getOtpExpiryMinutes() {
        return otpExpiryMinutes;
    }

    @Scheduled(fixedDelayString = "PT30M")
    @Transactional
    public void cleanupExpiredOtps() {
        otpStoreRepository.deleteExpiredOtps(LocalDateTime.now());
        log.info("Expired OTPs cleaned up");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****";
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        String masked = name.length() > 2
                ? name.charAt(0) + "****" + name.charAt(name.length() - 1)
                : "****";
        return masked + "@" + domain;
    }
}