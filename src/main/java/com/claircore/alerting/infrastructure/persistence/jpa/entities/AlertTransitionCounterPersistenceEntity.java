package com.claircore.alerting.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One row: the last alert transition sequence handed out. Locked on every increment. */
@Entity
@Table(name = "alert_transition_counter")
public class AlertTransitionCounterPersistenceEntity {
    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;
    @Column(name = "last_value", nullable = false)
    private long value;

    public AlertTransitionCounterPersistenceEntity() {
        // JPA
    }

    public AlertTransitionCounterPersistenceEntity(long value) {
        this.id = SINGLETON_ID;
        this.value = value;
    }

    public Integer getId() { return id; }
    public long getValue() { return value; }
    public void setValue(long value) { this.value = value; }
}
