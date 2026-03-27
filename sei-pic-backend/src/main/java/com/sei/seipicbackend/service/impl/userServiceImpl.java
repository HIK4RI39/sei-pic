package com.sei.seipicbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sei.seipicbackend.model.pojo.user;
import com.sei.seipicbackend.service.userService;
import com.sei.seipicbackend.mapper.userMapper;
import org.springframework.stereotype.Service;

/**
* @author hikari39
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2026-03-28 03:18:14
*/
@Service
public class userServiceImpl extends ServiceImpl<userMapper, user>
    implements userService{

}




