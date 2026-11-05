package com.ticket.system.controller;

import com.ticket.system.common.Result;
import com.ticket.system.entity.Category;
import com.ticket.system.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 获取分类列表（无需登录）
     */
    @GetMapping("/list")
    public Result<List<Category>> getCategoryList() {
        List<Category> list = categoryService.getCategoryList();
        return Result.success(list);
    }
}
