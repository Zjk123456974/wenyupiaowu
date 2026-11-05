-- =============================================================
-- 演出阵容 / 嘉宾 模块
-- 详情页「演出阵容」此前是空壳；本迁移新增 activity_lineup 表，
-- 让运营在后台按活动逐条配置嘉宾，详情页按 sort_order 展示。
-- 并写入一份演示数据（仅部分在售活动），方便直接看到效果。
-- =============================================================

CREATE TABLE IF NOT EXISTS activity_lineup (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  activity_id  BIGINT       NOT NULL COMMENT '所属活动',
  name         VARCHAR(100) NOT NULL COMMENT '嘉宾/艺人姓名',
  role         VARCHAR(50)  DEFAULT NULL COMMENT '角色：主演/嘉宾/乐队/主持/特邀等',
  avatar       VARCHAR(500) DEFAULT NULL COMMENT '头像URL，可空',
  description  VARCHAR(500) DEFAULT NULL COMMENT '一句话简介',
  sort_order   INT          DEFAULT 0  COMMENT '展示排序，越小越靠前',
  deleted      INT          DEFAULT 0,
  create_time  DATETIME     DEFAULT NULL,
  update_time  DATETIME     DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_lineup_activity (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动演出阵容/嘉宾';

-- 演示数据：仅当表为空时写入，避免重复执行造成重复
INSERT INTO activity_lineup (activity_id, name, role, avatar, description, sort_order, deleted, create_time, update_time)
SELECT * FROM (
  -- ===== 演唱会（category 1）=====
  SELECT 23,'薛之谦','主演',NULL,'华语流行歌手，《天外来物》世界巡回演唱会主咖',1,0,NOW(),NOW()
  UNION ALL SELECT 23,'巡演乐队','伴奏',NULL,'全程现场伴奏与编曲',2,0,NOW(),NOW()
  UNION ALL SELECT 24,'张杰','主演',NULL,'内地流行男歌手，「未·LIVE」巡回演唱会',1,0,NOW(),NOW()
  UNION ALL SELECT 24,'星空合唱团','特邀',NULL,'终场大合唱特别环节',2,0,NOW(),NOW()
  UNION ALL SELECT 25,'邓紫棋 G.E.M.','主演',NULL,'《I AM GLORIA》世界巡回演唱会',1,0,NOW(),NOW()
  UNION ALL SELECT 25,'弦乐团','伴奏',NULL,'多首经典曲目弦乐编配',2,0,NOW(),NOW()
  UNION ALL SELECT 26,'王力宏','主演',NULL,'《火力全开》巡回演唱会',1,0,NOW(),NOW()
  UNION ALL SELECT 26,'嘻哈嘉宾','特邀',NULL,'特别助阵演出',2,0,NOW(),NOW()
  UNION ALL SELECT 27,'华晨宇','主演',NULL,'「火星演唱会」沉浸式视听',1,0,NOW(),NOW()
  UNION ALL SELECT 27,'舞美视觉团队','舞美',NULL,'打造火星主题舞台',2,0,NOW(),NOW()
  UNION ALL SELECT 28,'五月天','乐队',NULL,'乐团全体成员联袂出演',1,0,NOW(),NOW()
  UNION ALL SELECT 28,'相信音乐','主办',NULL,'巡演出品与制作',2,0,NOW(),NOW()
  UNION ALL SELECT 42,'刀郎','主演',NULL,'《山歌寥哉》世界巡回演唱会',1,0,NOW(),NOW()
  UNION ALL SELECT 50,'陈奕迅 Eason','主演',NULL,'FEAR AND DREAMS 世界巡回演唱会',1,0,NOW(),NOW()
  UNION ALL SELECT 50,'现场乐队','伴奏',NULL,'全程现场伴奏',2,0,NOW(),NOW()

  -- ===== 话剧歌剧（category 2）=====
  UNION ALL SELECT 13,'濮存昕','主演',NULL,'饰演 王利发',1,0,NOW(),NOW()
  UNION ALL SELECT 13,'梁冠华','主演',NULL,'人艺经典班底',2,0,NOW(),NOW()
  UNION ALL SELECT 13,'冯远征','特邀',NULL,'特别出演',3,0,NOW(),NOW()
  UNION ALL SELECT 15,'孟京辉','导演',NULL,'先锋戏剧导演',1,0,NOW(),NOW()
  UNION ALL SELECT 15,'年度主演阵容','主演',NULL,'轮换主演班底',2,0,NOW(),NOW()
  UNION ALL SELECT 29,'赖声川','导演',NULL,'经典话剧导演',1,0,NOW(),NOW()
  UNION ALL SELECT 29,'八小时主演班底','主演',NULL,'史诗剧场全景呈现',2,0,NOW(),NOW()

  -- ===== 艺术展览（category 3）=====
  UNION ALL SELECT 16,'沉浸式策展团队','策展',NULL,'梵高光影特展策展',1,0,NOW(),NOW()
  UNION ALL SELECT 16,'梵高博物馆','出品',NULL,'数字展品授权',2,0,NOW(),NOW()
  UNION ALL SELECT 18,'三星堆博物馆','出品',NULL,'古蜀文明特展',1,0,NOW(),NOW()
  UNION ALL SELECT 18,'考古研究院','学术支持',NULL,'文物解读与学术支持',2,0,NOW(),NOW()

  -- ===== 体育赛事（category 4）=====
  UNION ALL SELECT 19,'全球24支战队','战队',NULL,'CS2 Major 正赛参赛队伍',1,0,NOW(),NOW()
  UNION ALL SELECT 19,'解说嘉宾团','解说',NULL,'全程中文解说',2,0,NOW(),NOW()
  UNION ALL SELECT 39,'卫冕拳王','主赛',NULL,'WBC 世界拳王争霸主赛',1,0,NOW(),NOW()
  UNION ALL SELECT 39,'特邀拳手','副赛',NULL,'垫场赛出战',2,0,NOW(),NOW()

  -- ===== 校园文娱 / 音乐节（category 5）=====
  UNION ALL SELECT 21,'高校乐队联盟','演出',NULL,'多支校园乐队联合出演',1,0,NOW(),NOW()
  UNION ALL SELECT 21,'民谣歌手','嘉宾',NULL,'草坪民谣专场',2,0,NOW(),NOW()
  UNION ALL SELECT 21,'电音DJ','嘉宾',NULL,'黄昏电音派对',3,0,NOW(),NOW()
  UNION ALL SELECT 46,'独立音乐人','舞台A',NULL,'草莓音乐节多舞台演出',1,0,NOW(),NOW()
  UNION ALL SELECT 46,'摇滚乐队','舞台B',NULL,'主舞台摇滚之夜',2,0,NOW(),NOW()
  UNION ALL SELECT 46,'流行歌手','主舞台',NULL,'压轴演出',3,0,NOW(),NOW()
) AS t(activity_id, name, role, avatar, description, sort_order, deleted, create_time, update_time)
WHERE NOT EXISTS (SELECT 1 FROM activity_lineup);
