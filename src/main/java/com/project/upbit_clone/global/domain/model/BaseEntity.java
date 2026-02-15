package com.project.upbit_clone.global.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public class BaseEntity {

    // TODO: DB의 시간으로 통일을 위해 @CreateDate대신 DDL로 timestamp(3) 적용 예정.
    @Column(name="created_at", insertable=false, updatable=false, nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", insertable=false, updatable=false, nullable=false)
    private LocalDateTime updatedAt;
}
