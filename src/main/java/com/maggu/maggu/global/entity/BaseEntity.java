package com.maggu.maggu.global.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Getter
@MappedSuperclass
public abstract class BaseEntity extends BaseCreatedAtEntity {

    @LastModifiedDate
    private Instant updatedAt;
}
