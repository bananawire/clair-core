package com.claircore.billing.application.acl;

import com.claircore.billing.domain.model.aggregates.UserPlan;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingContextFacadeImplTest {

    @Mock
    private UserPlanRepository userPlanRepository;

    private BillingContextFacadeImpl facade;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        facade = new BillingContextFacadeImpl(new com.claircore.billing.application.internal.queryservices.SubscriptionQueryServiceImpl(
                org.mockito.Mockito.mock(com.claircore.billing.domain.repositories.PaymentRecordRepository.class), userPlanRepository));
    }

    @Test
    void shouldReturnPremiumLimitsWhenPlanIsActive() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
        UserPlan userPlan = new UserPlan(new UserId(userId));
        userPlan.upgradeToPremium();
        when(userPlanRepository.findByUserId(new UserId(userId))).thenReturn(Optional.of(userPlan));

        int organizations = facade.getMaxOrganizations(userId);
        int spaces = facade.getMaxSpaces(userId);
        int devices = facade.getMaxDevices(userId);
        boolean canAccessReports = facade.canAccessMonthlyReports(userId);

        assertThat(organizations).isEqualTo(3);
        assertThat(spaces).isEqualTo(5);
        assertThat(devices).isEqualTo(10);
        assertThat(canAccessReports).isTrue();
        verify(userPlanRepository, times(4)).findByUserId(new UserId(userId));
    }

    @Test
    void shouldReturnFreemiumLimitsWhenPremiumPlanIsExpired() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440011");
        UserPlan userPlan = new UserPlan(new UserId(userId));
        userPlan.upgradeToPremium();
        setEndDate(userPlan, LocalDate.now().minusDays(1));
        when(userPlanRepository.findByUserId(new UserId(userId))).thenReturn(Optional.of(userPlan));

        int organizations = facade.getMaxOrganizations(userId);
        int spaces = facade.getMaxSpaces(userId);
        int devices = facade.getMaxDevices(userId);
        boolean canAccessReports = facade.canAccessMonthlyReports(userId);

        assertThat(organizations).isEqualTo(1);
        assertThat(spaces).isEqualTo(1);
        assertThat(devices).isEqualTo(1);
        assertThat(canAccessReports).isFalse();
        verify(userPlanRepository, times(4)).findByUserId(new UserId(userId));
    }

    @Test
    void shouldReturnFreemiumDefaultsWhenUserPlanDoesNotExist() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440012");
        when(userPlanRepository.findByUserId(new UserId(userId))).thenReturn(Optional.empty());

        assertThat(facade.getMaxOrganizations(userId)).isEqualTo(1);
        assertThat(facade.getMaxSpaces(userId)).isEqualTo(1);
        assertThat(facade.getMaxDevices(userId)).isEqualTo(1);
        assertThat(facade.canAccessMonthlyReports(userId)).isFalse();
        verify(userPlanRepository, times(4)).findByUserId(new UserId(userId));
    }

    @Test
    void shouldResolveMonthlyReportAccessWithSingleRepositoryLookupWhenPlanExists() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440013");
        UserPlan userPlan = new UserPlan(new UserId(userId));
        userPlan.upgradeToPremium();
        when(userPlanRepository.findByUserId(new UserId(userId))).thenReturn(Optional.of(userPlan));

        boolean canAccessReports = facade.canAccessMonthlyReports(userId);

        assertThat(canAccessReports).isTrue();
        verify(userPlanRepository).findByUserId(new UserId(userId));
        verifyNoMoreInteractions(userPlanRepository);
    }

    private static void setEndDate(UserPlan userPlan, LocalDate endDate) {
        try {
            Field field = UserPlan.class.getDeclaredField("endDate");
            field.setAccessible(true);
            field.set(userPlan, endDate);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to set endDate for test", e);
        }
    }
}
