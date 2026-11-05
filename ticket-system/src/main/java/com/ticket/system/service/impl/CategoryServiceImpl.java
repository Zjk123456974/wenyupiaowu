package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.Category;
import com.ticket.system.mapper.CategoryMapper;
import com.ticket.system.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private com.ticket.system.mapper.ActivityMapper activityMapper;

    @Override
    public List<Category> getCategoryList() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSortOrder);
        return categoryMapper.selectList(wrapper);
    }

    @Override
    public void addCategory(String name, Integer sortOrder) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        // 检查是否已存在同名分类
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, name.trim());
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }
        Category category = new Category();
        category.setName(name.trim());
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }
        category.setStatus(1);
        categoryMapper.insert(category);
    }

    @Override
    public void updateCategory(Long id, String name, Integer sortOrder) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        // 检查是否已存在同名分类（排除自身）
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, name.trim());
        wrapper.ne(Category::getId, id);
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }
        category.setName(name.trim());
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }
        categoryMapper.updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        // 检查是否仍有活动引用该分类
        Long usedCount = activityMapper.selectCount(
                new LambdaQueryWrapper<com.ticket.system.entity.Activity>()
                        .eq(com.ticket.system.entity.Activity::getCategoryId, id));
        if (usedCount != null && usedCount > 0) {
            throw new BusinessException("该分类下还有 " + usedCount + " 个活动，无法删除，请先转移或删除活动");
        }
        categoryMapper.deleteById(id);
    }
}
