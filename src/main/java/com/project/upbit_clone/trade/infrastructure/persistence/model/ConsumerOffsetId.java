package com.project.upbit_clone.trade.infrastructure.persistence.model;

import com.project.upbit_clone.trade.infrastructure.persistence.vo.LogType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
// consumer offset 복합키
public class ConsumerOffsetId implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 16)
    private LogType logType;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "partition_key", nullable = false, length = 64)
    private String partitionKey;
}
