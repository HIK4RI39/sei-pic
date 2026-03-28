package com.sei.seipicbackend.service;

import com.sei.seipicbackend.model.dto.picture.PictureUploadRequest;
import com.sei.seipicbackend.model.pojo.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sei.seipicbackend.model.vo.PictureVO;
import com.sei.seipicbackend.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author hikari39
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-03-29 03:39:44
*/
public interface PictureService extends IService<Picture> {

    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, UserVO loginUser);

    boolean deletePictureById(long pictureId, HttpServletRequest request);

    Picture getPictureById(long pictureId);

    PictureVO getPictureVoById(long pictureId);
}
