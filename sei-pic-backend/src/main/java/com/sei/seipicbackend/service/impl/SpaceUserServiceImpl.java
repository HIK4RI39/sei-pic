package com.sei.seipicbackend.service.impl;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sei.seipicbackend.common.IdRequest;
import com.sei.seipicbackend.exception.BusinessException;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserAddRequest;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserConfirmRequest;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserEditRequest;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserQueryRequest;
import com.sei.seipicbackend.model.enums.SpaceRoleEnum;
import com.sei.seipicbackend.model.enums.SpaceUserStatusEnum;
import com.sei.seipicbackend.model.pojo.Space;
import com.sei.seipicbackend.model.pojo.SpaceUser;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.UserVO;
import com.sei.seipicbackend.model.vo.space.SpaceUserVO;
import com.sei.seipicbackend.model.vo.space.SpaceVO;
import com.sei.seipicbackend.service.SpaceService;
import com.sei.seipicbackend.service.SpaceUserService;
import com.sei.seipicbackend.mapper.SpaceUserMapper;
import com.sei.seipicbackend.service.UserService;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
* @author hikari39
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2026-04-03 04:35:40
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    // region -------------------------- 通用 --------------------------

    /**
     * 将SpaceUser列表转换为SpaceUserVO列表
     * @param spaceUserList SpaceUser实体列表
     * @return SpaceUserVO视图对象列表
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 判断输入列表是否为空，为空则返回空列表
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 对象列表 => 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
        // 1. 收集需要关联查询的用户 ID 和空间 ID
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 2. 批量查询用户和空间
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 3. 填充 SpaceUserVO 的用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 填充用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            // 填充空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }

    /**
     * 获取封装类
     * @param spaceUser
     * @param request
     * @return
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        // 对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    /**
     * 确认空间用户的方法
     * @param spaceUserConfirmRequest 包含用户ID和确认状态的请求对象
     * @param request HTTP请求对象
     * @return 返回空间ID
     */
    @Override
    public Boolean confirmSpaceUser(SpaceUserConfirmRequest spaceUserConfirmRequest, HttpServletRequest request) {
        Long id = spaceUserConfirmRequest.getId();
        Integer confirmStatus = spaceUserConfirmRequest.getConfirmStatus();
        ThrowUtils.throwIf(id == null || id<=0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(confirmStatus == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(confirmStatus==0, ErrorCode.PARAMS_ERROR, "请选择确认状态");
        SpaceUserStatusEnum enumByValue = SpaceUserStatusEnum.getEnumByValue(confirmStatus);
        ThrowUtils.throwIf(enumByValue == null, ErrorCode.PARAMS_ERROR, "确认状态不存在");

        SpaceUser spaceUser = this.getById(id);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR, "邀请不存在");
        // 如果不是待确认
        if (SpaceUserStatusEnum.CONFIRMING.getValue() != (spaceUser.getConfirmStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "错误的确认状态");
        }

        // 更新确认状态
        SpaceUser newSpaceUser = new SpaceUser();
        newSpaceUser.setId(id);
        newSpaceUser.setConfirmStatus(confirmStatus);
        // 如果是同意, 将权限设置为viewer
        if (SpaceUserStatusEnum.AGREED.getValue() == confirmStatus) {
            newSpaceUser.setSpaceRole(SpaceRoleEnum.VIEWER.getValue());
        }

        boolean result = this.updateById(newSpaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "确认失败");

        return true;
    }


    /**
     * 校验spaceUser
     * @param spaceUser
     * @param isAdd
     */
    private void validSpaceUser (SpaceUser spaceUser, boolean isAdd) {
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        String spaceRole = spaceUser.getSpaceRole();

        if (isAdd) {
            ThrowUtils.throwIf(spaceId == null || spaceId<=0, ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(userId == null || userId<=0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }

        SpaceRoleEnum enumByValue = SpaceRoleEnum.getEnumByValue(spaceRole);
        // 仅当传入spaceRole时校验
        ThrowUtils.throwIf(spaceRole!=null && enumByValue == null, ErrorCode.PARAMS_ERROR, "空间角色不存在");
    }


    /**
     * 构建查询wrapper
     * @param spaceUserQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }



    // endregion

    // region -------------------------- 用户 --------------------------

    // 添加空间用户
    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest==null, ErrorCode.PARAMS_ERROR);

        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddRequest, spaceUser);

        this.validSpaceUser(spaceUser, true);

        // 填写邀请人和邀请状态
        UserVO loginUser = userService.getLoginUser(request);
        spaceUser.setCreateUserId(loginUser.getId());
        spaceUser.setConfirmStatus(SpaceUserStatusEnum.CONFIRMING.getValue());

        boolean save = this.save(spaceUser);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "添加空间用户失败");
        return spaceUser.getId();
    }

    /**
     * 查询空间用户
     * @param spaceUserQueryRequest
     * @return
     */
    @Override
    public SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest) {
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);

        QueryWrapper<SpaceUser> queryWrapper = this.getQueryWrapper(spaceUserQueryRequest);
        SpaceUser spaceUser = this.getOne(queryWrapper);
        ThrowUtils.throwIf(spaceUser==null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        return spaceUser;
    }

    /**
     * 实现团队空间-成员信息列表
     * @param spaceUserQueryRequest
     * @return
     */
    @Override
    public List<SpaceUserVO> listSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest) {
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceId), ErrorCode.PARAMS_ERROR);
        QueryWrapper<SpaceUser> queryWrapper = this.getQueryWrapper(spaceUserQueryRequest);

        List<SpaceUser> list = this.list(queryWrapper);
        return getSpaceUserVOList(list);
    }

    /**
     * 编辑成员信息 (设置权限)
     * @param spaceUserEditRequest
     * @return
     */
    @Override
    public boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserEditRequest, spaceUser);
        this.validSpaceUser(spaceUser, false);

        Long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = this.getById(id);
        ThrowUtils.throwIf(oldSpaceUser==null, ErrorCode.NOT_FOUND_ERROR, "成员不存在");

        Long spaceId = oldSpaceUser.getSpaceId();
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space==null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 不能修改创建者权限
        Long userId = oldSpaceUser.getUserId();
        ThrowUtils.throwIf(userId.equals(space.getUserId()), ErrorCode.PARAMS_ERROR, "不能修改创建者权限");

        boolean result = this.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "编辑成员信息失败");
        return true;
    }

    /**
     * 查询用户加入的团队空间列表
     * @param request
     * @return
     */
    @Override
    public List<SpaceUserVO> listMyTeamSpace(HttpServletRequest request) {
        UserVO loginUser = userService.getLoginUser(request);
        List<SpaceUser> spaceUsers = this.lambdaQuery().eq(SpaceUser::getUserId, loginUser.getId()).list();
        if (CollUtil.isEmpty(spaceUsers)) {
            return Collections.emptyList();
        }

        return getSpaceUserVOList(spaceUsers);
    }

    /**
     * 退出团队空间
     * @param idRequest
     * @param request
     * @return
     */
    @Override
    public boolean quitSpaceUser(IdRequest idRequest, HttpServletRequest request) {
        long id = idRequest.getId();
        ThrowUtils.throwIf(id<=0, ErrorCode.PARAMS_ERROR);
        UserVO loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        SpaceUser spaceUser = this.lambdaQuery().eq(SpaceUser::getUserId, id)
                .eq(SpaceUser::getUserId, userId)
                .eq(SpaceUser::getConfirmStatus, SpaceUserStatusEnum.AGREED.getValue())
                .one();
        ThrowUtils.throwIf(spaceUser==null, ErrorCode.NOT_FOUND_ERROR, "成员不存在");
        // 创始人不可退出
        SpaceUserVO spaceUserVO = getSpaceUserVO(spaceUser, request);
        SpaceVO space = spaceUserVO.getSpace();
        Long spaceOwner = space.getUserId();
        ThrowUtils.throwIf(spaceOwner.equals(userId), ErrorCode.PARAMS_ERROR, "创始人不可退出");
        // 退出
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "退出失败");

        return true;
    }

    // endregion

}




