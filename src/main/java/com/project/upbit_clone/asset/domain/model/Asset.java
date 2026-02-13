package com.project.upbit_clone.asset.domain.model;

import com.project.upbit_clone.global.domain.model.BaseEntity;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "asset")
public class Asset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long id;

    @Column(name = "symbol", nullable = false, length = 10)
    private String symbol;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "decimals", nullable = false)
    private Byte decimals;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EnumStatus status = EnumStatus.ACTIVE;
}
