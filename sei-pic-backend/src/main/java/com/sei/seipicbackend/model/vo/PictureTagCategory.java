package com.sei.seipicbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author hikari39_
 * @since 2026-03-29
 */
@Data
public class PictureTagCategory implements Serializable {
    private List<String> tagList;
    private List<String> categoryList;
    private static final long serialVersionUID = 1L;
}
