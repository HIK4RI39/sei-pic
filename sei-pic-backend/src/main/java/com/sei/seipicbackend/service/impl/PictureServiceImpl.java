package com.sei.seipicbackend.service.impl;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sei.seipicbackend.constant.UserConstant;
import com.sei.seipicbackend.exception.BusinessException;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.mapper.FileManager;
import com.sei.seipicbackend.model.dto.picture.*;
import com.sei.seipicbackend.model.enums.PictureReviewStatusEnum;
import com.sei.seipicbackend.model.pojo.Picture;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.PictureVO;
import com.sei.seipicbackend.model.vo.UserVO;
import com.sei.seipicbackend.service.PictureService;
import com.sei.seipicbackend.mapper.PictureMapper;
import com.sei.seipicbackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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
     * @param request
     * @return
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        // 校验登录, 需要填充上传者id
        UserVO loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginUser), ErrorCode.NOT_LOGIN_ERROR);

        // 如果存在pictureId, 说明是图片更新
        Long pictureId = pictureUploadRequest.getId();
        if (ObjUtil.isNotNull(pictureId)) {
            Picture picture = lambdaQuery().eq(Picture::getId, pictureId).one();
            ThrowUtils.throwIf(picture==null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 如果是编辑, 需要鉴权
            isOwnerOrAdmin(picture, loginUser);
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

        // 填写审核参数
        fillReviewParams(picture, loginUser);

        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");

        // 封装图片
        return getPictureVO(picture);
    }

    /**
     * pojo转vo, 关联查询用户信息
     * @param picture
     * @return
     */
    private PictureVO getPictureVO(Picture picture) {
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询脱敏用户信息
        Long userId = picture.getUserId();
        if (ObjUtil.isNotEmpty(userId) && userId>0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
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
        return getPictureVO(picture);
    }

    @Override
    public Page<PictureVO> getPictureVoPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 主页仅能查询过审数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        LambdaQueryWrapper<Picture> queryWrapper = getQueryWrapper(pictureQueryRequest);
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> page = new Page<>(current, pageSize);
        Page<Picture> picturePage = page(page, queryWrapper);
        return getPictureVoPage(picturePage);
    }

    @Override
    public Page<Picture> getPicturePage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        LambdaQueryWrapper<Picture> queryWrapper = getQueryWrapper(pictureQueryRequest);
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> page = new Page<>(current, pageSize);
        return page(page, queryWrapper);
    }

    @Override
    public boolean editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        Long pictureId = pictureEditRequest.getId();
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture==null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 鉴权
        UserVO loginUser = userService.getLoginUser(request);
        isOwnerOrAdmin(oldPicture, loginUser);

        Picture newPicture = new Picture();
        BeanUtil.copyProperties(pictureEditRequest, newPicture);
        newPicture.setEditTime(new Date());

        // tags需要转化为 json数组 字符串
        List<String> tags = pictureEditRequest.getTags();
        if (CollUtil.isNotEmpty(tags)) {
            newPicture.setTags(JSONUtil.toJsonStr(tags));
        }

        // 校验
        validPicture(newPicture);
        // 填充审核参数
        fillReviewParams(newPicture, loginUser);

        boolean result = updateById(newPicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();

        if (id==null || id<=0 || pictureReviewStatusEnum==null || PictureReviewStatusEnum.REVIEWING.equals(pictureReviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Picture oldPicture = getById(id);
        ThrowUtils.throwIf(oldPicture==null, ErrorCode.NOT_FOUND_ERROR);

        // 不能重复审核
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }

        Picture reviewedPicture = new Picture();
        BeanUtil.copyProperties(oldPicture, reviewedPicture);
        reviewedPicture.setReviewStatus(reviewStatus);
        reviewedPicture.setReviewMessage(reviewMessage);
        reviewedPicture.setReviewerId(loginUser.getId());
        reviewedPicture.setReviewTime(new Date());

        boolean result = this.updateById(reviewedPicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public boolean updatePicture(PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        Long pictureId = pictureUpdateRequest.getId();
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture==null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        Picture newPicture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, newPicture);
        // tags需要转化为 json数组 字符串
        List<String> tags = pictureUpdateRequest.getTags();
        if (CollUtil.isNotEmpty(tags)) {
            newPicture.setTags(JSONUtil.toJsonStr(tags));
        } 
        // 校验
        validPicture(newPicture);
        // 填充校验参数
        fillReviewParams(newPicture, userService.getLoginUser(request));

        boolean result = updateById(newPicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }


    /**
     * pojo Page转vo Page
     * @param picturePage
     * @return
     */
    private Page<PictureVO> getPictureVoPage(Page<Picture> picturePage) {
        List<Picture> pictureList = picturePage.getRecords();

        // 利用mp的Page转换工具，自动拷贝分页元数据
        Page<PictureVO> pictureVoPage = (Page<PictureVO>) picturePage.convert(PictureVO::objToVo);

        // 返回空数据
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVoPage;
        }

        // 获取用户id列表
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());

        // id:user Map
        Map<Long, UserVO> idUserVoMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));

        // 填充user信息
        pictureVoPage.getRecords().forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            if (idUserVoMap.containsKey(userId)) {
                pictureVO.setUser(idUserVoMap.get(userId));
            }
        });

        return pictureVoPage;
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

    /**
     * 将查询请求体转化为queryWrapper
     * @param pictureQueryRequest
     * @return
     */
    private LambdaQueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Long reviewerId = pictureQueryRequest.getReviewerId();

        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), Picture::getId, id);
        queryWrapper.like(StrUtil.isNotBlank(name), Picture::getName, name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), Picture::getIntroduction, introduction);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), Picture::getReviewMessage, reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), Picture::getPicSize, picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), Picture::getPicWidth, picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), Picture::getPicHeight, picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), Picture::getPicScale, picScale);
        queryWrapper.eq(StrUtil.isNotBlank(picFormat), Picture::getPicFormat, picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), Picture::getCategory, category);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), Picture::getUserId, userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), Picture::getReviewerId, reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), Picture::getReviewStatus, reviewStatus);


        // 后端代码改进
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                // 手动拼接引号，匹配 JSON 格式中的内容，例如 "%"水獭2"%"
                // 这样可以避免搜索 "Java" 匹配到 "JavaScript"
                String preciseTag = "\"" + tag + "\"";
                queryWrapper.like(Picture::getTags, preciseTag);
            }
        }

        // searchText可以是name或intro, 使用and拼接2个条件
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                .like(Picture::getName, searchText).or()
                .like(Picture::getIntroduction, searchText));
        }

        // 排序(写在最后)
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), "ascend".equals(sortOrder), getOrderColumn(sortField));

        return queryWrapper;
    }


    private static final Map<String, SFunction<Picture, ?>> COLUMN_MAP = new ConcurrentHashMap<>();
    static {
        COLUMN_MAP.put("id", Picture::getId);
        COLUMN_MAP.put("name", Picture::getName);
        COLUMN_MAP.put("picSize", Picture::getPicSize);
        COLUMN_MAP.put("picWidth", Picture::getPicWidth);
        COLUMN_MAP.put("picHeight", Picture::getPicHeight);
        COLUMN_MAP.put("picScale", Picture::getPicScale);
        COLUMN_MAP.put("createTime", Picture::getCreateTime);
        COLUMN_MAP.put("updateTime", Picture::getUpdateTime);
    }

    /**
     * 返回排序字段的方法引用
     * @param sortField
     * @return
     */
    private SFunction<Picture, ?> getOrderColumn(String sortField) {
        if (sortField==null) {
            return Picture::getCreateTime;
        }

        return COLUMN_MAP.getOrDefault(sortField, Picture::getCreateTime);
    }


    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /***
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    private void fillReviewParams(Picture picture, UserVO loginUser){
        // 管理员自动过审
        if (isAdmin(loginUser)) {
            picture.setReviewerId(loginUser.getId());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
        } else {
        // 非管理员, 创建或编辑的图片状态改为未审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

}




