package com.chipin.chipin.repository;

import com.chipin.chipin.model.CreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {
    List<CreditTransaction> findByUserIdOrderByIdDesc(String userId);
}