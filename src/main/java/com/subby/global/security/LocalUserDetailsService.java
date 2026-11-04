package com.subby.global.security;

import com.subby.user.User;
import com.subby.user.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 + 비밀번호 로그인에서 이메일로 사용자를 찾는다. 비밀번호가 없는 소셜 전용 계정은 찾지 못한 것으로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class LocalUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalized)
                .filter(User::hasPassword)
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
    }
}
