package com.loyaltyService.user_service;

import com.cloudinary.Cloudinary;
import com.loyaltyService.user_service.client.AuthServiceClient;
import com.loyaltyService.user_service.client.AuthServiceClientFallback;
import com.loyaltyService.user_service.client.RewardsServiceClientFallback;
import com.loyaltyService.user_service.client.WalletServiceClientFallback;
import com.loyaltyService.user_service.config.CloudinaryConfig;
import com.loyaltyService.user_service.dto.ApiResponse;
import com.loyaltyService.user_service.exception.BadRequestException;
import com.loyaltyService.user_service.exception.DuplicateKycException;
import com.loyaltyService.user_service.exception.GlobalExceptionHandler;
import com.loyaltyService.user_service.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SupportCoverageTest {

    @Test
    void apiResponseFactoryMethodsPopulateFields() {
        ApiResponse<String> withData = ApiResponse.ok("done", "value");
        ApiResponse<Void> withoutData = ApiResponse.ok("done");

        assertThat(withData.isSuccess()).isTrue();
        assertThat(withData.getData()).isEqualTo("value");
        assertThat(withData.getTimestamp()).isNotNull();
        assertThat(withoutData.isSuccess()).isTrue();
        assertThat(withoutData.getData()).isNull();
    }

    @Test
    void fallbackClientsAreNoOps() {
        new AuthServiceClientFallback().updateProfile(new AuthServiceClient.UpdateProfileRequest(1L, "Sam", "999"));
        new AuthServiceClientFallback().updateStatus(new AuthServiceClient.StatusUpdateRequest(1L, "BLOCKED"));
        new AuthServiceClientFallback().updateRole(new AuthServiceClient.RoleUpdateRequest(1L, "ADMIN"));
        new RewardsServiceClientFallback().createRewardAccount(1L);
        new WalletServiceClientFallback().createWallet(1L);
    }

    @Test
    void cloudinaryConfigBuildsClientFromProperties() {
        CloudinaryConfig config = new CloudinaryConfig();
        ReflectionTestUtils.setField(config, "cloudName", "demo-cloud");
        ReflectionTestUtils.setField(config, "apiKey", "key-123");
        ReflectionTestUtils.setField(config, "apiSecret", "secret-456");

        Cloudinary cloudinary = config.cloudinary();

        assertThat(cloudinary.config.cloudName).isEqualTo("demo-cloud");
        assertThat(cloudinary.config.apiKey).isEqualTo("key-123");
        assertThat(cloudinary.config.apiSecret).isEqualTo("secret-456");
    }

    @Test
    void exceptionHandlerMapsKnownExceptions() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        assertThat(handler.badRequest(new BadRequestException("bad")).getStatusCode().value()).isEqualTo(400);
        assertThat(handler.notFound(new ResourceNotFoundException("missing")).getStatusCode().value()).isEqualTo(404);
        assertThat(handler.duplicate(new DuplicateKycException("duplicate")).getStatusCode().value()).isEqualTo(409);
        assertThat(handler.general(new IllegalStateException("boom")).getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void exceptionHandlerFormatsValidationErrors() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        SamplePayload payload = new SamplePayload("");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(payload, "payload");
        bindingResult.addError(new FieldError("payload", "name", "must not be blank"));
        Method method = ValidationTarget.class.getDeclaredMethod("accept", SamplePayload.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        String message = handler.validation(exception).getBody().getMessage();

        assertThat(message).isEqualTo("name: must not be blank");
    }

    private record SamplePayload(@NotBlank String name) {}

    private static class ValidationTarget {
        @SuppressWarnings("unused")
        void accept(@Valid SamplePayload payload) {
        }
    }
}
