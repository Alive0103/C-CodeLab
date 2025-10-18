package com.codelab.domain.repository;

import com.codelab.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    //实现大小写敏感用户查询
    @Query(value = "SELECT * FROM user u WHERE BINARY u.username = :username", nativeQuery = true)
    Optional<User> findByUsername(@Param("username") String username);


    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

}


