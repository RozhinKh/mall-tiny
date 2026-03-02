package com.macro.mall.tiny.modules.ums.dto;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

/**
 * 用户登录参数
 * Created by macro on 2018/4/26.
 */
public record UmsAdminParam(
    @NotEmpty
    @ApiModelProperty(value = "用户名", required = true)
    String username,
    @NotEmpty
    @ApiModelProperty(value = "密码", required = true)
    String password,
    @ApiModelProperty(value = "用户头像")
    String icon,
    @Email
    @ApiModelProperty(value = "邮箱")
    String email,
    @ApiModelProperty(value = "用户昵称")
    String nickName,
    @ApiModelProperty(value = "备注")
    String note
) {
}
