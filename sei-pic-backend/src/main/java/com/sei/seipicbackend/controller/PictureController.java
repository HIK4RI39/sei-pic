package com.sei.seipicbackend.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sei.seipicbackend.annotation.AuthCheck;
import com.sei.seipicbackend.common.BaseResponse;
import com.sei.seipicbackend.common.IdRequest;
import com.sei.seipicbackend.common.ResponseUtils;
import com.sei.seipicbackend.constant.UserConstant;
import com.sei.seipicbackend.exception.BusinessException;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.model.dto.picture.*;
import com.sei.seipicbackend.model.pojo.Picture;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.PictureTagCategory;
import com.sei.seipicbackend.model.vo.PictureVO;
import com.sei.seipicbackend.model.vo.UserVO;
import com.sei.seipicbackend.service.PictureService;
import com.sei.seipicbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * @author hikari39_
 * @since 2026-03-29
 */
@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    // region -------------------------- 用户 --------------------------

    /**
     * 根据文件上传图片
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(
        @RequestPart MultipartFile multipartFile,
        PictureUploadRequest pictureUploadRequest, // 可以为空, 首次上传没有图片id
        HttpServletRequest request
    ) {
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, request);
        return ResponseUtils.success(pictureVO);
    }

    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, request);
        return ResponseUtils.success(pictureVO);
    }


    /**
     * 根据id删除图片
     * @param idRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePictureById(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        if (ObjUtil.isEmpty(idRequest) || idRequest.getId() <=0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long pictureId = idRequest.getId();
        boolean result = pictureService.deletePictureById(pictureId, request);
        return ResponseUtils.success(result);
    }

    /**
     * 用户 根据id获得pictureVo
     * @param idRequest
     * @return
     */
    @PostMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVoById(@RequestBody IdRequest idRequest) {
        if (ObjUtil.isEmpty(idRequest) || idRequest.getId() <=0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long pictureId = idRequest.getId();
        PictureVO pictureVO = pictureService.getPictureVoById(pictureId);
        return ResponseUtils.success(pictureVO);
    }

    /**
     * 用户 分页获取pictureVo
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/page/vo")
    public BaseResponse<Page<PictureVO>> getPictureVoPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pictureQueryRequest.getCurrent() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR);
        Page<PictureVO> pictureVoPage = pictureService.getPictureVoPage(pictureQueryRequest, request);
        return ResponseUtils.success(pictureVoPage);
    }

    /**
     * 用户 分页获取vo缓存
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/page/vo/cache")
    public BaseResponse<Page<PictureVO>> getPictureVoPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pictureQueryRequest.getCurrent() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR);
        Page<PictureVO> pictureVoPage = pictureService.getPictureVoPageWithCache(pictureQueryRequest, request);
        return ResponseUtils.success(pictureVoPage);
    }


    /**
     * 用户 更新图片
     * @param pictureEditRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureEditRequest), ErrorCode.PARAMS_ERROR);
        boolean update = pictureService.editPicture(pictureEditRequest, request);
        return ResponseUtils.success(update);
    }

    /**
     * 返回预设的tag,category列表
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意", "自然", "动物");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResponseUtils.success(pictureTagCategory);
    }


    // endregion

    // region -------------------------- 管理员 --------------------------

    /**
     * 管理员 根据id查询图片
     * @param idRequest
     * @return
     */
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(@RequestBody IdRequest idRequest) {
        if (ObjUtil.isEmpty(idRequest) || idRequest.getId() <=0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long pictureId = idRequest.getId();
        Picture picture = pictureService.getPictureById(pictureId);
        return ResponseUtils.success(picture);
    }

    /**
     * 管理员 分页获取图片数据
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> getPicturePage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pictureQueryRequest.getCurrent() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() <= 0, ErrorCode.PARAMS_ERROR);
        Page<Picture> picturePage = pictureService.getPicturePage(pictureQueryRequest, request);
        return ResponseUtils.success(picturePage);
    }

    /**
     * 管理员 更新图片
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureUpdateRequest), ErrorCode.PARAMS_ERROR);
        boolean update = pictureService.updatePicture(pictureUpdateRequest, request);
        return ResponseUtils.success(update);
    }

    /**
     * 管理员审核
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<?> reviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureReviewRequest), ErrorCode.PARAMS_ERROR);
        UserVO loginUser = userService.getLoginUser(request);
        User user = User.toBean(loginUser);
        pictureService.doPictureReview(pictureReviewRequest, user);
        return ResponseUtils.success();
    }

    /**
     * 管理员 批量审核通过
     * @param picReviewBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/review/batch/pass")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<?> reviewPicBatchPass(@RequestBody PicReviewBatchRequest picReviewBatchRequest, HttpServletRequest request) {
        if (ObjUtil.isEmpty(picReviewBatchRequest) || CollUtil.isEmpty(picReviewBatchRequest.getIdList())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        List<Long> idList = picReviewBatchRequest.getIdList();
        pictureService.reviewPicBatchPass(idList, request);
        return ResponseUtils.success();
    }

    /**
     * 管理员 批量删除
     * @return
     */
    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<?> deleteByBatch(@RequestBody List<String> idList) {
        if (CollUtil.isEmpty(idList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 删除COS的图片
        List<Picture> pictures = pictureService.listByIds(idList);
        for (Picture picture : pictures) {
            pictureService.clearPictureFile(picture);
        }
        // 删除数据库记录
        boolean result = pictureService.removeBatchByIds(idList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResponseUtils.success();
    }

    /**
     * 根据关键词, 批量拉取图片
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest==null, ErrorCode.PARAMS_ERROR);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, request);
        return ResponseUtils.success(uploadCount);
    }
    // endregion

}
