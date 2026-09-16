package com.claircore.shared.domain.model.aggregates;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for aggregate roots that publish domain events.
 *
 * <p>It deliberately does not extend Spring Data's {@code AbstractAggregateRoot}. That class works
 * by having the repository publish the events of the instance handed to {@code save()}, and after
 * the persistence split the instance a Spring Data repository sees is the persistence entity, never
 * the aggregate — events registered here would be collected from the wrong object and silently
 * dropped. Draining is therefore the adapter's job: it calls {@link #domainEvents()} on the
 * aggregate after a successful save, publishes each event, and calls {@link #clearDomainEvents()}.
 *
 * <p>Subclasses record events with the {@code protected registerEvent(Object)}: only the aggregate
 * itself decides what it emits. Nothing from Spring, JPA or Hibernate appears here, which is what
 * makes this class safe to sit under a {@code domain} package.
 */
public abstract class AbstractDomainAggregateRoot {

    private final List<Object> domainEvents = new ArrayList<>();

    /** Records an event to be published once the aggregate has been stored. */
    protected void registerEvent(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        domainEvents.add(event);
    }

    /** Events registered since the last drain, oldest first. */
    public List<Object> domainEvents() {
        return List.copyOf(domainEvents);
    }

    /** Called by the persistence adapter once the events have been published. */
    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
