package com.jobify.microservices.services.Impl;

import com.jobify.microservices.entities.dtos.LoginRequestDto;
import com.jobify.microservices.entities.dtos.UserDto;
import com.jobify.microservices.entities.mappers.UserMapper;
import com.jobify.microservices.entities.models.User;
import com.jobify.microservices.exceptionHandling.customExceptions.UserAlreadyExistsException;
import com.jobify.microservices.exceptionHandling.customExceptions.UserNotFoundException;
import com.jobify.microservices.exceptionHandling.customExceptions.WrongCredentialsException;
import com.jobify.microservices.repositories.UserRepo;
import com.jobify.microservices.services.AuthService;
import com.jobify.microservices.utilities.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Date;

@Log4j2
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService, ReactiveAuthenticationManager {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public Mono<UserDto> signup(UserDto userDto) {
        final boolean newUser = true;
        return Mono.just(userDto)
                .doOnNext(user -> log.info("Searching for user in the system : {}",userDto))
                .map(UserDto::getEmail)
                .flatMap(userRepo::findUserByEmail)
                .map(user -> !newUser)
                .switchIfEmpty(Mono.just(newUser))
                .doOnNext(isNewUser -> log.info("new user : {}",isNewUser))
                .filter(user -> user.equals(newUser))
                .switchIfEmpty(Mono.error(new UserAlreadyExistsException("Signup failed")))
                .map(user -> userDto)
                .map(UserMapper.INSTANCE::dtoToModel)
                .doOnNext(user -> {
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    user.setCreated(new Date().getTime());
                    user.setUpdated(new Date().getTime());
                    log.info("saving user into system : {}",user);
                })
                .flatMap(userRepo::save)
                .doOnNext(user -> log.info("SignUp successful : {}",user))
                .map(UserMapper.INSTANCE::modelToDto);
    }

    @Override
    public Mono<String> login(LoginRequestDto loginRequestDto) {
        return Mono.just(loginRequestDto)
                .doOnNext(loginCredentials -> log.info("incoming login request : {}",loginCredentials))
                .flatMap(this::isValidUser)
                .doOnNext(user -> log.info("login authentication successful, generating token for {}",user))
                .flatMap(jwtTokenUtil::generateToken)
                .doOnNext(token -> log.info("token generated successfully : token {}",token));
    }

    private Mono<User> isValidUser(LoginRequestDto loginRequestDto) {
        return userRepo.findUserByEmail(loginRequestDto.getEmail())
                .switchIfEmpty(Mono.error(new UserNotFoundException("login authentication failed")))
                .filter(savedUser -> passwordEncoder.matches(loginRequestDto.getPassword(),savedUser.getPassword()))
                .switchIfEmpty(Mono.error(new WrongCredentialsException("login authentication failed")));
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
//        return Mono.just(jwtTokenUtil.validateToken(authentication.getCredentials().toString()))
//                .filter(valid -> true)
//                .switchIfEmpty(Mono.empty())
//                .flatMap(isValid -> jwtTokenUtil.extractAllClaims(authentication.getCredentials().toString()))
//                .map(claims -> {
//                    return new UsernamePasswordAuthenticationToken(
//                            claims.getSubject(),
//                            null,
//                            Collections.singletonList(new SimpleGrantedAuthority(claims.get(KEY_ROLE).toString()))
//                    );
//                });
        return null;
    }

}