package com.sei.seipicbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sei.seipicbackend.configuration.UserConfig;
import com.sei.seipicbackend.constant.UserConstant;
import com.sei.seipicbackend.exception.BusinessException;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.manager.auth.StpKit;
import com.sei.seipicbackend.mapper.UserMapper;
import com.sei.seipicbackend.model.dto.space.SpaceAddRequest;
import com.sei.seipicbackend.model.dto.user.*;
import com.sei.seipicbackend.model.enums.SpaceLevelEnum;
import com.sei.seipicbackend.model.enums.SpaceTypeEnum;
import com.sei.seipicbackend.model.enums.UserRoleEnum;
import com.sei.seipicbackend.model.pojo.Space;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.UserVO;
import com.sei.seipicbackend.service.SpaceService;
import com.sei.seipicbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
* @author hikari39
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2026-03-28 03:18:14
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    // 定义加密盐值和默认密码常量
    @Autowired
    UserConfig userConfig;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    @Lazy
    private SpaceService spaceService;

    // 文件读写锁（确保并发安全）
    private final ReentrantLock fileLock = new ReentrantLock();

    // region -------------------------- 通用 --------------------------


    /**
     * 校验兑换码并标记为已使用
     */
    private VipCode validateAndMarkVipCode(String vipCode) {
        fileLock.lock(); // 加锁保证文件操作原子性
        try {
            // 读取 JSON 文件
            JSONArray jsonArray = readVipCodeFile();

            // 查找匹配的未使用兑换码
            List<VipCode> codes = JSONUtil.toList(jsonArray, VipCode.class);
            VipCode target = codes.stream()
                    .filter(code -> code.getCode().equals(vipCode) && !code.isHasUsed())
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "无效的兑换码"));

            // 标记为已使用
            target.setHasUsed(true);

            // 写回文件
            writeVipCodeFile(JSONUtil.parseArray(codes));
            return target;
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * 读取兑换码文件
     */
    private JSONArray readVipCodeFile() {
        try {
            Resource resource = resourceLoader.getResource("classpath:biz/vipCode.json");
            String content = FileUtil.readString(resource.getFile(), StandardCharsets.UTF_8);
            return JSONUtil.parseArray(content);
        } catch (IOException e) {
            log.error("读取兑换码文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
        }
    }

    /**
     * 写入兑换码文件
     */
    private void writeVipCodeFile(JSONArray jsonArray) {
        try {
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:biz/vipCode.json");
            FileUtil.writeString(jsonArray.toStringPretty(), resource.getFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("更新兑换码文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
        }
    }

    /**
     * 更新用户会员信息
     */
    private void updateUserVipInfo(User user, String usedVipCode) {
        // 计算过期时间（当前时间 + 1 年）
        Date expireTime = DateUtil.offsetMonth(new Date(), 12); // 计算当前时间加 1 年后的时间

        // 构建更新对象
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setVipExpiredTime(expireTime);
        updateUser.setVipCode(usedVipCode);
        updateUser.setUserRole(UserConstant.VIP);

        // 执行更新
        boolean updated = this.updateById(updateUser);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "开通会员失败，操作数据库失败");
        }
    }
    /**
     * 校验管理员权限
     * @param userVO
     */
    @Override
    public void checkAdmin(UserVO userVO) {
        String userRole = userVO.getUserRole();
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(userRole);
        ThrowUtils.throwIf(!isAdmin, ErrorCode.NO_AUTH_ERROR);
    }

    @Override
    public boolean isAdmin(User user) {
        return UserConstant.ADMIN_ROLE.equals(user.getUserRole());
    }

    // endregion

    // region -------------------------- 用户 --------------------------

    /**
     * 兑换会员
     * @param vipCode
     * @param request
     * @return
     */
    @Override
    public boolean exchangeVip(VipCode vipCode, HttpServletRequest request) {
        String code = vipCode.getCode();
        ThrowUtils.throwIf(!StrUtil.isAllNotBlank(code), ErrorCode.PARAMS_ERROR, "VIP兑换码不能为空");
        UserVO loginUser = this.getLoginUser(request);
        User user = loginUser.voToBean();
        // 1. 参数校验
        if (user == null || StrUtil.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 读取并校验兑换码
        VipCode targetCode = validateAndMarkVipCode(code);
        // 3. 更新用户信息
        updateUserVipInfo(user, targetCode.getCode());

        // 对于会员有什么增值服务? => 空间容量 => 升级旗舰版? (暂定)
        // 如果过期了 => ? 返回普通版
        // 将该用户名下所有空间转化为旗舰空间, 如果没有, 就创建
        Space privateSpace = spaceService.lambdaQuery().eq(Space::getUserId, user.getId()).eq(Space::getSpaceType, SpaceTypeEnum.PRIVATE).one();
        Space teamSpace = spaceService.lambdaQuery().eq(Space::getUserId, user.getId()).eq(Space::getSpaceType, SpaceTypeEnum.TEAM).one();
        SpaceAddRequest spaceAddRequest = new SpaceAddRequest();
        spaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        spaceAddRequest.setSpaceName("默认空间");

        if (privateSpace==null) {
            spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
            spaceService.createSpace(spaceAddRequest, request);
        }

        if (teamSpace==null) {
            spaceAddRequest.setSpaceType(SpaceTypeEnum.TEAM.getValue());
            spaceService.createSpace(spaceAddRequest, request);
        }

        // 更新空间额度
        boolean update = spaceService.lambdaUpdate().eq(Space::getUserId, user.getId())
                .set(Space::getSpaceLevel, SpaceLevelEnum.FLAGSHIP.getValue())
                .set(Space::getMaxCount, SpaceLevelEnum.FLAGSHIP.getMaxCount())
                .set(Space::getMaxSize, SpaceLevelEnum.FLAGSHIP.getMaxSize())
                .update();

        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "兑换失败");

        return true;
    }

    /**
     * 用户注册
     * @param userAccount
     * @param password
     * @param checkPassword
     * @return
     */
    @Override
    public long userRegister(String userAccount, String password, String checkPassword) {
        // 任何一个都不能为空
        ThrowUtils.throwIf(!StrUtil.isAllNotBlank(userAccount, password, checkPassword), ErrorCode.PARAMS_ERROR, "账号密码不能为空!");
        // 对账号密码做左右空格去除(trim)
        userAccount = StrUtil.trim(userAccount);
        password = StrUtil.trim(password);
        checkPassword = StrUtil.trim(checkPassword);
        // 账号长度4~16, 且不能包含非法字符
        ThrowUtils.throwIf(!(4<=userAccount.length()&&userAccount.length()<=16), ErrorCode.PARAMS_ERROR, "账号长度限制4~16");
        ThrowUtils.throwIf(!userAccount.matches("^[a-zA-Z0-9.]+$"), ErrorCode.PARAMS_ERROR, "账号只能包含数字和字母以及.");
        // 密码长度8~16, 不能包含非法字符, 且与校验密码相同
        ThrowUtils.throwIf(!(8<=password.length()&&password.length()<=16), ErrorCode.PARAMS_ERROR, "账号长度限制8~16");
        ThrowUtils.throwIf(!password.matches("^[a-zA-Z0-9.]+$"), ErrorCode.PARAMS_ERROR, "密码只能包含数字和字母以及.");
        ThrowUtils.throwIf(!password.equals(checkPassword), ErrorCode.PARAMS_ERROR);


        boolean exists = lambdaQuery().eq(User::getUserAccount, userAccount).exists();
        ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "账号已存在");
        // 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((userConfig.getSalt() + password).getBytes());
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = save(user);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR);
        return user.getId();
    }

    /**
     * 用户登录
     * @param userAccount
     * @param password
     * @param request
     * @return
     */
    @Override
    public UserVO login(String userAccount, String password, HttpServletRequest request) {
        // 任何一个都不能为空
        ThrowUtils.throwIf(!StrUtil.isAllNotBlank(userAccount, password), ErrorCode.PARAMS_ERROR, "账号密码不能为空!");
        // 对账号密码做左右空格去除(trim)
        userAccount = StrUtil.trim(userAccount);
        password = StrUtil.trim(password);
        // 账号长度4~16, 且不能包含非法字符
        ThrowUtils.throwIf(!(4<=userAccount.length()&&userAccount.length()<=16), ErrorCode.PARAMS_ERROR, "账号长度限制4~16");
        ThrowUtils.throwIf(!userAccount.matches("^[a-zA-Z0-9.]+$"), ErrorCode.PARAMS_ERROR, "账号只能包含数字和字母以及.");
        // 密码长度8~16, 且不能包含非法字符
        ThrowUtils.throwIf(!(8<=password.length()&&password.length()<=16), ErrorCode.PARAMS_ERROR, "账号长度限制8~16");
        ThrowUtils.throwIf(!password.matches("^[a-zA-Z0-9.]+$"), ErrorCode.PARAMS_ERROR, "密码只能包含数字和字母以及.");

        // 获得加密密码
        String encryptPassword = DigestUtils.md5DigestAsHex((userConfig.getSalt() + password).getBytes());
        User user = lambdaQuery().eq(User::getUserAccount, userAccount).eq(User::getUserPassword, encryptPassword).one();
        ThrowUtils.throwIf(ObjUtil.isNull(user), ErrorCode.OPERATION_ERROR, "用户不存在或密码错误");
        // 记录登录态到session
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 记录登录态到sa-token
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);

        return user.beanToVo();
    }

    @Override
    public UserVO getLoginUser(HttpServletRequest request) {
        User user =  (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(ObjUtil.isNull(user), ErrorCode.NOT_LOGIN_ERROR);
        return user.beanToVo();
    }

    @Override
    public Boolean logout(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) attribute;
        ThrowUtils.throwIf(ObjUtil.isNull(user), ErrorCode.NOT_LOGIN_ERROR);
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 用户 根据id获取用户vo
     * @param id
     * @return
     */
    @Override
    public UserVO getUserVoById(Long id) {
        User user = getById(id);
        ThrowUtils.throwIf(ObjUtil.isNull(user), ErrorCode.NOT_FOUND_ERROR);
        return user.beanToVo();
    }

    @Override
    public Boolean editUser(UserEditRequest userEditRequest, HttpServletRequest request) {
        // 左右空格去除(trim)
        String userName = StrUtil.trim(userEditRequest.getUserName());
        String userProfile = StrUtil.trim(userEditRequest.getUserProfile());
        String oldPassword = StrUtil.trim(userEditRequest.getOldPassword());
        String newPassWord = StrUtil.trim(userEditRequest.getNewPassWord());
        String userAvatar = StrUtil.trim(userEditRequest.getUserAvatar());
        // 不能全部为空
        ThrowUtils.throwIf(StrUtil.isAllBlank(userName, userProfile, oldPassword, newPassWord, userAvatar), ErrorCode.PARAMS_ERROR);

        // 编辑时间
        Date date = new Date();
        UserVO loginUser = getLoginUser(request);

        /**
         * 根据密码查询出用户
         */
        if (StrUtil.isNotBlank(newPassWord)) {
            ThrowUtils.throwIf(oldPassword.equals(newPassWord), ErrorCode.PARAMS_ERROR, "新密码不能与旧密码相同");
            ThrowUtils.throwIf(!(8<=newPassWord.length()&&newPassWord.length()<=16), ErrorCode.PARAMS_ERROR, "新密码长度限制8~16");
            ThrowUtils.throwIf(!newPassWord.matches("^[a-zA-Z0-9.]+$"), ErrorCode.PARAMS_ERROR, "新密码只能包含数字和字母以及.");

            ThrowUtils.throwIf(StrUtil.isBlank(oldPassword), ErrorCode.PARAMS_ERROR, "旧密码不能为空");
            ThrowUtils.throwIf(!(8<=oldPassword.length()&&oldPassword.length()<=16), ErrorCode.PARAMS_ERROR, "旧密码长度限制8~16");
            ThrowUtils.throwIf(!oldPassword.matches("^[a-zA-Z0-9.]+$"), ErrorCode.PARAMS_ERROR, "旧密码只能包含数字和字母以及.");
            // 得到加密后的旧密码
            String encryptPassword = getEncryptPassword(oldPassword);
            Long userId = loginUser.getId();
            User user = lambdaQuery().eq(User::getId, userId).eq(User::getUserPassword, encryptPassword).one();
            ThrowUtils.throwIf(user==null, ErrorCode.OPERATION_ERROR, "密码错误!");
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, loginUser.getId())
                .set(StrUtil.isNotBlank(userName), User::getUserName, userName)
                .set(StrUtil.isNotBlank(userProfile), User::getUserProfile, userProfile)
                .set(StrUtil.isNotBlank(newPassWord), User::getUserPassword, getEncryptPassword(newPassWord))
                .set(StrUtil.isNotBlank(userAvatar), User::getUserAvatar, userAvatar)
                .set(User::getEditTime, date);

        boolean result = update(updateWrapper);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    // endregion

    // region -------------------------- 管理员 --------------------------

    @Override
    public Long addUser(UserAddRequest userAddRequest) {
        String userAccount = userAddRequest.getUserAccount();
        return this.userRegister(userAccount, userConfig.getDefaultPwd(), userConfig.getDefaultPwd());
    }

    /**
     * 管理员 根据id获取用户
     * @param id
     * @return
     */
    @Override
    public User getUserById(Long id) {
        User user = getById(id);
        ThrowUtils.throwIf(ObjUtil.isNull(user), ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public Boolean deleteUserById(long id) {
        boolean result = removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public Page<UserVO>  listUserVoByPage(UserPageRequest userPageRequest) {
        Long id = userPageRequest.getId();
        String userAccount = userPageRequest.getUserAccount();
        String userName = userPageRequest.getUserName();
        String userRole = userPageRequest.getUserRole();
        Date createBeginTime = userPageRequest.getCreateBeginTime();
        Date createEndTime = userPageRequest.getCreateEndTime();

        Page<User> page = new Page<>();
        int pageSize = userPageRequest.getPageSize();
        int current = userPageRequest.getCurrent();
        page.setCurrent(current>0 ? current : 1);
        page.setSize(pageSize>0 ? pageSize : 10);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(id != null, User::getId, id)
                .like(StrUtil.isNotBlank(userAccount), User::getUserAccount, userAccount)
                .like(StrUtil.isNotBlank(userName), User::getUserName, userName)
                .eq(StrUtil.isNotBlank(userRole), User::getUserRole, userRole)
                .gt(createBeginTime != null, User::getCreateTime, createBeginTime)
                .lt(createEndTime != null, User::getCreateTime, createEndTime);

        page(page, queryWrapper);

        return (Page<UserVO>) page.convert(User::beanToVo);
    }

    @Override
    public Boolean updateUser(UserUpdateRequest userUpdateRequest) {
        Long id = userUpdateRequest.getId();
        String userName = userUpdateRequest.getUserName();
        String userProfile = userUpdateRequest.getUserProfile();
        String userPassword = userUpdateRequest.getUserPassword();
        String userRole = userUpdateRequest.getUserRole();

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, id)
                .set(StrUtil.isNotBlank(userName), User::getUserName, userName)
                .set(StrUtil.isNotBlank(userProfile), User::getUserProfile, userProfile)
                .set(StrUtil.isNotBlank(userPassword), User::getUserPassword, getEncryptPassword(userPassword))
                .set(StrUtil.isNotBlank(userRole), User::getUserRole, userRole);

        boolean result = update(updateWrapper);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    // endregion

    public String getEncryptPassword(String password) {
        return DigestUtils.md5DigestAsHex((userConfig.getSalt() + password).getBytes());
    }

}




