package com.sei.seipicbackend.service.impl;
import java.util.Date;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.intern.InternUtil;
import cn.hutool.core.lang.intern.Interner;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.sei.seipicbackend.model.dto.space.analyze.SpaceAnalyzeRequest;
import com.sei.seipicbackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.sei.seipicbackend.model.enums.SpaceLevelEnum;
import com.sei.seipicbackend.model.pojo.Picture;
import com.sei.seipicbackend.model.pojo.Space;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.PictureVO;
import com.sei.seipicbackend.model.vo.SpaceUsageAnalyzeResponse;
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

    // region -------------------------- 通用 --------------------------

    /**
     * 校验空间权限
     * @param spaceId
     * @param loginUser
     */
    @Override
    public void checkSpaceAuth(Long spaceId, UserVO loginUser) {
        ThrowUtils.throwIf(spaceId==null || spaceId<=0, ErrorCode.PARAMS_ERROR, "非法的空间ID");
        Space space = this.getById(spaceId);
        ThrowUtils.throwIf(space==null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        Long userId = space.getUserId();
        String userRole = loginUser.getUserRole();
        if (!userId.equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(userRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有访问该空间的权限");
        }
    }

    /**
     * 填写分析条件
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, LambdaQueryWrapper<Picture> queryWrapper) {
        // 全图库, 无需填写查询条件
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        // 公共图库, spaceId为null
        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull(Picture::getSpaceId);
            return;
        }
        // 个人图库, 设置spaceId
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq(Picture::getSpaceId, spaceId);
            return;
        }
        // 查询条件为空
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }


    /**
     * 校验空间分析权限
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, UserVO loginUser) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        ThrowUtils.throwIf(queryAll&&queryPublic, ErrorCode.PARAMS_ERROR, "分析范围冲突");

        // 查个人空间, 仅有本人或管理员
        if (spaceId!=null) {
            checkSpaceAuth(spaceId, loginUser);
        }

        // 查公共图库/全部图库, 仅有管理员
        if (queryAll || queryPublic) {
            userService.checkAdmin(loginUser);
        }
    }

    /**
     * pojo转vo, 关联查询用户信息
     * @param space
     * @return
     */
    @Override
    public SpaceVO getSpaceVoWithUser(Space space) {
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
     * 空间用量分析
     * @param analyzeRequest
     * @param request
     * @return
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest analyzeRequest, HttpServletRequest request) {
        Long spaceId = analyzeRequest.getSpaceId();
        boolean isQueryPublic = analyzeRequest.isQueryPublic();
        boolean isQueryAll = analyzeRequest.isQueryAll();

        // 校验查询参数
        UserVO loginUser = userService.getLoginUser(request);
        checkSpaceAnalyzeAuth(analyzeRequest, loginUser);

        // 填写查询条件
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        fillAnalyzeQueryWrapper(analyzeRequest,queryWrapper);
        // 只需查询此列
        queryWrapper.select(Picture::getPicSize);

        // 查询用量
        // 使用baseMapper不用封装为Picture对象, 提高性能节约空间
        List<Object> pictureObject = pictureService.getBaseMapper().selectObjs(queryWrapper);
        long usedSize = pictureObject.stream().mapToLong(pic -> pic instanceof Long ? (Long) pic : 0).sum();
        long usedCount = pictureObject.size();

        SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();

        // 公共空间 或 全空间
        if (isQueryAll || isQueryPublic) {
            response.setUsedSize(usedSize);
            response.setMaxSize(null);
            response.setSizeUsageRatio(null);
            response.setUsedCount(usedCount);
            response.setMaxCount(null);
            response.setCountUsageRatio(null);
        // 个人空间
        } else {
            Space space = this.getById(spaceId);
            Long maxSize = space.getMaxSize();
            Long maxCount = space.getMaxCount();
            Long totalSize = space.getTotalSize();
            Long totalCount = space.getTotalCount();
            double sizeUsageRatio = NumberUtil.round((totalSize * 100.0 / maxSize), 2).doubleValue();
            double countUsageRatio = NumberUtil.round((totalCount * 100.0 / maxCount), 2).doubleValue();
            response.setUsedSize(totalSize);
            response.setMaxSize(maxSize);
            response.setSizeUsageRatio(sizeUsageRatio);
            response.setUsedCount(totalCount);
            response.setMaxCount(maxCount);
            response.setCountUsageRatio(countUsageRatio);
        }

        return response;
    }


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
        // 鉴权
        long spaceId = idRequest.getId();
        UserVO loginUser = userService.getLoginUser(request);
        checkSpaceAuth(spaceId, loginUser);

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
        String spaceName = spaceEditRequest.getSpaceName();
        ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");

        // 鉴权
        Long spaceId = spaceEditRequest.getId();
        UserVO loginUser = userService.getLoginUser(request);
        checkSpaceAuth(spaceId, loginUser);

        // 编辑
        Space space = this.getById(spaceId);
        space.setId(spaceId);
        space.setSpaceName(spaceName);
        validSpace(space, false);

        boolean result = updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "编辑空间失败");
        return true;
    }
    // endregion


}




