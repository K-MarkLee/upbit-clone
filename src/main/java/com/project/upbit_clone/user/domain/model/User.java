package com.project.upbit_clone.user.domain.model;

import com.project.upbit_clone.global.domain.model.BaseEntity;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    // unique는 ddl 에서 uk_users_email
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EnumStatus status = EnumStatus.ACTIVE;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    public static User create(String email, String userName, EnumStatus status, String passwordHash) {
        return new User(email, userName, status, passwordHash);
    }

    private User(String email, String userName, EnumStatus status, String passwordHash) {
        this.email = email;
        this.userName = userName;
        this.status = (status == null) ? EnumStatus.ACTIVE : status;
        this.passwordHash = passwordHash;
    }
}
