package com.ticket.system.service;

import com.ticket.system.entity.Category;

import java.util.List;

public interface CategoryService {

    /**
     * 获取所有分类
     */
    List<Category> getCategoryList();

    /**
     * 新增分类
     */
    void addCategory(String name, Integer sortOrder);

    /**
     * 编辑分类
     */
    void updateCategory(Long id, String name, Integer sortOrder);

    /**
     * 删除分类
     */
    void deleteCategory(Long id);
}
