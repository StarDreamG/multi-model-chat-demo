package com.afatguy.multimodelchat.persistence.repo;

import com.afatguy.multimodelchat.persistence.entity.SysUserRoleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SysUserRoleRepository extends JpaRepository<SysUserRoleEntity, Long> {

    @Query("select r.roleCode from SysUserRoleEntity ur join SysRoleEntity r on ur.roleId = r.id where ur.userId = :userId")
    List<String> findRoleCodesByUserId(Long userId);

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}