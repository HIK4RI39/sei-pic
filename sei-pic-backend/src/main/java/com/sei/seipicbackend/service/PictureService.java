package com.sei.seipicbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sei.seipicbackend.model.dto.picture.*;
import com.sei.seipicbackend.model.pojo.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author hikari39
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-03-29 03:39:44
*/
public interface PictureService extends IService<Picture> {

    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest request);

    boolean deletePictureById(long pictureId, HttpServletRequest request);

    Picture getPictureById(long pictureId);

    PictureVO getPictureVoById(long pictureId);

    Page<PictureVO> getPictureVoPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    Page<Picture> getPicturePage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    boolean updatePicture(PictureUpdateRequest pictureUpdateRequest);

    boolean editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request);

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

}
