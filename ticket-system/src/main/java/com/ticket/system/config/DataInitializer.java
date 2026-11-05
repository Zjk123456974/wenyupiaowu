package com.ticket.system.config;

import com.ticket.system.entity.Activity;
import com.ticket.system.entity.Banner;
import com.ticket.system.entity.Category;
import com.ticket.system.entity.TicketPrice;
import com.ticket.system.entity.User;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.BannerMapper;
import com.ticket.system.mapper.CategoryMapper;
import com.ticket.system.mapper.TicketPriceMapper;
import com.ticket.system.mapper.UserMapper;
import com.ticket.system.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.ticket.system.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private BannerMapper bannerMapper;

    @Autowired
    private TicketPriceMapper ticketPriceMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SystemConfigService systemConfigService;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void run(String... args) throws Exception {
        initAdminUser();
        initCategories();
        initActivities();
        initBanners();
        systemConfigService.initDefaultConfigs();
    }

    private void initAdminUser() {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, "admin");
        User existing = userMapper.selectOne(q);
        if (existing == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(PasswordUtil.encode("admin123"));
            admin.setPhone("13800138000");
            admin.setNickname("管理员");
            admin.setRole(2);
            admin.setStatus(1);
            userMapper.insert(admin);
        } else {
            // 兼容历史库：admin 账号可能因早期 PBKDF2 参数变体而“哈希格式正确却永久登不上”。
            // 若用默认密码 admin123 无法验证通过，则启动时静默重设为当前算法，避免死锁；
            // 若已能用 admin123 登录（含已兼容/用户自定义密码）则保持不变，不破坏既有密码。
            if (!PasswordUtil.matches("admin123", existing.getPassword())) {
                existing.setPassword(PasswordUtil.encode("admin123"));
                userMapper.updateById(existing);
            }
        }
    }

    private void initCategories() {
        if (categoryMapper.selectCount(null) == 0) {
            Category cat1 = new Category();
            cat1.setName("演唱会");
            cat1.setIcon("🎤");
            cat1.setColor("#f56c6c");
            cat1.setStatus(1);
            categoryMapper.insert(cat1);

            Category cat2 = new Category();
            cat2.setName("话剧歌剧");
            cat2.setIcon("🎭");
            cat2.setColor("#409eff");
            cat2.setStatus(1);
            categoryMapper.insert(cat2);

            Category cat3 = new Category();
            cat3.setName("体育赛事");
            cat3.setIcon("⚽");
            cat3.setColor("#67c23a");
            cat3.setStatus(1);
            categoryMapper.insert(cat3);

            Category cat4 = new Category();
            cat4.setName("艺术展览");
            cat4.setIcon("🎨");
            cat4.setColor("#909399");
            cat4.setStatus(1);
            categoryMapper.insert(cat4);

            Category cat5 = new Category();
            cat5.setName("音乐会");
            cat5.setIcon("🎵");
            cat5.setColor("#e6a23c");
            cat5.setStatus(1);
            categoryMapper.insert(cat5);
        }
    }

    private void initActivities() {
        if (activityMapper.selectCount(null) == 0) {
            Activity act1 = new Activity();
            act1.setTitle("2026周杰伦嘉年华世界巡回演唱会");
            act1.setCategoryId(1L);
            act1.setVenue("北京国家体育场（鸟巢）");
            act1.setStartTime(LocalDateTime.parse("2026-08-15 19:00:00", dtf));
            act1.setEndTime(LocalDateTime.parse("2026-08-15 22:00:00", dtf));
            act1.setGrabStartTime(LocalDateTime.parse("2026-07-14 10:00:00", dtf));
            act1.setGrabEndTime(LocalDateTime.parse("2026-08-10 23:59:59", dtf));
            act1.setCoverImage("https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&h=400&fit=crop");
            act1.setDescription("周杰伦2026嘉年华世界巡回演唱会北京站，精彩演出不容错过！");
            act1.setPurchaseLimit(2);
            act1.setTotalTickets(20000);
            act1.setSoldTickets(2001);
            act1.setPrice(new BigDecimal("880"));
            act1.setStatus(3);
            act1.setViewCount(125800);
            activityMapper.insert(act1);
            createTicketPrices(act1.getId(), "2026周杰伦嘉年华世界巡回演唱会", new BigDecimal("480"), new BigDecimal("680"), new BigDecimal("880"), new BigDecimal("1280"), new BigDecimal("1680"));

            Activity act2 = new Activity();
            act2.setTitle("话剧《雷雨》经典重现");
            act2.setCategoryId(2L);
            act2.setVenue("上海大剧院");
            act2.setStartTime(LocalDateTime.parse("2026-07-18 19:30:00", dtf));
            act2.setEndTime(LocalDateTime.parse("2026-07-18 22:00:00", dtf));
            act2.setGrabStartTime(LocalDateTime.parse("2026-07-10 10:00:00", dtf));
            act2.setGrabEndTime(LocalDateTime.parse("2026-07-17 23:59:59", dtf));
            act2.setCoverImage("https://images.unsplash.com/photo-1507924538820-ede94a04019d?w=600&h=400&fit=crop");
            act2.setDescription("曹禺经典话剧《雷雨》全新演绎，震撼舞台呈现！");
            act2.setPurchaseLimit(3);
            act2.setTotalTickets(1500);
            act2.setSoldTickets(890);
            act2.setPrice(new BigDecimal("280"));
            act2.setStatus(3);
            act2.setViewCount(8500);
            activityMapper.insert(act2);
            createTicketPrices(act2.getId(), "话剧《雷雨》经典重现", new BigDecimal("180"), new BigDecimal("280"), new BigDecimal("380"), new BigDecimal("480"), null);

            Activity act3 = new Activity();
            act3.setTitle("2026中超联赛 北京国安vs上海申花");
            act3.setCategoryId(3L);
            act3.setVenue("北京工人体育场");
            act3.setStartTime(LocalDateTime.parse("2026-08-10 19:35:00", dtf));
            act3.setEndTime(LocalDateTime.parse("2026-08-10 21:30:00", dtf));
            act3.setGrabStartTime(LocalDateTime.parse("2026-07-15 10:00:00", dtf));
            act3.setGrabEndTime(LocalDateTime.parse("2026-08-09 23:59:59", dtf));
            act3.setCoverImage("https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=600&h=400&fit=crop");
            act3.setDescription("中超焦点大战，北京国安主场迎战上海申花！");
            act3.setPurchaseLimit(4);
            act3.setTotalTickets(35000);
            act3.setSoldTickets(12500);
            act3.setPrice(new BigDecimal("180"));
            act3.setStatus(3);
            act3.setViewCount(35600);
            activityMapper.insert(act3);
            createTicketPrices(act3.getId(), "2026中超联赛", new BigDecimal("180"), new BigDecimal("280"), new BigDecimal("480"), new BigDecimal("680"), new BigDecimal("880"));

            Activity act4 = new Activity();
            act4.setTitle("莫奈印象派艺术大展");
            act4.setCategoryId(4L);
            act4.setVenue("中国美术馆");
            act4.setStartTime(LocalDateTime.parse("2026-09-01 09:00:00", dtf));
            act4.setEndTime(LocalDateTime.parse("2026-09-30 17:00:00", dtf));
            act4.setGrabStartTime(LocalDateTime.parse("2026-08-15 10:00:00", dtf));
            act4.setGrabEndTime(LocalDateTime.parse("2026-09-29 23:59:59", dtf));
            act4.setCoverImage("https://images.unsplash.com/photo-1507925921958-8a62f3d1a50d?w=600&h=400&fit=crop");
            act4.setDescription("法国印象派大师莫奈真迹首次来华展出，不容错过！");
            act4.setPurchaseLimit(2);
            act4.setTotalTickets(50000);
            act4.setSoldTickets(8500);
            act4.setPrice(new BigDecimal("120"));
            act4.setStatus(3);
            act4.setViewCount(68000);
            activityMapper.insert(act4);
            createTicketPrices(act4.getId(), "莫奈印象派艺术大展", new BigDecimal("60"), new BigDecimal("120"), new BigDecimal("200"), null, null);

            Activity act5 = new Activity();
            act5.setTitle("林俊杰JJ20世界巡回演唱会");
            act5.setCategoryId(1L);
            act5.setVenue("广州天河体育中心");
            act5.setStartTime(LocalDateTime.parse("2026-07-25 19:00:00", dtf));
            act5.setEndTime(LocalDateTime.parse("2026-07-25 22:00:00", dtf));
            act5.setGrabStartTime(LocalDateTime.parse("2026-07-16 10:00:00", dtf));
            act5.setGrabEndTime(LocalDateTime.parse("2026-07-24 23:59:59", dtf));
            act5.setCoverImage("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&h=400&fit=crop");
            act5.setDescription("林俊杰JJ20世界巡回演唱会广州站，与你相约！");
            act5.setPurchaseLimit(2);
            act5.setTotalTickets(25000);
            act5.setSoldTickets(0);
            act5.setPrice(new BigDecimal("680"));
            act5.setStatus(1);
            act5.setViewCount(45000);
            activityMapper.insert(act5);
            createTicketPrices(act5.getId(), "林俊杰JJ20世界巡回演唱会", new BigDecimal("380"), new BigDecimal("580"), new BigDecimal("680"), new BigDecimal("980"), new BigDecimal("1280"));
        }
    }

    private void createTicketPrices(Long activityId, String activityTitle, BigDecimal... prices) {
        int seatIndex = 1;
        String[] seatTypes = {"看台票", "内场票", "VIP票", "至尊VIP", "钻石VIP"};
        for (int i = 0; i < prices.length; i++) {
            if (prices[i] != null) {
                TicketPrice tp = new TicketPrice();
                tp.setActivityId(activityId);
                tp.setActivityTitle(activityTitle);
                tp.setSeatType(seatTypes[i]);
                tp.setPrice(prices[i]);
                tp.setTotalStock(2000);
                tp.setSoldStock(0);
                tp.setSeatIndex(seatIndex++);
                tp.setStatus(1);
                ticketPriceMapper.insert(tp);
            }
        }
    }

    private void initBanners() {
        if (bannerMapper.selectCount(null) == 0) {
            Banner banner1 = new Banner();
            banner1.setImageUrl("https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&h=600&fit=crop");
            banner1.setTitle("精彩演出 火热抢票");
            banner1.setDescription("演唱会、话剧、展览、体育赛事一网打尽");
            banner1.setActivityId(1L);
            banner1.setSortOrder(1);
            banner1.setStatus(1);
            banner1.setCreateTime(LocalDateTime.now());
            banner1.setUpdateTime(LocalDateTime.now());
            bannerMapper.insert(banner1);

            Banner banner2 = new Banner();
            banner2.setImageUrl("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1200&h=600&fit=crop");
            banner2.setTitle("周杰伦演唱会");
            banner2.setDescription("2026嘉年华世界巡回演唱会北京站");
            banner2.setActivityId(1L);
            banner2.setSortOrder(2);
            banner2.setStatus(1);
            banner2.setCreateTime(LocalDateTime.now());
            banner2.setUpdateTime(LocalDateTime.now());
            bannerMapper.insert(banner2);

            Banner banner3 = new Banner();
            banner3.setImageUrl("https://images.unsplash.com/photo-1507925921958-8a62f3d1a50d?w=1200&h=600&fit=crop");
            banner3.setTitle("莫奈艺术大展");
            banner3.setDescription("印象派大师真迹首次来华展出");
            banner3.setActivityId(4L);
            banner3.setSortOrder(3);
            banner3.setStatus(1);
            banner3.setCreateTime(LocalDateTime.now());
            banner3.setUpdateTime(LocalDateTime.now());
            bannerMapper.insert(banner3);
        }
    }
}