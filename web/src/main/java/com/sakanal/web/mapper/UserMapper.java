package com.sakanal.web.mapper;

import com.sakanal.web.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author sakanal
 * @since 2023-01-13
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
