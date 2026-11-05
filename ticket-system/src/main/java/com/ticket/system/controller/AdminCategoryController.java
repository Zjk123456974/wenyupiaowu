package com.ticket.system.controller;

import com.ticket.system.common.BusinessException;
import com.ticket.system.annotation.OperLog;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/category")
public class AdminCategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RequestContext requestContext;

    /**
     * 新增分类
     */
    @OperLog(module = "分类管理", operation = "新增分类")
    @PostMapping
    public Result<Void> addCategory(@RequestBody CategoryNameRequest request) {
        checkAdmin();
        categoryService.addCategory(request.getName(), request.getSortOrder());
        return Result.success();
    }

    /**
     * 编辑分类
     */
    @OperLog(module = "分类管理", operation = "编辑分类")
    @PutMapping("/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody CategoryNameRequest request) {
        checkAdmin();
        categoryService.updateCategory(id, request.getName(), request.getSortOrder());
        return Result.success();
    }

    /**
     * 删除分类
     */
    @OperLog(module = "分类管理", operation = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        checkAdmin();
        categoryService.deleteCategory(id);
        return Result.success();
    }

    /**
     * 校验当前用户是否为管理员（role==2）
     */
    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无权限操作");
        }
    }

    /**
     * 分类名称请求体
     */
    @lombok.Data
    public static class CategoryNameRequest {
        private String name;
        private Integer sortOrder;
    }
}
