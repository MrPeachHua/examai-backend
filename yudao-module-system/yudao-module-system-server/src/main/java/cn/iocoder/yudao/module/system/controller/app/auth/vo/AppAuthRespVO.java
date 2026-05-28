package cn.iocoder.yudao.module.system.controller.app.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "用户 APP - 认证 Response VO（含令牌和用户信息）")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppAuthRespVO {

    @Schema(description = "访问令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "happy")
    private String token;

    @Schema(description = "用户信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private AppUserInfoRespVO user;

}
