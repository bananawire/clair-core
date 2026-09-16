package com.claircore.billing.infrastructure.persistence.jpa.entities;

import com.claircore.billing.domain.model.valueobjects.PlanType;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Storage shape of {@code UserPlan}. The table name is stated explicitly: it used to be derived
 * from the aggregate's class name, and the entity is no longer called {@code UserPlan}.
 */
@Entity
@Table(name = "user_plan")
public class UserPlanPersistenceEntity extends AuditableAbstractPersistenceEntity {

    private UserId userId;

    @Enumerated(EnumType.STRING)
    private PlanType planType;

    private LocalDate startDate;
    private LocalDate endDate;

    public UserPlanPersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public UserId getUserId() { return userId; }
    public void setUserId(UserId userId) { this.userId = userId; }

    public PlanType getPlanType() { return planType; }
    public void setPlanType(PlanType planType) { this.planType = planType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
