package com.gridu.store.secure.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gridu.store.model.UserEntity;
import com.gridu.store.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET_KEY = "SecretKeySecretKeySecretKeySecretKeySecretKeySecretKey";

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    public void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY", SECRET_KEY);
    }


    @Test
    void isTokenValid() {
        UserEntity user = new UserEntity(1L, "user@gmail.com", "passwordEncode", UserRole.USER, null);
        String token = jwtService.generateToken(user);
        boolean result = jwtService.isTokenValid(token, user);
        assertTrue(result);
    }

    @Test
    void isTokenValid_tokenOtherUser() {
        UserEntity user = new UserEntity(1L, "user@gmail.com", "passwordEncode", UserRole.USER, null);
        UserEntity user2 = new UserEntity(2L, "user2@gmail.com", "passwordEncode", UserRole.USER, null);
        String token = jwtService.generateToken(user);
        boolean result = jwtService.isTokenValid(token, user2);
        assertFalse(result);
    }
}