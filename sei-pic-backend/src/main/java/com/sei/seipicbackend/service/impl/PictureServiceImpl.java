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
import com.sei.seipicbackend.api.aliyunai.AliYunAiApi;
import com.sei.seipicbackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.sei.seipicbackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.sei.seipicbackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.sei.seipicbackend.constant.UserConstant;
import com.sei.seipicbackend.exception.BusinessException;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.manager.CosManager;
import com.sei.seipicbackend.manager.auth.SpaceUserAuthManager;
import com.sei.seipicbackend.manager.auth.StpKit;
import com.sei.seipicbackend.manager.auth.model.SpaceUserPermissionConstant;
import com.sei.seipicbackend.manager.upload.FilePictureUpload;
import com.sei.seipicbackend.manager.upload.PictureUploadTemplate;
import com.sei.seipicbackend.manager.upload.UrlPictureUpload;
import com.sei.seipicbackend.mapper.PictureMapper;
import com.sei.seipicbackend.model.dto.picture.*;
import com.sei.seipicbackend.model.enums.PictureReviewStatusEnum;
import com.sei.seipicbackend.model.pojo.Picture;
import com.sei.seipicbackend.model.pojo.Space;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.PictureVO;
import com.sei.seipicbackend.model.vo.UserVO;
import com.sei.seipicbackend.service.PictureService;
import com.sei.seipicbackend.service.SpaceService;
import com.sei.seipicbackend.service.UserService;
import com.sei.seipicbackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
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
    private CosManager cosManager;

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private SpaceService spaceService;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    @Lazy
    private SpaceUserAuthManager spaceUserAuthManager;


    // region -------------------------- 管理员 --------------------------



    /**
     * 管理员 根据id查询图片
     * @param pictureId
     * @return
     */
    @Override
    public Picture getPictureById(long pictureId) {
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(ObjUtil.isNull(picture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return picture;
    }

    /**
     * 管理员 分页获取图片
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @Override
    public Page<Picture> getPicturePage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        LambdaQueryWrapper<Picture> queryWrapper = getQueryWrapper(pictureQueryRequest);
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> page = new Page<>(current, pageSize);
        return page(page, queryWrapper);
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
     * 审核图片
     * @param pictureReviewRequest
     * @param loginUser
     */
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

    /**
     * 管理员 更新图片
     * @param pictureUpdateRequest
     * @param request
     * @return
     */
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
     * 管理员 批量拉取图片
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
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

    // endregion

    // region -------------------------- 用户 --------------------------

    /**
     * 查询AI扩图任务
     * @param taskId
     */
    @Override
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        return aliYunAiApi.getOutPaintingTask(taskId);
    }


    /**
     * 获取AI扩图任务创建响应
     * @param createPictureOutPaintingTaskRequest
     * @param request
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, HttpServletRequest request) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture==null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 鉴权
        UserVO loginUser = userService.getLoginUser(request);
//        boolean hasPermission = this.isOwnerOrAdmin(picture, loginUser);
//        ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);

        // 构造请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        // 拷贝参数
        BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);

        // 创建任务
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }

    /**
     * 批量编辑图片
     * @param pictureEditByBatchRequest
     * @param request
     */
    @Override
    @Transactional // 真有用吗?
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        String nameRule = pictureEditByBatchRequest.getNameRule();
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR, "请选择图片");
        ThrowUtils.throwIf(spaceId==null || spaceId<=0, ErrorCode.PARAMS_ERROR, "非法的spaceId");
        // 空间鉴权
        UserVO loginUser = userService.getLoginUser(request);
        boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_EDIT);
        ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
//        spaceService.checkSpaceAuth(spaceId, loginUser);


        // 查询图片 (仅返回id和spaceId)
        List<Picture> pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId)
                .select(Picture::getId, Picture::getSpaceId)
                .in(Picture::getId, pictureIdList).list();
        // 更新
        for (Picture picture : pictureList) {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        }
        // 找不到
        if (CollUtil.isEmpty(pictureList)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "没有找到对应图片!");
        }
        // 批量重命名
        fillPictureWithNameRule(pictureList, nameRule);
        // 批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量编辑失败!");
    }

    @Override
    public PictureVO getPictureVoById(long pictureId, HttpServletRequest request) {
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(ObjUtil.isNull(picture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        User user = null;
        UserVO userVO = null;

        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (attribute!=null) {
            user = (User) attribute;
            userVO = user.beanToVo();
        }

        // 公共图库, 且 审核未通过, 只能由本人或管理员查看
        if (picture.getSpaceId()==null && PictureReviewStatusEnum.PASS.getValue()!=picture.getReviewStatus()) {
            boolean hasPermission = this.isOwnerOrAdmin(picture, userVO);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }

        // 私有空间需要鉴权
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId!=null) {
            // 一般来说不会不存在吧
//            UserVO loginUser = userService.getLoginUser(request);
//            spaceService.checkSpaceAuth(spaceId, loginUser);
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space==null, ErrorCode.NOT_FOUND_ERROR);
        }


        PictureVO pictureVoWithUser = getPictureVoWithUser(picture);

        // 补充权限列表
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, user, picture);
        pictureVoWithUser.setPermissionList(permissionList);

        return pictureVoWithUser;
    }

    /**
     * 用户 分页获取图片
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVoPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        Long spaceId = pictureQueryRequest.getSpaceId();

        // 可能需要权限校验, 如果userId为空, 查主页, 如果不为空, 查个人

        if (spaceId==null) {
            // 主页仅能查询过审数据
//            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        }
        else {
//             私有空间鉴权
//            UserVO loginUser = userService.getLoginUser(request);
//            spaceService.checkSpaceAuth(spaceId, loginUser);
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }

        LambdaQueryWrapper<Picture> queryWrapper = getQueryWrapper(pictureQueryRequest);
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> page = new Page<>(current, pageSize);
        Page<Picture> picturePage = page(page, queryWrapper);

        // 仅缓存公共图库的图片
        if (spaceId==null) {
            // 缓存到redis
            String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
            String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
            String rediskey = "seipic:getPictureVoPage:" + hashKey;
            String cachedPage = JSONUtil.toJsonStr(picturePage);
            // 随机设置5~10分钟过期时间, 防止雪崩
            int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
            stringRedisTemplate.opsForValue().set(rediskey, cachedPage, cacheExpireTime, TimeUnit.SECONDS);
        }

//        User user = userService.getLoginUser(request).voToBean();
        return convertToVoPage(picturePage, null);
    }

    /**
     * 分页获取vo with cache
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVoPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 鉴权
//        boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
//        ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);

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

    @Override
    public boolean editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        Long pictureId = pictureEditRequest.getId();
        Picture oldPicture = this.getById(pictureId);
        String name = pictureEditRequest.getName();
        String introduction = pictureEditRequest.getIntroduction();
        ThrowUtils.throwIf(oldPicture==null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 鉴权
        UserVO loginUser = userService.getLoginUser(request);
//        boolean hasPermission = isOwnerOrAdmin(oldPicture, loginUser);
//        ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);

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
        // 如果是公共图库, 填充审核参数
        if (oldPicture.getSpaceId()==null) {
            String oldName = oldPicture.getName();
            String oldIntro = oldPicture.getIntroduction();
            // 如果name和intro不全为空, 且与旧name/intro不相同, 需要重新提审
            if(!StrUtil.isAllBlank(name, introduction) && !( Objects.equals(oldName, name)&&Objects.equals(oldIntro, introduction) )) {
                fillReviewParams(newPicture, loginUser);
            } else {
                // 不需要重新提审
                newPicture.setReviewStatus(null);
            }
        }

        boolean result = updateById(newPicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    /**
     * 根据主色调搜索图库中的图片
     * @param spaceId
     * @param picColor
     * @param request
     * @return
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceId==null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        // 空间鉴权
        UserVO loginUser = userService.getLoginUser(request);
        spaceService.checkSpaceAuth(spaceId, loginUser);

        // 获取空间所有带有主色调的图片
        List<Picture> pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor).list();
        if (pictureList==null) {
            return Collections.emptyList();
        }
        // 计算主色调相似度并排序 (top 12)
        Color targetColor = Color.decode(picColor);
        List<Picture> sortedPictures = pictureList.stream().sorted(Comparator.comparingDouble(picture -> {
            if (StrUtil.isBlank(picture.getPicColor())) {
                return Double.MAX_VALUE;
            }
            Color pictureColor = Color.decode(picture.getPicColor());
            // utils返回的数字越大, 说明越相似
            // comparator数字越小名次越高, 所以对相似度取负
            return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
        })).limit(12).collect(Collectors.toList());

        // 以图搜图仅能在个人空间使用, 无需关联用户信息
        return BeanUtil.copyToList(sortedPictures, PictureVO.class);
    }


    /**
     * 上传图片
     * @param inputSource multipartFile或fileUrl
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) throws IOException {
        ThrowUtils.throwIf(inputSource==null, ErrorCode.NOT_FOUND_ERROR);
        Long newSpaceId = pictureUploadRequest.getSpaceId();
        Picture picture = null;

        // 校验登录, 需要填充上传者id
        UserVO loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginUser), ErrorCode.NOT_LOGIN_ERROR);

        // 如果存在pictureId, 说明是图片更新
        // 需要校验spaceId是否一致
        Long pictureId = pictureUploadRequest.getId();
        if (pictureId!=null) {
            picture = lambdaQuery().eq(Picture::getId, pictureId).one();
            ThrowUtils.throwIf(picture==null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            Long oldSpaceId = picture.getSpaceId();
            ThrowUtils.throwIf(!Objects.equals(oldSpaceId, newSpaceId), ErrorCode.PARAMS_ERROR, "前后空间id不一致");
            // 如果是编辑, 需要鉴权
//            isOwnerOrAdmin(picture, loginUser);
        }

        // 上传图片
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());

        // 根据inputSource决定上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 如果是更新, 删除原图片 (待新图片上传成功后执行)
        if (pictureId!=null) {
            // 更新图片后, 对原图片进行删除
            this.clearPictureFile(picture);
            // 如果是私有空间释放空间额度
            if (newSpaceId!=null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, newSpaceId)
                        .setSql("totalCount = totalCount-1")
                        .setSql("totalSize = totalSize-" + picture.getPicSize())
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "图片额度更新失败");
            }
        }

        // 从上传结果中获取图片信息, 并填充
        Picture newPicture = fillPictureParams(loginUser, uploadPictureResult);
        newPicture.setSpaceId(newSpaceId);

        // 如果有category或tags信息, 填充
        String category = pictureUploadRequest.getCategory();
        if (StrUtil.isNotBlank(category)) {
            newPicture.setCategory(category);
        }

        // tags需要转化为 json数组 字符串
        List<String> tags = pictureUploadRequest.getTags();
        if (CollUtil.isNotEmpty(tags)) {
            newPicture.setTags(JSONUtil.toJsonStr(tags));
        }

        String picName = pictureUploadRequest.getPicName();
        if (StrUtil.isNotBlank(picName)) {
            newPicture.setName(picName);
        }

        // 如果是更新, 设置编辑时间和id
        if (pictureId!=null) {
            newPicture.setId(pictureId);
            newPicture.setEditTime(new Date());
            // 如果是公共图库, 重新审核
            if (newSpaceId!=null) {
                newPicture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
            }
        }

        // 填写审核参数
        // 如果是私人空间, 没必要审核
        if (newSpaceId==null) {
            fillReviewParams(newPicture, loginUser);
        }

        // 上传到非公共图库, 需要鉴权
        if (newSpaceId!=null) {
            // 鉴权
//            spaceService.checkSpaceAuth(newSpaceId, loginUser);
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_UPLOAD);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);

            // 校验额度
            Space space = spaceService.getById(newSpaceId);
            Long maxCount = space.getMaxCount();
            Long maxSize = space.getMaxSize();
            Long totalCount = space.getTotalCount();
            Long totalSize = space.getTotalSize();
            ThrowUtils.throwIf(totalCount>=maxCount, ErrorCode.OPERATION_ERROR, "空间图片数量达到上限");
            ThrowUtils.throwIf(totalSize>=maxSize, ErrorCode.OPERATION_ERROR, "空间图片容量达到上限");
        }

        // 开启事务
        Long finalSpaceId = newSpaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(newPicture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            // 更新空间额度
            if(finalSpaceId!=null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, newSpaceId)
                        .setSql("totalCount = totalCount+1")
                        .setSql("totalSize = totalSize+" + newPicture.getPicSize())
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "图片额度更新失败");
            }
            return getPictureVoWithUser(newPicture);
        });

        // 封装图片
        return getPictureVoWithUser(newPicture);
    }

    /**
     * 删除图片
     * @param pictureId
     * @param request
     * @return
     */
    @Override
    public boolean deletePictureById(long pictureId, HttpServletRequest request) {
        UserVO loginUser = userService.getLoginUser(request);
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(ObjUtil.isNull(picture), ErrorCode.NOT_FOUND_ERROR);
//        ThrowUtils.throwIf(!isOwnerOrAdmin(picture, loginUser), ErrorCode.NO_AUTH_ERROR);
        Long spaceId = picture.getSpaceId();
        // 异步删除COS的图片
        this.clearPictureFile(picture);

        transactionTemplate.execute(status -> {
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片删除失败");
            // 删除私有空间, 更新空间额度
            if (spaceId!=null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalCount = totalCount-1")
                        .setSql("totalSize = totalSize-" + picture.getPicSize())
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "图片额度更新失败");
            }

            return true;
        });

        return true;
    }
    // endregion

    // region -------------------------- 公共方法 --------------------------

    /**
     * 图片鉴权
     * @param picture
     * @param user
     * @return
     */
    @Override
    public boolean checkPictureAuth(Picture picture, User user) {
        Long userId = picture.getUserId();
        return UserConstant.ADMIN_ROLE.equals(user.getUserRole()) || userId.equals(user.getId());

    }

    /**
     * nameRule 格式：图片{序号}
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
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
     * pojo Page转vo Page
     * @param picturePage
     * @return
     */
    private Page<PictureVO> convertToVoPage(Page<Picture> picturePage, User user) {
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
            // 填充权限列表
//            List<String> permissionList = spaceUserAuthManager.getPermissionList(null, user, PictureVO.voToObj(pictureVO));
//            pictureVO.setPermissionList(permissionList);
        });

        return pictureVoPage;
    }

    /**
     * 将Picture实体列表转换为PictureVO视图对象列表，并填充用户信息
     * @param pictureList Picture实体列表
     * @return 包含用户信息的PictureVO视图对象列表
     */
    private List<PictureVO> convertToVoList(List<Picture> pictureList, User user) {
    // 如果输入列表为空，返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }

        // 获取用户id列表
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // id:user Map
        Map<Long, UserVO> idUserVoMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        // 填充user信息
        List<PictureVO> pictureVOList = BeanUtil.copyToList(pictureList, PictureVO.class);
        for (PictureVO pictureVO : pictureVOList) {
            Long id = pictureVO.getId();
            if (idUserVoMap.containsKey(id)) {
                pictureVO.setUser(idUserVoMap.get(id));
            }
            // 填充权限列表
//            List<String> permissionList = spaceUserAuthManager.getPermissionList(null, user, PictureVO.voToObj(pictureVO));
//            pictureVO.setPermissionList(permissionList);
        }

        return pictureVOList;
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
//        Integer picWidth = pictureQueryRequest.getPicWidth();
//        Integer picHeight = pictureQueryRequest.getPicHeight();
        String scaleType = pictureQueryRequest.getScaleType();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Boolean nullSpaceId = pictureQueryRequest.getNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), Picture::getId, id);
        queryWrapper.like(StrUtil.isNotBlank(name), Picture::getName, name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), Picture::getIntroduction, introduction);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), Picture::getReviewMessage, reviewMessage);

        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), Picture::getPicSize, picSize);

        // 或者更语义化的分类查询
        if ("horizontal".equals(scaleType)) {
            queryWrapper.gt(Picture::getPicScale, 1.1); // 横图
        } else if ("vertical".equals(scaleType)) {
            queryWrapper.lt(Picture::getPicScale, 0.9); // 竖图
        } else if ("square".equals(scaleType)) {
            queryWrapper.between(Picture::getPicScale, 0.9, 1.1); // 方图
        }

//        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), Picture::getPicWidth, picWidth);
//        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), Picture::getPicHeight, picHeight);

        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), Picture::getPicScale, picScale);
        queryWrapper.eq(StrUtil.isNotBlank(picFormat), Picture::getPicFormat, picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), Picture::getCategory, category);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), Picture::getUserId, userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), Picture::getReviewerId, reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), Picture::getReviewStatus, reviewStatus);
        queryWrapper.ge(startEditTime!=null, Picture::getEditTime, startEditTime);
        queryWrapper.lt(endEditTime!=null, Picture::getEditTime, endEditTime);
        queryWrapper.eq(spaceId!=null, Picture::getSpaceId, spaceId);
        if (nullSpaceId!=null) {
            queryWrapper.isNull(nullSpaceId, Picture::getSpaceId);
        }


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

    /**
     * 填充图片参数
     * @param loginUser
     * @param uploadPictureResult
     * @return
     */
    private static Picture fillPictureParams(UserVO loginUser, UploadPictureResult uploadPictureResult) {
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
        picture.setPicColor(uploadPictureResult.getPicColor());
        return picture;
    }

    /**
     * pojo转vo, 关联查询用户信息
     * @param picture
     * @return
     */
    private PictureVO getPictureVoWithUser(Picture picture) {
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

    /**
     * 图片鉴权
     * 图片上传者/空间所有者/管理员 (之后补充空间管理者)
     * @param picture
     * @param loginUserVO
     * @return
     */
    private boolean isOwnerOrAdmin(Picture picture, UserVO loginUserVO) {
        if (loginUserVO==null) {
            return false;
        }

        Long owner = picture.getUserId();
        Long userId = loginUserVO.getId();
        Long spaceId = picture.getSpaceId();

        boolean isSpaceOwner = false;
        if (spaceId!=null) {
            Space space = spaceService.getById(spaceId);
            Long spaceUserId = space.getUserId();
            isSpaceOwner = spaceUserId.equals(userId);
        }
        boolean isPictureOwner = owner.equals(userId);
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUserVO.getUserRole());

        return isSpaceOwner || isPictureOwner || isAdmin;
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

    /**
     * 校验图片
     * @param picture
     */
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

            if ("jpg".equals(picFormat) || "jpeg".equals(picFormat)) {
                String jpegUrl = compressedUrl.substring(0, lastIndexOf+1) + "jpeg";
                String jpgUrl = compressedUrl.substring(0, lastIndexOf+1) + "jpg";
                cosManager.deleteObject(jpegUrl);
                cosManager.deleteObject(jpgUrl);
            } else {
                String originUrl = compressedUrl.substring(0, lastIndexOf+1) + picFormat;
                cosManager.deleteObject(originUrl);
            }
        }

        // 清理压缩图 (webp)
        cosManager.deleteObject(compressedUrl);

        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }
    // endregion
}