package com.chipin.chipin.repository;

import com.chipin.chipin.model.SettlementScheduleRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SettlementScheduleRunRepository extends JpaRepository<SettlementScheduleRun, Long> {
    List<SettlementScheduleRun> findByGroupIdOrderByIdDesc(String groupId);
}
