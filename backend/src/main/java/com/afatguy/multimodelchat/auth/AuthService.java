package com.afatguy.multimodelchat.auth;

import com.afatguy.multimodelchat.auth.AuthDtos.LoginRequest;
import com.afatguy.multimodelchat.auth.AuthDtos.LoginResponse;
import com.afatguy.multimodelchat.auth.AuthDtos.UserProfile;
import com.afatguy.multimodelchat.persistence.entity.SysUserEntity;
import com.afatguy.multimodelchat.persistence.repo.SysUserRepository;
import com.afatguy.multimodelchat.persistence.repo.SysUserRoleRepository;
import com.afatguy.multimodelchat.security.AppUserPrincipal;
import com.afatguy.multimodelchat.security.JwtTokenService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
        SysUserRepository userRepository,
        SysUserRoleRepository userRoleRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenService jwtTokenService
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResponse login(LoginRequest request) {
        SysUserEntity user = userRepository.findByUsernameAndEnabledTrue(request.username())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }

        List<String> roles = userRoleRepository.findRoleCodesByUserId(user.getId());
        AppUserPrincipal principal = new AppUserPrincipal(user.getId(), user.getUsername(), user.getDisplayName(), roles);
        String token = jwtTokenService.generateToken(principal);
        OffsetDateTime expiresAt = jwtTokenService.extractExpireTime(token);
        return new LoginResponse(token, expiresAt, toProfile(principal));
    }

    public UserProfile me(AppUserPrincipal principal) {
        return toProfile(principal);
    }

    private UserProfile toProfile(AppUserPrincipal principal) {
        return new UserProfile(principal.userId(), principal.username(), principal.displayName(), principal.roles());
    }
}