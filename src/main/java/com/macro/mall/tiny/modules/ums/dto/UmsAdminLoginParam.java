package com.macro.mall.tiny.modules.ums.dto;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;

/**
 * 用户登录参数
 * Created by macro on 2018/4/26.
 */
public record UmsAdminLoginParam(
    @NotEmpty
    @ApiModelProperty(value = "用户名", required = true)
    String username,
    @NotEmpty
    @ApiModelProperty(value = "密码", required = true)
    String password
) {
}
