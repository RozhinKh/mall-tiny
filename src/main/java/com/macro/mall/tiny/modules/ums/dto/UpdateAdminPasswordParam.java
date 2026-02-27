package com.macro.mall.tiny.modules.ums.dto;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;

/**
 * 修改用户名密码参数
 * Created by macro on 2019/10/9.
 */
public record UpdateAdminPasswordParam(
    @NotEmpty
    @ApiModelProperty(value = "用户名", required = true)
    String username,
    @NotEmpty
    @ApiModelProperty(value = "旧密码", required = true)
    String oldPassword,
    @NotEmpty
    @ApiModelProperty(value = "新密码", required = true)
    String newPassword
) {
}
