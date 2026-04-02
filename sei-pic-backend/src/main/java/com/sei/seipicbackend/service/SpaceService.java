package com.sei.seipicbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sei.seipicbackend.common.IdRequest;
import com.sei.seipicbackend.model.dto.picture.SpaceRankAnalyzeRequest;
import com.sei.seipicbackend.model.dto.space.SpaceAddRequest;
import com.sei.seipicbackend.model.dto.space.SpaceEditRequest;
import com.sei.seipicbackend.model.dto.space.SpaceQueryRequest;
import com.sei.seipicbackend.model.dto.space.SpaceUpdateRequest;
import com.sei.seipicbackend.model.dto.space.analyze.*;
import com.sei.seipicbackend.model.pojo.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sei.seipicbackend.model.vo.space.*;
import com.sei.seipicbackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author hikari39
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-03-31 01:00:14
*/
public interface SpaceService extends IService<Space> {

    // region -------------------------- 管理员 --------------------------

    // 获得用量topN空间
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest analyzeRequest);

    // 更新空间
    boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest);

    // 分页获取空间信息
    Page<SpaceVO> getSpacePage(SpaceQueryRequest spaceQueryRequest, HttpServletRequest request);

    // endregion

    // region -------------------------- 用户 --------------------------

    // 用户上传行为, 时间趋势分析
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest analyzeRequest, HttpServletRequest request);

    // 图片大小分段分析
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest analyzeRequest, HttpServletRequest request);

    // 标签分析
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest analyzeRequest, HttpServletRequest request);

    // 分类分析
    List<SpaceCategoryAnalyzeResponse> getCategoryAnalyzeResponse(SpaceCategoryAnalyzeRequest categoryAnalyzeRequest, HttpServletRequest request);

    // 空间用量分析
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest analyzeRequest, HttpServletRequest request);

    // 查询spaceVO with UserInfo
    SpaceVO getSpaceVoWithUser(Space space);

    // 创建空间
    Long createSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request);

    // 删除空间
    boolean deleteSpace(IdRequest idRequest, HttpServletRequest request);

    // 编辑空间信息
    boolean editSpace(SpaceEditRequest spaceEditRequest, HttpServletRequest request);

    // endregion

    // region -------------------------- 通用 --------------------------

    // 构建条件选择器
    LambdaQueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    // 获取封装类
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    // 校验空间权限
    void checkSpaceAuth(Long spaceId, UserVO loginUser);

    // 根据level填充空间信息
    void fillSpaceBySpaceLevel(Space space);

    // 检验空间信息
    void validSpace(Space space, boolean add);



    // endregion









}
