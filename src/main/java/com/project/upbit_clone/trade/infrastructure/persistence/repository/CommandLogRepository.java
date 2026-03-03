package com.project.upbit_clone.trade.infrastructure.persistence.repository;

import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface CommandLogRepository extends JpaRepository<CommandLog, Long> {

}
