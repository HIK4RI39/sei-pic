package com.sei.seipicbackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author hikari39_
 * @since 2026-03-27
 */
@Data
public class IdRequest implements Serializable {
    /**
     * id
     */
    private long id;

    private static final long serialVersionUID = 1L;
}
