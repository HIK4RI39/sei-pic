package com.sei.seipicbackend.websocket.disruptor;

import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.websocket.model.PictureEditRequestMessage;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author hikari39_
 * @since 2026-04-03
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
