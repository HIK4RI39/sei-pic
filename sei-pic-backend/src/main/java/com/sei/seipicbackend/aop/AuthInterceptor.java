package com.sei.seipicbackend.aop;

import cn.hutool.core.util.StrUtil;
import com.sei.seipicbackend.annotation.AuthCheck;
import com.sei.seipicbackend.constant.UserConstant;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.model.enums.UserRoleEnum;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.UserVO;
import com.sei.seipicbackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.Struct;

/**
 * @author hikari39_
 * @since 2026-03-28
 */
@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 获取要校验的角色
        String mustRole = authCheck.mustRole();

        // 通过全局上下文获得attribute
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        // 类型转化, 可以调用getAttribute方法
        // 需要调用getLoginUser
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        UserVO loginUser = userService.getLoginUser(request);


        // 鉴权或放行
        if (StrUtil.isBlank(mustRole)) {
            return joinPoint.proceed();
        }

        String userRole = loginUser.getUserRole();
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(userRole);
        ThrowUtils.throwIf(
                UserRoleEnum.ADMIN.getValue().equals(mustRole) && !UserRoleEnum.ADMIN.equals(userRoleEnum),
                ErrorCode.NO_AUTH_ERROR
        );

        return joinPoint.proceed();

    }
}
