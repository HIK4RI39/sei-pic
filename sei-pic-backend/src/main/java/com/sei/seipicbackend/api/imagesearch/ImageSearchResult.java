package com.sei.seipicbackend.api.imagesearch;

import lombok.Data;

/**
 * @author hikari39_
 * @since 2026-04-01
 */
@Data
public class ImageSearchResult {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;
}
