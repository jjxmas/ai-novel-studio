ALTER TABLE `model_configs`
  MODIFY COLUMN `api_key_ciphertext` VARCHAR(2048) NOT NULL COMMENT 'API密钥，本地MVP暂存明文，生产环境需改为真实加密存储';
