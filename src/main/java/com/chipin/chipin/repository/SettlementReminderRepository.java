package com.chipin.chipin.repository;

import com.chipin.chipin.model.SettlementReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SettlementReminderRepository extends JpaRepository<SettlementReminder, Long> {
    List<SettlementReminder> findByGroupIdAndDone(String groupId, boolean done);
    List<SettlementReminder> findByScheduleIdAndDone(Long scheduleId, boolean done);
}
