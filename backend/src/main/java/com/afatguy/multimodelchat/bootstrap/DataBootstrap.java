package com.afatguy.multimodelchat.bootstrap;

import com.afatguy.multimodelchat.persistence.entity.ModelConfigEntity;
import com.afatguy.multimodelchat.persistence.entity.SysRoleEntity;
import com.afatguy.multimodelchat.persistence.entity.SysUserEntity;
import com.afatguy.multimodelchat.persistence.entity.SysUserRoleEntity;
import com.afatguy.multimodelchat.persistence.repo.ModelConfigRepository;
import com.afatguy.multimodelchat.persistence.repo.SysRoleRepository;
import com.afatguy.multimodelchat.persistence.repo.SysUserRepository;
import com.afatguy.multimodelchat.persistence.repo.SysUserRoleRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataBootstrap {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final PasswordEncoder passwordEncoder;

    public DataBootstrap(
        SysUserRepository userRepository,
        SysRoleRepository roleRepository,
        SysUserRoleRepository userRoleRepository,
        ModelConfigRepository modelConfigRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.modelConfigRepository = modelConfigRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        SysRoleEntity adminRole = ensureRole("ADMIN", "Administrator");
        SysRoleEntity userRole = ensureRole("USER", "Normal User");
        SysUserEntity admin = ensureAdminUser();
        ensureUserRole(admin.getId(), adminRole.getId());
        ensureUserRole(admin.getId(), userRole.getId());

        if (modelConfigRepository.count() == 0) {
            seedDefaultModels();
        }
    }

    private SysRoleEntity ensureRole(String code, String name) {
        return roleRepository.findByRoleCode(code).orElseGet(() -> {
            SysRoleEntity role = new SysRoleEntity();
            role.setRoleCode(code);
            role.setRoleName(name);
            return roleRepository.save(role);
        });
    }

    private SysUserEntity ensureAdminUser() {
        return userRepository.findByUsernameAndEnabledTrue("admin").orElseGet(() -> {
            SysUserEntity user = new SysUserEntity();
            user.setUsername("admin");
            user.setDisplayName("Administrator");
            user.setEnabled(true);
            user.setPasswordHash(passwordEncoder.encode("admin123"));
            return userRepository.save(user);
        });
    }

    private void ensureUserRole(Long userId, Long roleId) {
        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            return;
        }
        SysUserRoleEntity rel = new SysUserRoleEntity();
        rel.setUserId(userId);
        rel.setRoleId(roleId);
        userRoleRepository.save(rel);
    }

    private void seedDefaultModels() {
        modelConfigRepository.saveAll(List.of(
            createModel("gpt-4o-mini", "GPT-4o Mini", "OPENAI_COMPATIBLE", "https://api.openai.com/v1/chat/completions", 30000, 30, true, "general,fast"),
            createModel("qwen-max", "Qwen Max", "OPENAI_COMPATIBLE", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", 40000, 20, true, "general,strong"),
            createModel("llama3", "Local Llama3", "OLLAMA", "http://localhost:11434/api/chat", 60000, 10, false, "local,private")
        ));
    }

    private ModelConfigEntity createModel(
        String modelCode,
        String displayName,
        String providerType,
        String endpoint,
        int timeoutMs,
        int maxQps,
        boolean enabled,
        String tags
    ) {
        ModelConfigEntity model = new ModelConfigEntity();
        model.setModelCode(modelCode);
        model.setDisplayName(displayName);
        model.setProviderType(providerType);
        model.setEndpoint(endpoint);
        model.setTimeoutMs(timeoutMs);
        model.setMaxQps(maxQps);
        model.setEnabled(enabled);
        model.setTags(tags);
        return model;
    }
}