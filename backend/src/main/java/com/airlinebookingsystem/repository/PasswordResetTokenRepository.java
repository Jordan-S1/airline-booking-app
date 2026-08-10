package com.airlinebookingsystem.repository;

import com.airlinebookingsystem.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * The only lookup path: a caller presents a token, and it is hashed before
     * it gets here. There is deliberately no way to find a token by user, so a
     * bug cannot accidentally hand someone else's out.
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Marks every outstanding token for a user as used.
     *
     * <p>Called when a new one is issued and again after a successful reset, so
     * at most one token is ever live for an account — a second request cannot
     * leave the first still working.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :now "
            + "WHERE t.user.id = :userId AND t.usedAt IS NULL")
    int invalidateOutstanding(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
