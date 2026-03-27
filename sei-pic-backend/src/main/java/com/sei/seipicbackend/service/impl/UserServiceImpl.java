package com.sei.seipicbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sei.seipicbackend.common.PageRequest;
import com.sei.seipicbackend.constant.UserConstant;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.mapper.UserMapper;
import com.sei.seipicbackend.model.dto.user.UserAddRequest;
import com.sei.seipicbackend.model.dto.user.UserPageRequest;
import com.sei.seipicbackend.model.dto.user.UserUpdateRequest;
import com.sei.seipicbackend.model.enums.UserRoleEnum;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.UserVO;
import com.sei.seipicbackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
* @author hikari39
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2026-03-28 03:18:14
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    private static final String SALT = "sei";
    private static final String DEFAULT_PWD = "12345678";

    // region -------------------------- 用户 --------------------------

    /**
     * 用户注册
     * @param userAccount
     * @param password
     * @param checkPassword
     * @return
     */
    @Override
    public long userRegister(String userAccount, String password, String checkPassword) {
        ThrowUtils.throwIf(!StrUtil.isAllNotBlank(userAccount, password, checkPassword), ErrorCode.PARAMS_ERROR);
        // 账号长度>=4
        ThrowUtils.throwIf(userAccount.length()<4, ErrorCode.PARAMS_ERROR);
        // 密码长度>=4
        ThrowUtils.throwIf(password.length()<8, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(checkPassword.length()<8, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!password.equals(checkPassword), ErrorCode.PARAMS_ERROR);

        boolean exists = lambdaQuery().eq(User::getUserAccount, userAccount).exists();
        ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "账号已存在");
        // 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
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
        ThrowUtils.throwIf(!StrUtil.isAllNotBlank(userAccount, password), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userAccount.length()<4, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(password.length()<8, ErrorCode.PARAMS_ERROR);
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());

        User user = lambdaQuery().eq(User::getUserAccount, userAccount).eq(User::getUserPassword, encryptPassword).one();
        ThrowUtils.throwIf(ObjUtil.isNull(user), ErrorCode.NO_AUTH_ERROR);
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);

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

    // endregion

    // region -------------------------- 管理员 --------------------------


    @Override
    public Long addUser(UserAddRequest userAddRequest) {
        String userAccount = userAddRequest.getUserAccount();
        return this.userRegister(userAccount, DEFAULT_PWD, DEFAULT_PWD);
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

        Page<User> userPage = lambdaQuery().eq(id != null, User::getId, id)
                .like(StrUtil.isNotBlank(userAccount), User::getUserAccount, userAccount)
                .like(StrUtil.isNotBlank(userName), User::getUserName, userName)
                .eq(StrUtil.isNotBlank(userRole), User::getUserRole, userRole)
                .gt(createBeginTime != null, User::getCreateTime, createBeginTime)
                .lt(createEndTime != null, User::getCreateTime, createEndTime)
                .page(page);

        long total = page.getTotal();

        List<UserVO> userVoList = BeanUtil.copyToList(userPage.getRecords(), UserVO.class);
        Page<UserVO> pageVo = new Page<>();
        pageVo.setTotal(total).setRecords(userVoList);
        return pageVo;
    }

    @Override
    public Boolean updateUser(UserUpdateRequest userUpdateRequest) {
        Long id = userUpdateRequest.getId();
        User user = getById(id);
        ThrowUtils.throwIf(user==null, ErrorCode.NOT_FOUND_ERROR);

        String userName = userUpdateRequest.getUserName();
        String userProfile = userUpdateRequest.getUserProfile();
        String userPassword = userUpdateRequest.getUserPassword();
        String userRole = userUpdateRequest.getUserRole();
        ThrowUtils.throwIf(StrUtil.isAllBlank(userName, userProfile, userProfile, userRole), ErrorCode.PARAMS_ERROR);

        User newUser = new User();
        // id
        newUser.setId(id);
        // username
        if (StrUtil.isNotBlank(userUpdateRequest.getUserName())) {
            newUser.setUserName(userUpdateRequest.getUserName());
        }
        // profile
        if (StrUtil.isNotBlank(userUpdateRequest.getUserProfile())) {
            newUser.setUserProfile(userUpdateRequest.getUserProfile());
        }
        // pwd
        if (StrUtil.isNotBlank(userPassword)) {
            String encryptPassword = getEncryptPassword(userPassword);
            newUser.setUserPassword(encryptPassword);
        }
        // role
        if (StrUtil.isNotBlank(userUpdateRequest.getUserRole())) {
            newUser.setUserRole(userUpdateRequest.getUserRole());
        }

        boolean result = updateById(newUser);

        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }
    // endregion

    public String getEncryptPassword(String password) {
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }

}




