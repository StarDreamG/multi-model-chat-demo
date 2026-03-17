package com.afatguy.multimodelchat.persistence.repo;

import com.afatguy.multimodelchat.persistence.entity.SysRoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysRoleRepository extends JpaRepository<SysRoleEntity, Long> {

    Optional<SysRoleEntity> findByRoleCode(String roleCode);
}