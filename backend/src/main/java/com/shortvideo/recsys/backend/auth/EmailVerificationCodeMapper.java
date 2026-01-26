package com.shortvideo.recsys.backend.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailVerificationCodeMapper extends BaseMapper<EmailVerificationCodeEntity> {
}

