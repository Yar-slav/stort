package com.gridu.store.service.implementation;

import com.gridu.store.dto.request.UserLoginRequest;
import com.gridu.store.dto.request.UserRegistrationRequestDto;
import com.gridu.store.dto.response.LoginResponseDto;
import com.gridu.store.dto.response.MessageResponseDto;
import com.gridu.store.exception.ApiException;
import com.gridu.store.exception.Exceptions;
import com.gridu.store.model.CartStatus;
import com.gridu.store.model.UserEntity;
import com.gridu.store.model.UserRole;
import com.gridu.store.repository.CartRepo;
import com.gridu.store.repository.UserRepo;
import com.gridu.store.secure.config.JwtService;
import com.gridu.store.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CartRepo cartRepo;

    // Why do you need transactional here?
    @Transactional
    @Override
    public MessageResponseDto register(UserRegistrationRequestDto requestDto) {
        // What happens if two users would try to register with the same email simultaneously?
        // Please, try to fix this and guarantee uniqueness of emails in each possible situation
        // Optionally you may want to guarantee 409 response in such cases but covering 100% scenarios wouldn't be easy
        // for this one, so it's up to you to implement it or not, I need only uniqueness of emails.
        checkIfUserExist(requestDto);
        UserEntity userEntity = UserEntity.builder()
                .email(requestDto.getEmail())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .userRole(UserRole.USER)
                .build();

        userRepo.save(userEntity);
        // If you have only two scenarios: fail with 409 and success, you would cover it with http statuses
        // You don't need to write any messages to your clients, you're making API here, not UI side
        return new MessageResponseDto("User with email: " + userEntity.getEmail() + " is successfully registered");
    }

    // Why do you need transactional here?
    @Transactional
    @Override
    public LoginResponseDto login(UserLoginRequest requestDto) {
        authenticate(requestDto);
        UserEntity userEntity = userRepo.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new ApiException(Exceptions.USER_NOT_FOUND));
        String token = jwtService.generateToken(userEntity);
        cartRepo.deleteByUserAndCartStatus(userEntity, CartStatus.ADDED_TO_CART);
        return new LoginResponseDto(token);
    }

    private void authenticate(UserLoginRequest requestDto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            requestDto.getEmail(),
                            requestDto.getPassword()));
        } catch (BadCredentialsException e) {
            throw new ApiException(Exceptions.USER_INCORRECT_PASSWORD);
        }
    }

    private void checkIfUserExist(UserRegistrationRequestDto userRegistrationRequestDto) {
        // Optional: existsByEmail would have better performance and readability
        boolean present = userRepo
                .findByEmail(userRegistrationRequestDto.getEmail())
                .isPresent();
        if(present) {
            throw new ApiException(Exceptions.USER_EXIST);
        }
    }

    public UserEntity getUserEntityByToken(String authHeader) {
        String token = authHeader.substring(7);
        String userEmail = jwtService.extractUsername(token);
        return userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(Exceptions.USER_NOT_FOUND));
    }

}
