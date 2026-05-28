package cn.iocoder.yudao.module.system.controller.app.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Schema(description = "用户 APP - 发送验证码 Request VO")
@Data
public class AppSendCodeReqVO {

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "验证码类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "register")
    @NotEmpty(message = "验证码类型不能为空")
    private String type;

}
