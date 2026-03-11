package com.auctiondemo.backend.repository;

import com.auctiondemo.backend.entity.UserCredit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCreditRepository extends JpaRepository<UserCredit, Long> {
}
