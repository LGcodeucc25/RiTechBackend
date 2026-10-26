package com.ritech.backend.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends JpaRepository<RoleEntity, Integer> {

    @Query("select r from RoleEntity r where lower(trim(r.name)) = lower(:name) order by r.id")
    List<RoleEntity> findAllByNameIgnoreCase(String name);

    default Optional<RoleEntity> findByNameIgnoreCase(String name) {
        return findAllByNameIgnoreCase(name).stream().findFirst();
    }
}
