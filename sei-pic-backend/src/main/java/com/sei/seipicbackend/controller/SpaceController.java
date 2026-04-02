package com.sei.seipicbackend.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sei.seipicbackend.annotation.AuthCheck;
import com.sei.seipicbackend.common.BaseResponse;
import com.sei.seipicbackend.common.IdRequest;
import com.sei.seipicbackend.common.ResponseUtils;
import com.sei.seipicbackend.constant.UserConstant;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.model.dto.picture.SpaceRankAnalyzeRequest;
import com.sei.seipicbackend.model.dto.space.SpaceAddRequest;
import com.sei.seipicbackend.model.dto.space.SpaceEditRequest;
import com.sei.seipicbackend.model.dto.space.SpaceQueryRequest;
import com.sei.seipicbackend.model.dto.space.SpaceUpdateRequest;
import com.sei.seipicbackend.model.dto.space.analyze.*;
import com.sei.seipicbackend.model.enums.SpaceLevelEnum;
import com.sei.seipicbackend.model.pojo.Space;
import com.sei.seipicbackend.model.vo.*;
import com.sei.seipicbackend.model.vo.space.*;
import com.sei.seipicbackend.service.SpaceService;
import com.sei.seipicbackend.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author hikari39_
 * @since 2026-03-31
 */
@RestController
@RequestMapping("/space")
public class SpaceController {
    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    // region -------------------------- 管理员 --------------------------

    /**
     * 返回用量topN的空间
     * @param analyzeRequest
     * @return
     */
    @PostMapping("/analyze/rank")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<Space>> getSpaceRankAnalyze(SpaceRankAnalyzeRequest analyzeRequest) {
        Integer topN = analyzeRequest.getTopN();
        ThrowUtils.throwIf(topN==null || topN<=0, ErrorCode.PARAMS_ERROR);
        List<Space> spaceList = spaceService.getSpaceRankAnalyze(analyzeRequest);
        return ResponseUtils.success(spaceList);
    }

    /**
     * 管理员 分页获取空间
     * @param spaceQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SpaceVO>> getSpacePage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceQueryRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceQueryRequest.getCurrent() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceQueryRequest.getPageSize() <= 0, ErrorCode.PARAMS_ERROR);
        Page<SpaceVO> spacePage = spaceService.getSpacePage(spaceQueryRequest, request);
        return ResponseUtils.success(spacePage);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUpdateRequest), ErrorCode.PARAMS_ERROR);
        boolean result = spaceService.updateSpace(spaceUpdateRequest);
        return ResponseUtils.success(result);
    }

    // endregion

    // region -------------------------- 用户 --------------------------

    /**
     * 用户上传行为, 时间趋势分析
     * @param analyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/analyze/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequest analyzeRequest, HttpServletRequest request) {
        List<SpaceUserAnalyzeResponse> responseList = spaceService.getSpaceUserAnalyze(analyzeRequest, request);
        return ResponseUtils.success(responseList);
    }

    /**
     * 根据图片大小对图库进行分段分析
     * @param analyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/analyze/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeRequest analyzeRequest, HttpServletRequest request) {
        List<SpaceSizeAnalyzeResponse> responseList = spaceService.getSpaceSizeAnalyze(analyzeRequest, request);
        return ResponseUtils.success(responseList);
    }



    @PostMapping("/analyze/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeRequest analyzeRequest, HttpServletRequest request) {
        List<SpaceTagAnalyzeResponse> responseList = spaceService.getSpaceTagAnalyze(analyzeRequest, request);
        return ResponseUtils.success(responseList);
    }

    /**
     * 图库分类分析
     * @param categoryAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/analyze/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeRequest categoryAnalyzeRequest, HttpServletRequest request) {
        List<SpaceCategoryAnalyzeResponse> responseList = spaceService.getCategoryAnalyzeResponse(categoryAnalyzeRequest, request);
        return ResponseUtils.success(responseList);
    }

    /**
     * 图库用量分析
     * @param analyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/analyze/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(
            @RequestBody SpaceUsageAnalyzeRequest analyzeRequest,
            HttpServletRequest request
    ) {
        SpaceUsageAnalyzeResponse analyzeResponse = spaceService.getSpaceUsageAnalyze(analyzeRequest, request);
        return ResponseUtils.success(analyzeResponse);
    }



    /**
     * 获取当前用户的vo
     * @return
     */
    @PostMapping("/vo")
    public BaseResponse<SpaceVO> getSpaceVo(HttpServletRequest request) {
        UserVO loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        SpaceVO spaceVO = null;
        Space space = spaceService.lambdaQuery().eq(Space::getUserId, userId).one();
        if (space!=null) {
            spaceVO = spaceService.getSpaceVoWithUser(space);
        }
        return ResponseUtils.success(spaceVO);
    }

    /**
     * 获取spaceLevel列表
     * @return
     */
    @PostMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()).map(spaceLevelEnum ->
                new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                )
        ).collect(Collectors.toList());
        return ResponseUtils.success(spaceLevelList);
    }

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/create")
    public BaseResponse<Long> createSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceAddRequest), ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceService.createSpace(spaceAddRequest, request);
        return ResponseUtils.success(spaceId);
    }

    /**
     * 删除空间
     * @param idRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(idRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(idRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        boolean result = spaceService.deleteSpace(idRequest, request);
        return ResponseUtils.success(result);
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceEditRequest), ErrorCode.PARAMS_ERROR);
        Long id = spaceEditRequest.getId();
        String spaceName = spaceEditRequest.getSpaceName();
        ThrowUtils.throwIf(id==null, ErrorCode.PARAMS_ERROR, "空间id不能为空");
        ThrowUtils.throwIf(id<=0, ErrorCode.PARAMS_ERROR, "非法的空间id");
        ThrowUtils.throwIf(spaceName==null || StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
        boolean result = spaceService.editSpace(spaceEditRequest, request);
        return ResponseUtils.success(result);
    }

    // endregion



}
