package com.example.accountdemo.infrastructure.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA cho {@link UserJpaEntity}.
 *
 * <p>Quyền/role load bằng query riêng — tránh EntityGraph nhiều collection
 * (cartesian product) khiến chỉ còn 1 permission/role.
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByUsername(String username);

    /** Username đã tồn tại chưa. */
    boolean existsByUsername(String username);

    /** Email đã tồn tại chưa (đăng ký / validation). */
    boolean existsByEmail(String email);

    /** Account đã gắn với user nào chưa. */
    boolean existsByAccountId(String accountId);

    @Query("""
            select distinct r.name
            from UserJpaEntity u
            join u.roles r
            where u.username = :username
              and u.deleted = false
              and r.deleted = false
            order by r.name
            """)
    List<String> findRoleNamesByUsername(@Param("username") String username);

    @Query("""
            select distinct p.name
            from UserJpaEntity u
            join u.roles r
            join r.permissions p
            where u.username = :username
              and u.deleted = false
              and r.deleted = false
              and p.deleted = false
            order by p.name
            """)
    List<String> findPermissionNamesFromRoles(@Param("username") String username);

    @Query("""
            select distinct p.name
            from UserJpaEntity u
            join u.directPermissions p
            where u.username = :username
              and u.deleted = false
              and p.deleted = false
            order by p.name
            """)
    List<String> findDirectPermissionNames(@Param("username") String username);
}
