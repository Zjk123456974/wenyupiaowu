package com.ticket.system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/page")
public class PageController {

    // 用户前台页面
    @GetMapping({"/", "/index"})
    public String index() { return "index"; }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String register() { return "register"; }

    @GetMapping("/activity/list")
    public String activityList() { return "activity/list"; }

    @GetMapping("/activity/detail")
    public String activityDetail() { return "activity/detail"; }

    @GetMapping("/announcement/detail")
    public String announcementDetail() { return "announcement/detail"; }

    // 用户个人中心
    @GetMapping("/user/center")
    public String userCenter() { return "user/center"; }

    // 订单支付页面
    @GetMapping("/order/pay")
    public String orderPay() { return "order/pay"; }

    @GetMapping("/order/result")
    public String orderResult() { return "order/result"; }

    @GetMapping("/order/list")
    public String orderList() { return "order/list"; }

    // 运营后台
    @GetMapping("/operator/dashboard")
    public String operatorDashboard() { return "operator/dashboard"; }

    @GetMapping("/operator/activities")
    public String operatorActivities() { return "operator/activities"; }

    @GetMapping("/operator/orders")
    public String operatorOrders() { return "operator/orders"; }

    @GetMapping("/operator/announcements")
    public String operatorAnnouncements() { return "operator/announcements"; }

    // 管理员后台
    @GetMapping("/admin/dashboard")
    public String adminDashboard() { return "admin/dashboard"; }

    @GetMapping("/admin/users")
    public String adminUsers() { return "admin/users"; }

    @GetMapping("/admin/activities")
    public String adminActivities() { return "admin/activities"; }

    @GetMapping("/admin/orders")
    public String adminOrders() { return "admin/orders"; }

    @GetMapping("/admin/categories")
    public String adminCategories() { return "admin/categories"; }

    @GetMapping("/admin/banners")
    public String adminBanners() { return "admin/banners"; }

    @GetMapping("/admin/announcements")
    public String adminAnnouncements() { return "admin/announcements"; }

    @GetMapping("/admin/stats")
    public String adminStats() { return "admin/stats"; }
}
