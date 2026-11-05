-- ================================================================
-- 内容更新补丁（2026-10-22）
-- 用途：数据库重建 / 重新灌入种子数据后，执行本脚本即可复现最新的
--      活动标题、分类、场馆、时间、票价、票档与高清真实封面图。
--
-- 背景：本轮把 5 个「0 订单 0 售出」的冷门校园活动替换为热门活动，
--      并把 CS2 等 10 个活动的封面换成网上真实的高清赛事/演出图；
--      同时修复 ticket_price 中残留的旧活动标题（与 activity.title 不一致）。
--
-- 依赖：uploads/ 目录下已存在对应图片文件（见文末清单）
-- ================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------
-- 一、CS2 封面换成 Team Spirit 上海 Major 2024 总冠军合影
-- ---------------------------------------------------------------
UPDATE activity SET
  title = 'CS2 Major 2026·上海大师赛',
  cover_image = '/uploads/cs2_spirit_champions.jpg',
  update_time = NOW()
WHERE title = 'CS2 国际邀请赛·上海站' OR id = 19;

-- 首页轮播图换成 IEM 大奖杯（与活动封面刻意区分）
UPDATE banner SET
  title = 'CS2 Major 2026 · 上海大师赛',
  image_url = '/uploads/banner_iem_trophy.jpg',
  update_time = NOW()
WHERE id = 4 OR title = 'major';

-- ---------------------------------------------------------------
-- 二、无畏契约封面换成 EDG 全球冠军赛夺冠海报
-- ---------------------------------------------------------------
UPDATE activity SET
  title = '无畏契约（VALORANT）2026 上海冠军赛',
  venue = '上海·梅赛德斯奔驰文化中心',
  cover_image = '/uploads/valorant_champions_edg.jpg',
  update_time = NOW()
WHERE id = 20;

-- ---------------------------------------------------------------
-- 三、其余活动封面换成真实高清图
-- ---------------------------------------------------------------
UPDATE activity SET cover_image='/uploads/deng_ziqi_concert_real.jpg',        update_time=NOW() WHERE id=25;
UPDATE activity SET cover_image='/uploads/rumeng_zhimeng_stage.jpg',          update_time=NOW() WHERE id=29;
UPDATE activity SET cover_image='/uploads/anlian_taohuayuan_stage.jpg',       update_time=NOW() WHERE id=30;
UPDATE activity SET cover_image='/uploads/bingmayong_pit1.jpg',               update_time=NOW() WHERE id=33;
UPDATE activity SET cover_image='/uploads/qianli_jiangshan_art.jpg',          update_time=NOW() WHERE id=34;
UPDATE activity SET cover_image='/uploads/shiqu_baojian_hall.jpg',            update_time=NOW() WHERE id=36;
UPDATE activity SET cover_image='/uploads/lpl_finals_stage.jpg',              update_time=NOW() WHERE id=37;

-- ---------------------------------------------------------------
-- 四、冷门校园活动 → 热门活动（保留 activity.id，票档 id 亦保留）
--    这 5 个活动原本 0 订单 0 售出，替换不影响任何历史订单关联
-- ---------------------------------------------------------------

-- 42：2026校园文艺汇演 → 刀郎「山歌寥哉」世界巡回演唱会·北京站
UPDATE activity SET
  title = '刀郎「山歌寥哉」世界巡回演唱会·北京站', category_id = 1,
  venue = '北京·工人体育场', start_time = '2026-11-14 19:30:00', end_time = '2026-11-14 22:00:00',
  price = 480.00, total_tickets = 16000, purchase_limit = 4,
  grab_start_time = '2026-10-24 10:00:00', grab_end_time = '2026-11-14 18:00:00',
  cover_image = '/uploads/daolang_concert_beijing.jpg',
  description = '刀郎「山歌寥哉」世界巡回演唱会北京站，一首《西海情歌》唱响工体，超大舞台、沉浸式灯光与万人合唱，年度华语live演出必看之选。',
  ticket_rule = '每人每单限购4张，开演前1小时停止入场，凭电子票二维码入场。',
  ticket_notice = '凭电子票入场，一人一票对号入座；演出前1小时停止入场。',
  status = 3, update_time = NOW()
WHERE id = 42;

-- 43：校园社团嘉年华 → 舞剧《只此青绿》·北京站
UPDATE activity SET
  title = '舞剧《只此青绿》·北京站', category_id = 2,
  venue = '北京·保利剧院', start_time = '2026-11-20 19:30:00', end_time = '2026-11-20 21:30:00',
  price = 280.00, total_tickets = 1800, purchase_limit = 4,
  grab_start_time = '2026-10-25 10:00:00', grab_end_time = '2026-11-20 18:00:00',
  cover_image = '/uploads/wuzhichengqing_dance.jpg',
  description = '舞剧《只此青绿》北京站，以宋代名画《千里江山图》为灵感，展卷、问篆、唱丝、寻石、习笔、淬墨、入画七个篇章，青年演员以现代舞语演绎千年青绿神话。',
  ticket_rule = '每人每单限购4张，开演前1小时停止入场，凭电子票二维码入场。',
  ticket_notice = '凭电子票入场，一人一票对号入座；演出约120分钟，中场休息15分钟。',
  status = 3, update_time = NOW()
WHERE id = 43;

-- 44：迎新跨年晚会 → 2026新年交响音乐会·国家大剧院
UPDATE activity SET
  title = '2026新年交响音乐会·国家大剧院', category_id = 2,
  venue = '北京·国家大剧院音乐厅', start_time = '2026-12-31 19:30:00', end_time = '2026-12-31 21:30:00',
  price = 380.00, total_tickets = 2400, purchase_limit = 4,
  grab_start_time = '2026-11-20 10:00:00', grab_end_time = '2026-12-31 18:00:00',
  cover_image = '/uploads/symphony_concert_national.jpg',
  description = '2026新年交响音乐会国家大剧院版，首席指挥执棒，跨越东西方的经典与新作，管弦齐鸣与合唱团共同奏响跨年序曲。',
  ticket_rule = '每人每单限购4张，开演前1小时停止入场，凭电子票二维码入场。',
  ticket_notice = '凭电子票入场，一人一票对号入座；迟到观众须服从现场引导安静入场。',
  status = 3, update_time = NOW()
WHERE id = 44;

-- 45：校园戏剧节·原创短剧展演 → 2026世界斯诺克上海大师赛
UPDATE activity SET
  title = '2026世界斯诺克上海大师赛', category_id = 4,
  venue = '上海·上海体育馆', start_time = '2026-11-22 19:30:00', end_time = '2026-11-29 23:00:00',
  price = 180.00, total_tickets = 9000, purchase_limit = 6,
  grab_start_time = '2026-10-25 10:00:00', grab_end_time = '2026-11-22 18:00:00',
  cover_image = '/uploads/snooker_master_shanghai.jpg',
  description = '2026世界斯诺克上海大师赛，顶级斯诺克高手云集上海体育馆，精准走位与母球控制的对决盛宴，观赛体验接近现场。',
  ticket_rule = '每人每单限购6张，比赛日每日有效，凭电子票入场。',
  ticket_notice = '凭电子票入场，一人一票对号入座；场馆内禁止闪光灯与快门声较大的设备。',
  status = 3, update_time = NOW()
WHERE id = 45;

-- 49：高校十大歌手·年度总决赛 → 2026英雄联盟全球总决赛S赛·上海决赛
UPDATE activity SET
  title = '2026英雄联盟全球总决赛S赛·上海决赛', category_id = 4,
  venue = '上海·浦东足球场', start_time = '2026-12-05 16:00:00', end_time = '2026-12-06 23:00:00',
  price = 199.00, total_tickets = 10000, purchase_limit = 6,
  grab_start_time = '2026-11-05 10:00:00', grab_end_time = '2026-12-05 15:00:00',
  cover_image = '/uploads/lol_esports_arena.jpg',
  description = '2026英雄联盟全球总决赛S赛上海决赛，全球顶尖战队鏖战到最后一天，召唤师峡谷的终极对决，WE ARE ONE。',
  ticket_rule = '每人每单限购6张，决赛日现场观赛，凭电子票入场。',
  ticket_notice = '凭电子票入场，一人一票对号入座；赛事现场有大流量，请提前1小时到场。',
  status = 3, update_time = NOW()
WHERE id = 49;

-- ---------------------------------------------------------------
-- 五、票档同步：价格、库存、档位名，以及与 activity 标题对齐
-- ---------------------------------------------------------------
UPDATE ticket_price SET activity_title='刀郎「山歌寥哉」世界巡回演唱会·北京站',   seat_type='看台票', price=480.00, total_stock=8000,  sold_stock=0, seat_index=1 WHERE activity_id=42 AND seat_index=1;
UPDATE ticket_price SET activity_title='刀郎「山歌寥哉」世界巡回演唱会·北京站',   seat_type='内场票', price=680.00, total_stock=5000,  sold_stock=0, seat_index=2 WHERE activity_id=42 AND seat_index=2;
UPDATE ticket_price SET activity_title='刀郎「山歌寥哉」世界巡回演唱会·北京站',   seat_type='VIP票',  price=980.00, total_stock=3000,  sold_stock=0, seat_index=3 WHERE activity_id=42 AND seat_index=3;

UPDATE ticket_price SET activity_title='舞剧《只此青绿》·北京站', seat_type='学生票', price=180.00, total_stock=400,  sold_stock=0, seat_index=1 WHERE activity_id=43 AND seat_index=1;
UPDATE ticket_price SET activity_title='舞剧《只此青绿》·北京站', seat_type='普通票', price=280.00, total_stock=1100, sold_stock=0, seat_index=2 WHERE activity_id=43 AND seat_index=2;
UPDATE ticket_price SET activity_title='舞剧《只此青绿》·北京站', seat_type='VIP票',  price=480.00, total_stock=300,  sold_stock=0, seat_index=3 WHERE activity_id=43 AND seat_index=3;

UPDATE ticket_price SET activity_title='2026新年交响音乐会·国家大剧院', seat_type='学生票', price=280.00, total_stock=400,  sold_stock=0, seat_index=1 WHERE activity_id=44 AND seat_index=1;
UPDATE ticket_price SET activity_title='2026新年交响音乐会·国家大剧院', seat_type='普通票', price=380.00, total_stock=1500, sold_stock=0, seat_index=2 WHERE activity_id=44 AND seat_index=2;
UPDATE ticket_price SET activity_title='2026新年交响音乐会·国家大剧院', seat_type='VIP票',  price=580.00, total_stock=500,  sold_stock=0, seat_index=3 WHERE activity_id=44 AND seat_index=3;

UPDATE ticket_price SET activity_title='2026世界斯诺克上海大师赛', seat_type='学生票', price=128.00, total_stock=2000, sold_stock=0, seat_index=1 WHERE activity_id=45 AND seat_index=1;
UPDATE ticket_price SET activity_title='2026世界斯诺克上海大师赛', seat_type='普通票', price=180.00, total_stock=5000, sold_stock=0, seat_index=2 WHERE activity_id=45 AND seat_index=2;
UPDATE ticket_price SET activity_title='2026世界斯诺克上海大师赛', seat_type='VIP票',  price=380.00, total_stock=2000, sold_stock=0, seat_index=3 WHERE activity_id=45 AND seat_index=3;

UPDATE ticket_price SET activity_title='2026英雄联盟全球总决赛S赛·上海决赛', seat_type='看台票', price=199.00, total_stock=6000, sold_stock=0, seat_index=1 WHERE activity_id=49 AND seat_index=1;
UPDATE ticket_price SET activity_title='2026英雄联盟全球总决赛S赛·上海决赛', seat_type='内场票', price=399.00, total_stock=3000, sold_stock=0, seat_index=2 WHERE activity_id=49 AND seat_index=2;
UPDATE ticket_price SET activity_title='2026英雄联盟全球总决赛S赛·上海决赛', seat_type='VIP票',  price=799.00, total_stock=1000, sold_stock=0, seat_index=3 WHERE activity_id=49 AND seat_index=3;

-- 全库兜底：ticket_price 的活动标题始终与 activity.title 保持一致
-- （CONVERT 规避 utf8mb4_unicode_ci 与 utf8mb4_0900_ai_ci 的排序规则冲突）
UPDATE ticket_price tp JOIN activity a ON a.id = tp.activity_id
SET tp.activity_title = a.title
WHERE CONVERT(tp.activity_title USING utf8mb4) <> CONVERT(a.title USING utf8mb4);

-- ---------------------------------------------------------------
-- 需要的图片文件（置于 ticket-system/uploads/）：
--   cs2_spirit_champions.jpg           Team Spirit 上海 Major 2024 总冠军合影
--   banner_iem_trophy.jpg              IEM 大奖杯（首页轮播）
--   valorant_champions_edg.jpg         EDG 无畏契约全球冠军赛夺冠海报
--   deng_ziqi_concert_real.jpg         邓紫棋演唱会现场
--   rumeng_zhimeng_stage.jpg           话剧《如梦之梦》舞台
--   anlian_taohuayuan_stage.jpg        话剧《暗恋桃花源》舞台
--   bingmayong_pit1.jpg                兵马俑一号坑阵列
--   qianli_jiangshan_art.jpg           《千里江山图》青绿真迹
--   shiqu_baojian_hall.jpg             故宫《石渠宝笈》展厅
--   lpl_finals_stage.jpg               LPL 总决赛现场
--   daolang_concert_beijing.jpg        刀郎演唱会现场
--   wuzhichengqing_dance.jpg           舞剧《只此青绿》剧照
--   symphony_concert_national.jpg      国家大剧院交响音乐会现场
--   snooker_master_shanghai.jpg        斯诺克大师赛现场
--   lol_esports_arena.jpg              英雄联盟职业联赛赛场
--   strawberry_music_festival_stage.jpg 草莓音乐节现场
-- ================================================================
