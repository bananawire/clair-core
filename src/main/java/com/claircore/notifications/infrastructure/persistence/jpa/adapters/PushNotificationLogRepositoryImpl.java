package com.claircore.notifications.infrastructure.persistence.jpa.adapters;

import com.claircore.notifications.domain.model.aggregates.PushNotificationLog;
import com.claircore.notifications.domain.repositories.PushNotificationLogRepository;
import com.claircore.notifications.infrastructure.persistence.jpa.assemblers.PushNotificationLogPersistenceAssembler;
import com.claircore.notifications.infrastructure.persistence.jpa.repositories.PushNotificationLogPersistenceRepository;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class PushNotificationLogRepositoryImpl implements PushNotificationLogRepository {

    /** The ordering the REST layer used to pass down as part of its Pageable. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final PushNotificationLogPersistenceRepository pushNotificationLogPersistenceRepository;

    public PushNotificationLogRepositoryImpl(PushNotificationLogPersistenceRepository pushNotificationLogPersistenceRepository) {
        this.pushNotificationLogPersistenceRepository = pushNotificationLogPersistenceRepository;
    }

    @Override
    public PushNotificationLog save(PushNotificationLog pushNotificationLog) {
        var saved = pushNotificationLogPersistenceRepository.save(
                PushNotificationLogPersistenceAssembler.toPersistenceFromDomain(pushNotificationLog));
        return PushNotificationLogPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public PageResult<PushNotificationLog> findByUserId(UUID userId, int page, int size) {
        var found = pushNotificationLogPersistenceRepository.findByUserId(userId, PageRequest.of(page, size, NEWEST_FIRST));
        return new PageResult<>(
                found.getContent().stream().map(PushNotificationLogPersistenceAssembler::toDomainFromPersistence).toList(),
                page,
                size,
                found.getTotalElements());
    }
}
