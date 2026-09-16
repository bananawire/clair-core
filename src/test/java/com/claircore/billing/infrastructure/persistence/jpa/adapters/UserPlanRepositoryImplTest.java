package com.claircore.billing.infrastructure.persistence.jpa.adapters;

import com.claircore.billing.domain.model.aggregates.UserPlan;
import com.claircore.billing.domain.model.valueobjects.PlanType;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfiguration.class, UserPlanRepositoryImpl.class})
class UserPlanRepositoryImplTest {

    @Autowired
    private UserPlanRepository repository;

    @Test
    void savesWithTheIdentityTheAggregateAssignedAndFillsTheAuditTimestamps() {
        var plan = new UserPlan(new UserId(UUID.randomUUID()));

        var saved = repository.save(plan);

        assertThat(saved.getId()).isEqualTo(plan.getId());
        assertThat(saved.getPlanType()).isEqualTo(PlanType.FREEMIUM);
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getEndDate()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    /** A stored plan is updated in place, not inserted a second time under a new id. */
    @Test
    void updatesTheStoredPlanWhenAnUpgradeIsSaved() {
        UUID userId = UUID.randomUUID();
        var plan = repository.save(new UserPlan(new UserId(userId)));
        plan.upgradeToPremium();

        var upgraded = repository.save(plan);

        assertThat(upgraded.getId()).isEqualTo(plan.getId());
        var found = repository.findByUserId(new UserId(userId)).orElseThrow();
        assertThat(found.getId()).isEqualTo(plan.getId());
        assertThat(found.getPlanType()).isEqualTo(PlanType.PREMIUM);
        assertThat(found.getEndDate()).isEqualTo(LocalDate.now().plusDays(30));
    }
}
