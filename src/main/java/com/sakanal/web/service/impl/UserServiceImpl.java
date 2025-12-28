package com.sakanal.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sakanal.web.entity.User;
import com.sakanal.web.mapper.UserMapper;
import com.sakanal.web.service.UserService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author sakanal
 * @since 2023-01-13
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}
