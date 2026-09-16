package com.claircore.iam.infrastructure.persistence.jpa.entities;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.UserStatus;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Storage shape of {@code User}.
 *
 * <p>The two column names are stated explicitly. They came from the aggregate's
 * {@code @AttributeOverride} and from an embedded component's field name, neither of which survives
 * the move to converters — without them the columns would derive from these field names and become
 * {@code email} and {@code password}.
 */
@Entity
@Table(name = "users")
public class UserPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "address", nullable = false, unique = true)
    private EmailAddress email;

    @Column(name = "password_hash")
    private Password password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider")
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_subject")
    private String oauthSubject;

    public UserPersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public EmailAddress getEmail() { return email; }
    public void setEmail(EmailAddress email) { this.email = email; }

    public Password getPassword() { return password; }
    public void setPassword(Password password) { this.password = password; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public OAuthProvider getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(OAuthProvider oauthProvider) { this.oauthProvider = oauthProvider; }

    public String getOauthSubject() { return oauthSubject; }
    public void setOauthSubject(String oauthSubject) { this.oauthSubject = oauthSubject; }
}
