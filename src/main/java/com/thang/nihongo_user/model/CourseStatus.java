package com.thang.nihongo_user.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum CourseStatus {

    ACTIVE(0, "Đang hoạt động"),
    FULL(1, "Đã đủ người"),
    INACTIVE(2, "Tạm ngưng hoạt động");

    private final int code;
    private final String description;

    CourseStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CourseStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (CourseStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }

        throw new IllegalArgumentException(
                "Invalid course status: " + code
        );
    }
}
