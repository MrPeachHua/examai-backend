package cn.iocoder.yudao.module.system.controller.app.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Schema(description = "用户 APP - 微信登录 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppAuthWechatLoginReqVO {

    @Schema(description = "微信授权码", requiredMode = Schema.RequiredMode.REQUIRED, example = "code123")
    @NotEmpty(message = "微信授权码不能为空")
    private String code;

    @Schema(description = "状态码，防止CSRF攻击", example = "state123")
    private String state;

    @Schema(description = "用户角色", example = "student")
    private String role;

}
