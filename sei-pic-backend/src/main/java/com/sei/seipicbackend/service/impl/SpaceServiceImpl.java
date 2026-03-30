package com.sei.seipicbackend.service.impl;
import java.util.Date;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.intern.InternUtil;
import cn.hutool.core.lang.intern.Interner;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sei.seipicbackend.common.IdRequest;
import com.sei.seipicbackend.constant.UserConstant;
import com.sei.seipicbackend.exception.BusinessException;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.model.dto.picture.PictureQueryRequest;
import com.sei.seipicbackend.model.dto.space.SpaceAddRequest;
import com.sei.seipicbackend.model.dto.space.SpaceEditRequest;
import com.sei.seipicbackend.model.dto.space.SpaceQueryRequest;
import com.sei.seipicbackend.model.dto.space.SpaceUpdateRequest;
import com.sei.seipicbackend.model.enums.SpaceLevelEnum;
import com.sei.seipicbackend.model.pojo.Picture;
import com.sei.seipicbackend.model.pojo.Space;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.PictureVO;
import com.sei.seipicbackend.model.vo.SpaceVO;
import com.sei.seipicbackend.model.vo.UserVO;
import com.sei.seipicbackend.service.PictureService;
import com.sei.seipicbackend.service.SpaceService;
import com.sei.seipicbackend.mapper.SpaceMapper;
import com.sei.seipicbackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author hikari39
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2026-03-31 01:00:14
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private PictureService pictureService;

    // 弱引用, 自动回收
    private static final Interner<String> USER_LOCK_INTERNER = InternUtil.createWeakInterner();

    // region -------------------------- 公共公共方法 --------------------------

    /**
     * pojo转vo, 关联查询用户信息
     * @param space
     * @return
     */
    private SpaceVO getSpaceVoWithUser(Space space) {
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询脱敏用户信息
        Long userId = space.getUserId();
        if (ObjUtil.isNotEmpty(userId) && userId>0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * pojo Page转vo Page
     * @param spacePage
     * @return
     */
    private Page<SpaceVO> convertToVoPage(Page<Space> spacePage) {
        List<Space> spaceList = spacePage.getRecords();

        // 利用mp的Page转换工具，自动拷贝分页元数据
        Page<SpaceVO> spaceVoPage = (Page<SpaceVO>) spacePage.convert(SpaceVO::objToVo);

        // 返回空数据
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVoPage;
        }

        // 获取用户id列表
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());

        // id:user Map
        Map<Long, UserVO> idUserVoMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));

        // 填充user信息
        spaceVoPage.getRecords().forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            if (idUserVoMap.containsKey(userId)) {
                spaceVO.setUser(idUserVoMap.get(userId));
            }
        });

        return spaceVoPage;
    }

    /**
     * 校验空间
     * 校验名称, 级别是否合法/存在
     *
     * @param space
     * @param add
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);

        // 创建
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf((spaceLevel==null), ErrorCode.PARAMS_ERROR, "空间级别不能为空");
        }

        // 更新/编辑
        ThrowUtils.throwIf(spaceLevel!=null && spaceLevelEnum==null, ErrorCode.PARAMS_ERROR, "空间级别不存在");
        ThrowUtils.throwIf(spaceName!=null && spaceName.length()>30, ErrorCode.PARAMS_ERROR, "空间名称过长");
        ThrowUtils.throwIf(spaceName!=null && StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空字符串");
    }

    /**
     * 创建或更新空间时, 根据空间级别自动填充限额等数据
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 鉴权, 所有者或管理员
     *
     * @param space
     * @param request
     * @return
     */
    @Override
    public boolean isOwnerOrAdmin(Space space, HttpServletRequest request) {
        Long userId = space.getUserId();
        UserVO loginUser = userService.getLoginUser(request);
        String userRole = loginUser.getUserRole();
        return userId.equals(loginUser.getId()) || UserConstant.ADMIN_ROLE.equals(userRole);
    }

    // endregion


    // region -------------------------- 管理员 --------------------------

    /**
     * 管理员 分页查询space
     * @param spaceQueryRequest
     * @param request
     * @return
     */
    @Override
    public Page<SpaceVO> getSpacePage(SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();

        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(id!=null, Space::getId, id)
                .eq(userId!=null, Space::getUserId, userId)
                .like(StrUtil.isNotBlank(spaceName), Space::getSpaceName, spaceLevel);

        Page<Space> page = new Page<>(current, pageSize);

        return convertToVoPage(page(page, queryWrapper));
    }

    /**
     * 管理员更新空间
     *
     * @param spaceUpdateRequest
     * @return
     */
    @Override
    public boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest) {
        Long id = spaceUpdateRequest.getId();
        String spaceName = spaceUpdateRequest.getSpaceName();
        Integer spaceLevel = spaceUpdateRequest.getSpaceLevel();
        Long maxSize = spaceUpdateRequest.getMaxSize();
        Long maxCount = spaceUpdateRequest.getMaxCount();

        Space space = this.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        // 可能存在1个问题: 传入spaceLevel, fillSpaceBySpaceLevel可能会覆盖自定义的count或level
        // 但是问题不大
        space.setSpaceName(spaceName);
        space.setSpaceLevel(spaceLevel);
        if (spaceLevel!=null) {
            fillSpaceBySpaceLevel(space);
        }
        if (maxCount!=null) {
            space.setMaxCount(maxCount);
        }
        if (maxSize!=null) {
            space.setMaxSize(maxSize);
        }
        validSpace(space, false);

        boolean result = updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新空间失败");
        return true;
    }

    // endregion

    // region -------------------------- 用户 --------------------------

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @Override
    public Long createSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        String spaceName = spaceAddRequest.getSpaceName();
        Integer spaceLevel = spaceAddRequest.getSpaceLevel();
        Space space = new Space();
        space.setSpaceName(spaceName);
        space.setSpaceLevel(spaceLevel);
        // 校验空间
        validSpace(space, true);
        // 用户仅能创建基础类型空间
        UserVO loginUser = userService.getLoginUser(request);
        String userRole = loginUser.getUserRole();
        if (UserConstant.DEFAULT_ROLE.equals(userRole) && SpaceLevelEnum.COMMON.getValue() != spaceLevel) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 填写空间参数
        fillSpaceBySpaceLevel(space);
        // 填写userId
        Long userId = loginUser.getId();
        space.setUserId(userId);

        // 加锁
        String lock = USER_LOCK_INTERNER.intern(userId.toString());
        synchronized (lock) {
            // 不能重复创建空间
            boolean exists = lambdaQuery().eq(Space::getUserId, userId).exists();
            ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "最多创建1个空间");
            boolean result = save(space);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "空间创建失败");
        }

        return Optional.ofNullable(space.getId()).orElse(-1L);
    }

    /**
     * 删除空间
     *
     * @param idRequest
     * @param request
     * @return
     */
    @Override
    public boolean deleteSpace(IdRequest idRequest, HttpServletRequest request) {
        // 空间是否存在
        long spaceId = idRequest.getId();
        Space space = this.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 仅限本人或管理员删除
        ThrowUtils.throwIf(!isOwnerOrAdmin(space, request), ErrorCode.NO_AUTH_ERROR);

        // 关联删除空间内所有图片
        List<Picture> pictureList = pictureService.lambdaQuery().eq(Picture::getSpaceId, spaceId).list();
        if (CollUtil.isNotEmpty(pictureList)) {
            // 删除COS存储图片
            for (Picture picture : pictureList) {
                pictureService.clearPictureFile(picture);
            }
            // 删除数据库记录
            Set<Long> pictureIds = pictureList.stream().map(Picture::getId).collect(Collectors.toSet());
            boolean deletePicture = pictureService.removeBatchByIds(pictureIds);
            ThrowUtils.throwIf(!deletePicture, ErrorCode.OPERATION_ERROR, "关联删除图片失败");
        }

        boolean result = removeById(spaceId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "空间删除失败");
        return true;
    }

    /**
     * 编辑空间
     * @param spaceEditRequest
     * @param request
     * @return
     */
    @Override
    public boolean editSpace(SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        Long id = spaceEditRequest.getId();
        String spaceName = spaceEditRequest.getSpaceName();
        Space space = this.getById(id);
        ThrowUtils.throwIf(space==null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        ThrowUtils.throwIf(!isOwnerOrAdmin(space, request), ErrorCode.NO_AUTH_ERROR);

        space.setId(id);
        space.setSpaceName(spaceName);
        validSpace(space, false);

        boolean result = updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "编辑空间失败");
        return true;
    }
    // endregion


}




