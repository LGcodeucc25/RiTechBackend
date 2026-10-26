package com.ritech.backend.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Integer> {

    /** Email is not unique in the database, so the application treats the oldest match as the account. */
    @Query("select u from User u where lower(u.email) = lower(:email) order by u.id")
    List<User> findAllByEmailIgnoreCase(String email);

    default Optional<User> findByEmailIgnoreCase(String email) {
        return findAllByEmailIgnoreCase(email.trim()).stream().findFirst();
    }
}
