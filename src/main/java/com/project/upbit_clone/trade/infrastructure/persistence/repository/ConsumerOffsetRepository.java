package com.project.upbit_clone.trade.infrastructure.persistence.repository;

import com.project.upbit_clone.trade.infrastructure.persistence.model.ConsumerOffset;
import com.project.upbit_clone.trade.infrastructure.persistence.model.ConsumerOffsetId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsumerOffsetRepository extends JpaRepository<ConsumerOffset, ConsumerOffsetId> {
}
