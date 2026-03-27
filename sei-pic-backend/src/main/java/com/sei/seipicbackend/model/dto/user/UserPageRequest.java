package com.sei.seipicbackend.model.dto.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.sei.seipicbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author hikari39_
 * @since 2026-03-28
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserPageRequest extends PageRequest {
    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

//    /**
//     * 编辑时间
//     */
//    private Date editTime;

    /**
     * 创建时间-起始
     */
    private Date createBeginTime;

    /**
     * 创建时间-结束
     */
    private Date createEndTime;

//    /**
//     * 更新时间
//     */
//    private Date updateTime;
}
