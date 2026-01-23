package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.document.UserView;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserViewRepository extends MongoRepository<UserView, Integer> {
    Optional<UserView> findByUsername(String username);
    Optional<UserView> findByEmail(String email);
}
