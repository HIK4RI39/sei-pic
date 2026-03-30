package com.sei.seipicbackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author hikari39_
 * @since 2026-03-31
 */
@Data
@AllArgsConstructor
public class SpaceLevel {

    private int value;

    private String text;

    private long maxCount;

    private long maxSize;
}
