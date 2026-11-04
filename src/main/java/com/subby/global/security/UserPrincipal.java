package com.subby.global.security;

import com.subby.user.User;
import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * 로그인한 사용자. 이메일 로그인과 소셜 로그인이 같은 클래스를 써서 컨트롤러는 로그인 방식을 신경 쓰지 않는다.
 * 세션에 직렬화되어 DB에 저장되므로 최소한의 값만 담는다. 이름(getName)은 사용자 ID이고,
 * Spring Session이 이 값으로 세션을 색인해서 비밀번호 재설정 시 해당 사용자의 모든 세션을 지울 수 있다.
 */
public class UserPrincipal implements UserDetails, OAuth2User, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final List<GrantedAuthority> AUTHORITIES =
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

    private final Long id;
    private String passwordHash;

    private UserPrincipal(Long id, String passwordHash) {
        this.id = id;
        this.passwordHash = passwordHash;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getPasswordHash());
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    @Override
    public String getUsername() {
        return String.valueOf(id);
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Map<String, Object> getAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", id);
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    /** 인증이 끝나면 비밀번호 해시를 지워서 세션에 저장되지 않게 한다. */
    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}
