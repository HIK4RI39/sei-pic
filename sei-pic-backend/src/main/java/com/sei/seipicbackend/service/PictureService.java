package com.sei.seipicbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sei.seipicbackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.sei.seipicbackend.model.dto.picture.*;
import com.sei.seipicbackend.model.pojo.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author hikari39
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2026-03-29 03:39:44
 */
public interface PictureService extends IService<Picture> {
    // region 管理员

    // 根据id获取图片
    Picture getPictureById(long pictureId);

    // 分页获取图片
    Page<Picture> getPicturePage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    // 更新图片
    boolean updatePicture(PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request);

    // 审核图片
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    // 批量上传图片
    int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request);

    // 批量审核通过
    void reviewPicBatchPass(List<Long> idList, HttpServletRequest request);

    // endregion

    // region 用户

    // 创建扩图任务
    CreateOutPaintingTaskResponse createOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, HttpServletRequest request);

    // 批量编辑图片
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request);

    // 上传图片
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, HttpServletRequest request);

    // 删除图片
    boolean deletePictureById(long pictureId, HttpServletRequest request);

    // 获取图片vo
    PictureVO getPictureVoById(long pictureId, HttpServletRequest request);

    // 分页获取图片vo
    Page<PictureVO> getPictureVoPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    // 分页获取图片vo (带有缓存)
    Page<PictureVO> getPictureVoPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    // 编辑图片
    boolean editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request);

    // 根据主色调查询图库中的图片
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, HttpServletRequest request);

    // endregion

    // region 通用

    // 清理COS存储中的图片
    public void clearPictureFile(Picture oldPicture);

    // endregion
}
