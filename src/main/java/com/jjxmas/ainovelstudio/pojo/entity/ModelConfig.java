package com.jjxmas.ainovelstudio.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jjxmas.ainovelstudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 模型配置实体占位，API Key 后续必须加密存储" */
/**
 * 模型配置实体，保存 AI 服务供应商、模型名称、密钥和默认启用状态。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("model_configs")
public class ModelConfig extends BaseEntity {

    private String provider;

    private String displayName;

    private String baseUrl;

    private String modelName;

    private String apiKeyCiphertext;

    private String usageType;

    @TableField("is_default")
    private Boolean defaultModel;

    private Boolean enabled;

    private Integer contextWindow;

    private Double temperature;

    private Double topP;

    private Boolean supportsJson;

    private Boolean supportsStream;

    private String notes;
}
