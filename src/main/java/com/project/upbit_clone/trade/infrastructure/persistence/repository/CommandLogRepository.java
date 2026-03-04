package com.project.upbit_clone.trade.infrastructure.persistence.repository;

import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommandLogRepository extends JpaRepository<CommandLog, Long> {

    Optional<CommandLog> findByUserIdAndClientOrderIdAndCommandType(
            Long userId,
            String clientOrderId,
            CommandType commandType
    );
}
