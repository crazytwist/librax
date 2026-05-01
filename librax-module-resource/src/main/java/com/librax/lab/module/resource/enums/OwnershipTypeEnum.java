package com.librax.lab.module.resource.enums;

import lombok.Getter;

@Getter
public enum OwnershipTypeEnum {

    EXCLUSIVE("独占"),
    SHARED("共享");

    private final String desc;

    OwnershipTypeEnum(String desc) {
        this.desc = desc;
    }
}