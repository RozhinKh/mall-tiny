package com.macro.mall.tiny.modules.ums.dto;

import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * 后台菜单节点封装
 * Created by macro on 2020/2/4.
 */
public record UmsMenuNode(
    @ApiModelProperty(value = "子级菜单")
    List<UmsMenuNode> children
) {
}
