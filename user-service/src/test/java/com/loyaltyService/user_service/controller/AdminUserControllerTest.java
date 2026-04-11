package com.loyaltyService.user_service.controller;

import com.loyaltyService.user_service.dto.AdminDashboardResponse;
import com.loyaltyService.user_service.dto.AdminUserResponse;
import com.loyaltyService.user_service.entity.User;
import com.loyaltyService.user_service.exception.GlobalExceptionHandler;
import com.loyaltyService.user_service.service.AdminUserService;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void dashboardReturnsDataForAdmin() throws Exception {
        AdminDashboardResponse dashboard = AdminDashboardResponse.builder()
                .totalUsers(42)
                .activeUsers(39)
                .build();
        when(adminUserService.getDashboard()).thenReturn(dashboard);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(42));
    }

    @Test
    void listUsersBuildsPageableAndParsesEnums() throws Exception {
        AdminUserResponse response = AdminUserResponse.builder()
                .id(7L)
                .name("Alice")
                .role("ADMIN")
                .status("ACTIVE")
                .createdAt(Instant.parse("2026-04-01T00:00:00Z"))
                .build();
        when(adminUserService.listUsers(org.mockito.ArgumentMatchers.any(Pageable.class),
                eq(User.UserStatus.ACTIVE), eq(User.Role.ADMIN)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/admin/users")
                        .header("X-User-Role", "ADMIN")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sortBy", "name")
                        .param("sortDir", "asc")
                        .param("status", "active")
                        .param("userRole", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Alice"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminUserService).listUsers(pageableCaptor.capture(), eq(User.UserStatus.ACTIVE), eq(User.Role.ADMIN));
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("name")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("name").isAscending()).isTrue();
    }

    @Test
    void listUsersRejectsInvalidSortDirection() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("X-User-Role", "ADMIN")
                        .param("sortDir", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sortDir: sideways"));
    }

    @Test
    void findByDateRangeRejectsInvertedDates() throws Exception {
        mockMvc.perform(get("/api/admin/users/search/date-range")
                        .header("X-User-Role", "ADMIN")
                        .param("from", "2026-04-10")
                        .param("to", "2026-04-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'from' date must be before or equal to 'to' date"));
    }

    @Test
    void blockUserRejectsSelfBlock() throws Exception {
        mockMvc.perform(patch("/api/admin/users/5/block")
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Id", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Admin cannot block themselves"));
    }

    @Test
    void changeRoleParsesRoleValue() throws Exception {
        AdminUserResponse response = AdminUserResponse.builder()
                .id(9L)
                .role("MERCHANT")
                .build();
        when(adminUserService.changeRole(9L, User.Role.MERCHANT)).thenReturn(response);

        mockMvc.perform(patch("/api/admin/users/9/role")
                        .header("X-User-Role", "ADMIN")
                        .param("newRole", "merchant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("MERCHANT"));
    }

    @Test
    void nonAdminRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/admin/users/3")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }
}
