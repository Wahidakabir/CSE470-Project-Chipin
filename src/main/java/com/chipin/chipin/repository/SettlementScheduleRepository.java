package com.chipin.chipin.repository;

import com.chipin.chipin.model.SettlementSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SettlementScheduleRepository extends JpaRepository<SettlementSchedule, Long> {
    List<SettlementSchedule> findByGroupIdOrderByIdDesc(String groupId);
    List<SettlementSchedule> findByActive(boolean active);
}
