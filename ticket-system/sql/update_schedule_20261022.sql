-- 活动时间重排 + 封面/轮播更新（幂等，可重复执行）
-- 注意：自 2026-10-22 起，活动日期改由后端「滚动排期」动态维护
--   （登录成功触发 + 每日03:00定时任务，见 ActivityServiceImpl.rescheduleRolling）。
--   本脚本仅作初始种子/封面与轮播图修复用途；其中的静态重排日期会被动态排期覆盖，无冲突。
-- 用途：重建库后执行一次，即可复现 2026-10-22 的排期与图片改动

-- 活动时间重排（交错日历 2026-10 ~ 2027-06），幂等可重跑
SET NAMES utf8mb4;
SET @dummy = 0;

-- 删除重复活动 45 / 57 及其票档
DELETE FROM ticket_price WHERE activity_id IN (45,57);
DELETE FROM activity WHERE id IN (45,57);

UPDATE activity SET title = '2027周杰伦嘉年华世界巡回演唱会', start_time = '2027-01-10 19:30:00', end_time = '2027-01-10 22:30:00', grab_start_time = '2026-12-11 10:00:00', grab_end_time = '2027-01-10 18:00:00', status = 3, update_time = NOW() WHERE id = 1;
UPDATE activity SET title = '2027话剧《雷雨》经典重现', start_time = '2027-02-21 19:30:00', end_time = '2027-02-21 22:30:00', grab_start_time = '2027-01-22 10:00:00', grab_end_time = '2027-02-21 18:00:00', status = 3, update_time = NOW() WHERE id = 2;
UPDATE activity SET start_time = '2027-03-01 10:00:00', end_time = '2027-04-20 18:00:00', grab_start_time = '2027-01-30 10:00:00', grab_end_time = '2027-04-20 18:00:00', status = 3, update_time = NOW() WHERE id = 3;
UPDATE activity SET title = '2027中超联赛 北京国安vs上海申花', start_time = '2027-05-01 19:30:00', end_time = '2027-05-01 22:30:00', grab_start_time = '2027-04-01 10:00:00', grab_end_time = '2027-05-01 18:00:00', status = 3, update_time = NOW() WHERE id = 4;
UPDATE activity SET title = '2027校园十佳歌手大赛总决赛', start_time = '2027-06-12 19:30:00', end_time = '2027-06-12 22:30:00', grab_start_time = '2027-05-13 10:00:00', grab_end_time = '2027-06-12 18:00:00', status = 3, update_time = NOW() WHERE id = 5;
UPDATE activity SET title = '2027林俊杰JJ20世界巡回演唱会', start_time = '2027-01-17 19:30:00', end_time = '2027-01-17 22:30:00', grab_start_time = '2026-12-18 10:00:00', grab_end_time = '2027-01-17 18:00:00', status = 3, update_time = NOW() WHERE id = 6;
UPDATE activity SET title = '范丞丞2027个人巡回演唱会-北京', start_time = '2027-05-15 19:30:00', end_time = '2027-05-15 22:30:00', grab_start_time = '2027-04-15 10:00:00', grab_end_time = '2027-05-15 18:00:00', status = 3, update_time = NOW() WHERE id = 7;
UPDATE activity SET title = '范丞丞2027个人巡回演唱会-上海', start_time = '2027-05-22 19:30:00', end_time = '2027-05-22 22:30:00', grab_start_time = '2027-04-22 10:00:00', grab_end_time = '2027-05-22 18:00:00', status = 3, update_time = NOW() WHERE id = 8;
UPDATE activity SET title = '杨丞琳2027世界巡回演唱会-广州', start_time = '2027-05-29 19:30:00', end_time = '2027-05-29 22:30:00', grab_start_time = '2027-04-29 10:00:00', grab_end_time = '2027-05-29 18:00:00', status = 3, update_time = NOW() WHERE id = 9;
UPDATE activity SET title = '杨丞琳2027 Live音乐会-成都', start_time = '2027-06-05 19:30:00', end_time = '2027-06-05 22:30:00', grab_start_time = '2027-05-06 10:00:00', grab_end_time = '2027-06-05 18:00:00', status = 3, update_time = NOW() WHERE id = 10;
UPDATE activity SET title = '2027话剧《茶馆》人艺经典重演', start_time = '2027-02-07 19:30:00', end_time = '2027-02-07 22:30:00', grab_start_time = '2027-01-08 10:00:00', grab_end_time = '2027-02-07 18:00:00', status = 3, update_time = NOW() WHERE id = 13;
UPDATE activity SET start_time = '2026-10-24 19:30:00', end_time = '2026-10-24 22:30:00', grab_start_time = '2026-09-24 10:00:00', grab_end_time = '2026-10-24 18:00:00', status = 3, update_time = NOW() WHERE id = 14;
UPDATE activity SET start_time = '2026-11-06 19:30:00', end_time = '2026-11-06 22:30:00', grab_start_time = '2026-10-07 10:00:00', grab_end_time = '2026-11-06 18:00:00', status = 3, update_time = NOW() WHERE id = 15;
UPDATE activity SET start_time = '2027-03-10 10:00:00', end_time = '2027-04-24 18:00:00', grab_start_time = '2027-02-08 10:00:00', grab_end_time = '2027-04-24 18:00:00', status = 3, update_time = NOW() WHERE id = 16;
UPDATE activity SET start_time = '2027-03-20 10:00:00', end_time = '2027-05-04 18:00:00', grab_start_time = '2027-02-18 10:00:00', grab_end_time = '2027-05-04 18:00:00', status = 3, update_time = NOW() WHERE id = 17;
UPDATE activity SET start_time = '2026-11-01 10:00:00', end_time = '2026-12-21 18:00:00', grab_start_time = '2026-10-02 10:00:00', grab_end_time = '2026-12-21 18:00:00', status = 3, update_time = NOW() WHERE id = 18;
UPDATE activity SET start_time = '2026-11-07 19:30:00', end_time = '2026-11-07 22:30:00', grab_start_time = '2026-10-08 10:00:00', grab_end_time = '2026-11-07 18:00:00', status = 3, update_time = NOW() WHERE id = 19;
UPDATE activity SET start_time = '2026-10-31 19:30:00', end_time = '2026-10-31 22:30:00', grab_start_time = '2026-10-01 10:00:00', grab_end_time = '2026-10-31 18:00:00', status = 3, update_time = NOW() WHERE id = 20;
UPDATE activity SET title = '2027毕业季草坪音乐节', start_time = '2027-06-19 19:30:00', end_time = '2027-06-19 22:30:00', grab_start_time = '2027-05-20 10:00:00', grab_end_time = '2027-06-19 18:00:00', status = 3, update_time = NOW() WHERE id = 21;
UPDATE activity SET title = '2027大学生街舞挑战赛', start_time = '2027-06-26 19:30:00', end_time = '2027-06-26 22:30:00', grab_start_time = '2027-05-27 10:00:00', grab_end_time = '2027-06-26 18:00:00', status = 3, update_time = NOW() WHERE id = 22;
UPDATE activity SET title = '2027薛之谦「天外来物」世界巡回演唱会·广州站', start_time = '2027-01-24 19:30:00', end_time = '2027-01-24 22:30:00', grab_start_time = '2026-12-25 10:00:00', grab_end_time = '2027-01-24 18:00:00', status = 3, update_time = NOW() WHERE id = 23;
UPDATE activity SET start_time = '2026-11-07 19:30:00', end_time = '2026-11-07 22:30:00', grab_start_time = '2026-10-08 10:00:00', grab_end_time = '2026-11-07 18:00:00', status = 3, update_time = NOW() WHERE id = 24;
UPDATE activity SET start_time = '2026-11-21 19:30:00', end_time = '2026-11-21 22:30:00', grab_start_time = '2026-10-22 10:00:00', grab_end_time = '2026-11-21 18:00:00', status = 3, update_time = NOW() WHERE id = 25;
UPDATE activity SET start_time = '2026-12-12 19:30:00', end_time = '2026-12-12 22:30:00', grab_start_time = '2026-11-12 10:00:00', grab_end_time = '2026-12-12 18:00:00', status = 3, update_time = NOW() WHERE id = 26;
UPDATE activity SET title = '2027华晨宇「火星演唱会」·成都站', start_time = '2027-01-31 19:30:00', end_time = '2027-01-31 22:30:00', grab_start_time = '2027-01-01 10:00:00', grab_end_time = '2027-01-31 18:00:00', status = 3, update_time = NOW() WHERE id = 27;
UPDATE activity SET start_time = '2026-11-28 19:30:00', end_time = '2026-11-28 22:30:00', grab_start_time = '2026-10-29 10:00:00', grab_end_time = '2026-11-28 18:00:00', status = 3, update_time = NOW() WHERE id = 28;
UPDATE activity SET start_time = '2026-11-20 19:30:00', end_time = '2026-11-20 22:30:00', grab_start_time = '2026-10-21 10:00:00', grab_end_time = '2026-11-20 18:00:00', status = 3, update_time = NOW() WHERE id = 29;
UPDATE activity SET start_time = '2026-12-05 19:30:00', end_time = '2026-12-05 22:30:00', grab_start_time = '2026-11-05 10:00:00', grab_end_time = '2026-12-05 18:00:00', status = 3, update_time = NOW() WHERE id = 30;
UPDATE activity SET start_time = '2027-02-14 19:30:00', end_time = '2027-02-14 22:30:00', grab_start_time = '2027-01-15 10:00:00', grab_end_time = '2027-02-14 18:00:00', status = 3, update_time = NOW() WHERE id = 31;
UPDATE activity SET start_time = '2026-12-19 19:30:00', end_time = '2026-12-19 22:30:00', grab_start_time = '2026-11-19 10:00:00', grab_end_time = '2026-12-19 18:00:00', status = 3, update_time = NOW() WHERE id = 32;
UPDATE activity SET start_time = '2027-04-01 10:00:00', end_time = '2027-05-21 18:00:00', grab_start_time = '2027-03-02 10:00:00', grab_end_time = '2027-05-21 18:00:00', status = 3, update_time = NOW() WHERE id = 33;
UPDATE activity SET start_time = '2027-04-10 10:00:00', end_time = '2027-05-25 18:00:00', grab_start_time = '2027-03-11 10:00:00', grab_end_time = '2027-05-25 18:00:00', status = 3, update_time = NOW() WHERE id = 34;
UPDATE activity SET start_time = '2026-11-07 10:00:00', end_time = '2026-12-22 18:00:00', grab_start_time = '2026-10-08 10:00:00', grab_end_time = '2026-12-22 18:00:00', status = 3, update_time = NOW() WHERE id = 35;
UPDATE activity SET start_time = '2027-04-20 10:00:00', end_time = '2027-06-04 18:00:00', grab_start_time = '2027-03-21 10:00:00', grab_end_time = '2027-06-04 18:00:00', status = 3, update_time = NOW() WHERE id = 36;
UPDATE activity SET start_time = '2026-10-30 19:30:00', end_time = '2026-10-30 22:30:00', grab_start_time = '2026-09-30 10:00:00', grab_end_time = '2026-10-30 18:00:00', status = 3, update_time = NOW() WHERE id = 37;
UPDATE activity SET start_time = '2026-11-15 19:30:00', end_time = '2026-11-15 22:30:00', grab_start_time = '2026-10-16 10:00:00', grab_end_time = '2026-11-15 18:00:00', status = 3, update_time = NOW() WHERE id = 38;
UPDATE activity SET start_time = '2026-12-06 19:30:00', end_time = '2026-12-06 22:30:00', grab_start_time = '2026-11-06 10:00:00', grab_end_time = '2026-12-06 18:00:00', status = 3, update_time = NOW() WHERE id = 39;
UPDATE activity SET title = '2027 WTT乒乓球大满贯·北京站', start_time = '2027-05-08 19:30:00', end_time = '2027-05-08 22:30:00', grab_start_time = '2027-04-08 10:00:00', grab_end_time = '2027-05-08 18:00:00', status = 3, update_time = NOW() WHERE id = 40;
UPDATE activity SET start_time = '2026-11-22 19:30:00', end_time = '2026-11-22 22:30:00', grab_start_time = '2026-10-23 10:00:00', grab_end_time = '2026-11-22 18:00:00', status = 3, update_time = NOW() WHERE id = 41;
UPDATE activity SET start_time = '2026-11-14 19:30:00', end_time = '2026-11-14 22:30:00', grab_start_time = '2026-10-15 10:00:00', grab_end_time = '2026-11-14 18:00:00', status = 3, update_time = NOW() WHERE id = 42;
UPDATE activity SET start_time = '2026-11-20 19:30:00', end_time = '2026-11-20 22:30:00', grab_start_time = '2026-10-21 10:00:00', grab_end_time = '2026-11-20 18:00:00', status = 3, update_time = NOW() WHERE id = 43;
UPDATE activity SET start_time = '2026-12-31 19:30:00', end_time = '2026-12-31 22:30:00', grab_start_time = '2026-12-01 10:00:00', grab_end_time = '2026-12-31 18:00:00', status = 3, update_time = NOW() WHERE id = 44;
UPDATE activity SET start_time = '2026-10-31 19:30:00', end_time = '2026-10-31 22:30:00', grab_start_time = '2026-10-01 10:00:00', grab_end_time = '2026-10-31 18:00:00', status = 3, update_time = NOW() WHERE id = 46;
UPDATE activity SET start_time = '2026-12-20 19:30:00', end_time = '2026-12-20 22:30:00', grab_start_time = '2026-11-20 10:00:00', grab_end_time = '2026-12-20 18:00:00', status = 3, update_time = NOW() WHERE id = 47;
UPDATE activity SET start_time = '2026-12-01 19:30:00', end_time = '2026-12-01 22:30:00', grab_start_time = '2026-11-01 10:00:00', grab_end_time = '2026-12-01 18:00:00', status = 3, update_time = NOW() WHERE id = 48;
UPDATE activity SET start_time = '2026-12-05 19:30:00', end_time = '2026-12-05 22:30:00', grab_start_time = '2026-11-05 10:00:00', grab_end_time = '2026-12-05 18:00:00', status = 3, update_time = NOW() WHERE id = 49;
UPDATE activity SET start_time = '2026-11-15 19:30:00', end_time = '2026-11-15 22:30:00', grab_start_time = '2026-10-16 10:00:00', grab_end_time = '2026-11-15 18:00:00', status = 3, update_time = NOW() WHERE id = 50;

-- 同步票档标题（覆盖所有年份校正）
UPDATE ticket_price tp JOIN activity a ON a.id = tp.activity_id
SET tp.activity_title = a.title WHERE CONVERT(tp.activity_title USING utf8mb4) <> CONVERT(a.title USING utf8mb4);

-- 高清/匹配封面替换（1280x720），幂等
UPDATE activity SET cover_image='/uploads/chaguan_cover.jpg'            WHERE id=13;  -- 茶馆（AI高清水墨茶馆，去水印）
UPDATE activity SET cover_image='/uploads/xiniu_cover.jpg'              WHERE id=15;  -- 恋爱的犀牛（红犀牛舞台）
UPDATE activity SET cover_image='/uploads/fangao_hd.jpg'                WHERE id=16;  -- 梵高（真实自画像）
UPDATE activity SET cover_image='/uploads/dunhuang_cover.jpg'           WHERE id=17;  -- 敦煌飞天
UPDATE activity SET cover_image='/uploads/sanxingdui_cover.jpg'         WHERE id=18;  -- 三星堆铜面具
UPDATE activity SET cover_image='/uploads/caodi_cover.jpg'              WHERE id=21;  -- 草坪音乐节
UPDATE activity SET cover_image='/uploads/wang_lihong_concert_cover.jpg' WHERE id=26; -- 王力宏（原为座位图，已重做）
UPDATE activity SET cover_image='/uploads/zhichiciqinglv_cover.jpg'     WHERE id=43;  -- 只此青绿（青绿水墨舞台实拍）

-- 轮播图：去掉无关的世界杯/小黄人，改为真实热门活动
UPDATE banner SET title='薛之谦2027巡回演唱会·广州站', image_url='/uploads/xue_zhiqian_guangzhou.jpg', activity_id=23, update_time=NOW() WHERE id=2;
UPDATE banner SET title='2026上海网球大师赛',           image_url='/uploads/shanghai_tennis.jpg',      activity_id=48, update_time=NOW() WHERE id=3;
UPDATE banner SET activity_id=19, update_time=NOW() WHERE id=4;
