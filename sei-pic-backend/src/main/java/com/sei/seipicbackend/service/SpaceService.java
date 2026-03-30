package com.sei.seipicbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sei.seipicbackend.common.IdRequest;
import com.sei.seipicbackend.model.dto.space.SpaceAddRequest;
import com.sei.seipicbackend.model.dto.space.SpaceEditRequest;
import com.sei.seipicbackend.model.dto.space.SpaceQueryRequest;
import com.sei.seipicbackend.model.dto.space.SpaceUpdateRequest;
import com.sei.seipicbackend.model.pojo.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sei.seipicbackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author hikari39
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-03-31 01:00:14
*/
public interface SpaceService extends IService<Space> {

    SpaceVO getSpaceVoWithUser(Space space);

    void validSpace(Space space, boolean add);

    void fillSpaceBySpaceLevel(Space space);

    Long createSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request);

    boolean deleteSpace(IdRequest idRequest, HttpServletRequest request);

    boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest);

    boolean editSpace(SpaceEditRequest spaceEditRequest, HttpServletRequest request);

    boolean isOwnerOrAdmin(Space space, HttpServletRequest request);

    Page<SpaceVO> getSpacePage(SpaceQueryRequest spaceQueryRequest, HttpServletRequest request);
}
