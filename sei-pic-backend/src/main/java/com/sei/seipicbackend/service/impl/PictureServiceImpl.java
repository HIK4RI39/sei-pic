package com.sei.seipicbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sei.seipicbackend.constant.UserConstant;
import com.sei.seipicbackend.exception.BusinessException;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.manager.CosManager;
import com.sei.seipicbackend.manager.upload.FilePictureUpload;
import com.sei.seipicbackend.manager.upload.PictureUploadTemplate;
import com.sei.seipicbackend.manager.upload.UrlPictureUpload;
import com.sei.seipicbackend.mapper.PictureMapper;
import com.sei.seipicbackend.model.dto.picture.*;
import com.sei.seipicbackend.model.enums.PictureReviewStatusEnum;
import com.sei.seipicbackend.model.pojo.Picture;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.PictureVO;
import com.sei.seipicbackend.model.vo.UserVO;
import com.sei.seipicbackend.service.PictureService;
import com.sei.seipicbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author hikari39
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-03-29 03:39:44
*/
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CosManager cosManager;

//    @Resource
//    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    /**
     * 上传图片
     * @param inputSource multipartFile或fileUrl
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(inputSource==null, ErrorCode.NOT_FOUND_ERROR);

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
            // 更新图片后, 对原图片进行删除
            this.clearPictureFile(picture);
        }

        // 上传图片
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());

        // 根据inputSource决定上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);


        // 从上传结果中获取图片信息, 并填充
        Picture picture = getPicture(loginUser, uploadPictureResult);

        // 如果有category或tags信息, 填充
        String category = pictureUploadRequest.getCategory();
        if (StrUtil.isNotBlank(category)) {
            picture.setCategory(category);
        }

        // tags需要转化为 json数组 字符串
        List<String> tags = pictureUploadRequest.getTags();
        if (CollUtil.isNotEmpty(tags)) {
            picture.setTags(JSONUtil.toJsonStr(tags));
        }

        String picName = pictureUploadRequest.getPicName();
        if (StrUtil.isNotBlank(picName)) {
            picture.setName(picName);
        }

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
        // 异步删除COS的图片
        this.clearPictureFile(picture);

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

        // 缓存到redis
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String rediskey = "seipic:getPictureVoPage:" + hashKey;
        String cachedPage = JSONUtil.toJsonStr(picturePage);
        // 随机设置5~10分钟过期时间, 防止雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        stringRedisTemplate.opsForValue().set(rediskey, cachedPage, cacheExpireTime, TimeUnit.SECONDS);

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
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
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

    @Override
    public int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }

        // 格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
//        Elements imgElementList = div.select("img.mimg");
        Elements imgElementList = div.select(".iusc");  // 修改选择器，获取包含完整数据的元素


        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
//            String fileUrl = imgElement.attr("src");
            // 获取data-m属性中的JSON字符串
            String dataM = imgElement.attr("m");
            String fileUrl;
            try {
                // 解析JSON字符串
                JSONObject jsonObject = JSONUtil.parseObj(dataM);
                // 获取murl字段（原始图片URL）
                fileUrl = jsonObject.getStr("murl");
            } catch (Exception e) {
                log.error("解析图片数据失败", e);
                continue;
            }

            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            // 取出category和tags信息(如果有)
            String category = pictureUploadByBatchRequest.getCategory();
            List<String> tags = pictureUploadByBatchRequest.getTags();
            if (StrUtil.isNotBlank(category)) {
                pictureUploadRequest.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                pictureUploadRequest.setTags(tags);
            }

            if (StrUtil.isNotBlank(namePrefix)) {
                pictureUploadRequest.setPicName(namePrefix + (uploadCount+1));
            }

            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, request);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    /**
     * 管理员 批量审核通过
     * @param idList
     * @param request
     */
    @Override
    public void reviewPicBatchPass(List<Long> idList, HttpServletRequest request) {
        UserVO loginUser = userService.getLoginUser(request);
        Long id = loginUser.getId();

        List<Picture> pictures = this.listByIds(idList);
        ThrowUtils.throwIf(CollUtil.isEmpty(pictures), ErrorCode.PARAMS_ERROR);

        // 如果批量, 是否还需要校验重复审核
        for (Picture picture : pictures) {
            picture.setReviewTime(new Date());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员审核通过");
            picture.setReviewerId(id);
        }

        boolean result = updateBatchById(pictures);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 分页获取vo with cache
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVoPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 缓存key, 将查询条件作为key一并缓存, 可能会较长
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        // md5压缩key
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String rediskey = "seipic:getPictureVoPage:" + hashKey;

        // 从缓存中查询
        String cachedValue = stringRedisTemplate.opsForValue().get(rediskey);
        // 如果缓存命中, 直接返回结果
        if (cachedValue != null) {
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return cachedPage;
        }

        return this.getPictureVoPage(pictureQueryRequest, request);
    }

    /**
     * 异步清理对象存储中不被使用的图片
     * 当用户更新图片, 或图片被删除后, 清理COS中的图片
     * @param oldPicture
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用 (如果是秒传场景, 就有可能多个地址被1个图片使用)
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }

        String compressedUrl = oldPicture.getUrl();

        // 清理原图 (如果原图是webp无需清理)
        if (!"webp".equals(oldPicture.getPicFormat())) {
            int lastIndexOf = compressedUrl.lastIndexOf(".");
            // 有个坑, 文件后缀名转小写才能访问正确url
            String picFormat = oldPicture.getPicFormat().toLowerCase();
            String originUrl = compressedUrl.substring(0, lastIndexOf+1) + picFormat;
            cosManager.deleteObject(originUrl);
        }

        // 清理压缩图 (webp)
        cosManager.deleteObject(compressedUrl);

        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }


}




