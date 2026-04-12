package com.project.upbit_clone.trade.infrastructure.persistence.repository;

import com.project.upbit_clone.trade.infrastructure.persistence.model.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    List<EventLog> findByMarketIdAndIdGreaterThanOrderByIdAsc(Long marketId, Long eventLogId);
}
