-- 演示用 Open API Key（生产环境请更换并妥善保管 api_secret）
INSERT INTO api_key (app_name, api_key, api_secret, status, created_user_id, updated_user_id, deleted)
SELECT 'demo-app', 'aios-demo-key-change-me', 'demo-secret', 1, 0, 0, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM api_key WHERE api_key = 'aios-demo-key-change-me' AND deleted = 0);
