package com.sei.seipicbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserAddRequest;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserEditRequest;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserQueryRequest;
import com.sei.seipicbackend.model.pojo.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sei.seipicbackend.model.vo.space.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author hikari39
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2026-04-03 04:35:40
*/
public interface SpaceUserService extends IService<SpaceUser> {

    // region -------------------------- 通用 --------------------------

/**
 * 获取空间用户视图对象列表的方法
 * 该方法将空间用户实体列表转换为视图对象列表，通常用于前端展示
 *
 * @param spaceUserList 空间用户实体列表，包含用户的基本信息
 * @return 返回空间用户视图对象列表，视图对象可能包含前端需要展示的特定字段或格式化后的数据
 */
    // 获取封装类
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    // 构建查询wrapper
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    // 获取封装类
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    // endregion

    // region -------------------------- 用户 --------------------------

    // 新增空间用户
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    // 查询空间用户
    SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest);

    // 查询团队空间 成员信息列表
    List<SpaceUserVO> listSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest);

    // 编辑成员信息 (设置权限)
    boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest);

    // 查询用户加入的团队空间
    List<SpaceUserVO> listMyTeamSpace(HttpServletRequest request);

    // endregion

}
