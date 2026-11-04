package com.subby.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    Optional<UserIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<UserIdentity> findByUserIdAndProvider(Long userId, AuthProvider provider);

    boolean existsByUserIdAndProvider(Long userId, AuthProvider provider);

    List<UserIdentity> findAllByUserIdOrderByCreatedAtAsc(Long userId);

    long countByUserId(Long userId);
}
