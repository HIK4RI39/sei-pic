package com.sei.seipicbackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sei.seipicbackend.constant.UserConstant;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.mapper.FileManager;
import com.sei.seipicbackend.model.dto.picture.PictureUploadRequest;
import com.sei.seipicbackend.model.dto.picture.UploadPictureResult;
import com.sei.seipicbackend.model.pojo.Picture;
import com.sei.seipicbackend.model.vo.PictureVO;
import com.sei.seipicbackend.model.vo.UserVO;
import com.sei.seipicbackend.service.PictureService;
import com.sei.seipicbackend.mapper.PictureMapper;
import com.sei.seipicbackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
* @author hikari39
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-03-29 03:39:44
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    /**
     * 上传图片
     * @param multipartFile
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, UserVO loginUser) {
        // 校验登录, 需要填充上传者id
        ThrowUtils.throwIf(ObjUtil.isNull(loginUser), ErrorCode.NOT_LOGIN_ERROR);

        // 如果存在pictureId, 说明是图片更新
        Long pictureId = pictureUploadRequest.getId();
        if (ObjUtil.isNotNull(pictureId)) {
            boolean exists = lambdaQuery().eq(Picture::getId, pictureId).exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        // 上传图片
        String uploadPathPrefix = String.format("public/%s", loginUser.getId()); 
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        // 从上传结果中获取图片信息, 并填充
        Picture picture = getPicture(loginUser, uploadPictureResult);

        // 如果是更新, 设置编辑时间和id
        if (ObjUtil.isNull(picture)) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        
        return PictureVO.objToVo(picture);
    }

    @Override
    public boolean deletePictureById(long pictureId, HttpServletRequest request) {
        UserVO loginUser = userService.getLoginUser(request);
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(ObjUtil.isNull(picture), ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!isOwnerOrAdmin(picture, loginUser), ErrorCode.NO_AUTH_ERROR);

        boolean result = this.removeById(pictureId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片删除失败");

        return true;
    }

    @Override
    public Picture getPictureById(long pictureId) {
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(ObjUtil.isNull(picture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return picture;
    }

    @Override
    public PictureVO getPictureVoById(long pictureId) {
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(ObjUtil.isNull(picture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return PictureVO.objToVo(picture);
    }

    /**
     * 管理员鉴权
     * @param loginUserVO
     * @return
     */
    private boolean isAdmin(UserVO loginUserVO) {
        return UserConstant.ADMIN_ROLE.equals(loginUserVO.getUserRole());
    }

    /**
     * 拥有者或管理员 鉴权
     * @param picture
     * @param loginUserVO
     * @return
     */
    private boolean isOwnerOrAdmin(Picture picture, UserVO loginUserVO) {
        Long owner = picture.getUserId();
        Long userId = loginUserVO.getId();
        return owner.equals(userId) || UserConstant.ADMIN_ROLE.equals(loginUserVO.getUserRole());
    }

    /**
     * 填充图片参数
     * @param loginUser
     * @param uploadPictureResult
     * @return
     */
    private static Picture getPicture(UserVO loginUser, UploadPictureResult uploadPictureResult) {
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        return picture;
    }


}




