package com.afatguy.multimodelchat.persistence.repo;

import com.afatguy.multimodelchat.persistence.entity.SysUserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysUserRepository extends JpaRepository<SysUserEntity, Long> {

    Optional<SysUserEntity> findByUsernameAndEnabledTrue(String username);
}