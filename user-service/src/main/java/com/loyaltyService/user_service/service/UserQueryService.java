package com.loyaltyService.user_service.service;

import com.loyaltyService.user_service.dto.UserProfileResponse;
import com.loyaltyService.user_service.dto.UserLookupResponse;

/**
 * CQRS — Query side: all read operations for User.
 * Results are cached in Redis.
 */
public interface UserQueryService {

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse getUserProfile(Long userId);

    String getUserStatus(Long userId);

    UserLookupResponse findUserForTransfer(String email, String phone);
}
