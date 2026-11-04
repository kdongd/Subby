package com.subby.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subby.exchange.ExchangeRateClient;
import com.subby.global.security.UserPrincipal;
import com.subby.mail.MailSender;
import com.subby.user.User;
import com.subby.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 통합 테스트 공통 설정. 모든 통합 테스트가 같은 설정을 써서 스프링 컨텍스트와 DB 컨테이너를 한 번만 띄운다.
 * 메일 발송과 환율 API는 실제로 호출하지 않도록 가짜(Mock)로 바꾼다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "password123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @MockitoBean
    protected MailSender mailSender;

    @MockitoBean
    protected ExchangeRateClient exchangeRateClient;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE notification_logs, subscriptions, user_identities, password_reset_tokens,
                    login_attempts, exchange_rates, users, spring_session_attributes, spring_session
                RESTART IDENTITY CASCADE
                """);
    }

    protected User createUser(String email) {
        return userRepository.save(User.createLocal(email, passwordEncoder.encode(PASSWORD), "테스터"));
    }

    /** 해당 사용자로 로그인한 상태의 요청을 만든다. */
    protected RequestPostProcessor loginAs(User user) {
        return user(UserPrincipal.from(user));
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
