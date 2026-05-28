package cn.iocoder.yudao.module.system.controller.app.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Schema(description = "用户 APP - 微信绑定账号 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppAuthWechatBindReqVO {

    @Schema(description = "微信授权码", requiredMode = Schema.RequiredMode.REQUIRED, example = "code123")
    @NotEmpty(message = "微信授权码不能为空")
    private String code;

    @Schema(description = "状态码", example = "state123")
    private String state;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    @NotEmpty(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotEmpty(message = "密码不能为空")
    private String password;

}
