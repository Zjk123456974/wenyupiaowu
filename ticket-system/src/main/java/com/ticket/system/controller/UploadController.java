package com.ticket.system.controller;

import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
public class UploadController {

    private static final String UPLOAD_DIR = "uploads";
    private static final String BASE_URL = "/uploads/";
    /** 单文件上限 5MB（Spring 另有 10MB 兜底，这里给出更清晰的业务限制） */
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    @Resource
    private RequestContext requestContext;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        // 必须登录：头像由普通用户上传，海报/广告由管理员上传，但共用此端点，至少要求已登录，杜绝匿名滥用
        if (requestContext.getCurrentUserId() == null) {
            throw new BusinessException(401, "请先登录后再上传文件");
        }
        if (file.isEmpty()) {
            return Result.error("上传文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.error("文件过大，请上传不超过 5MB 的图片");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isImageFile(originalFilename)) {
            return Result.error("仅支持常见图片格式（jpg/jpeg/png/gif/webp/bmp/avif）");
        }

        try {
            // 仅截取最后一个点之后的扩展名并丢弃原始文件名其余部分，天然阻断路径穿越（../）
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + extension;

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 仅用生成的文件名 resolve，绝不使用原始文件名拼接，杜绝目录穿越
            Path filePath = uploadPath.resolve(newFilename).normalize().toAbsolutePath();
            Path absUploadDir = uploadPath.toAbsolutePath().normalize();
            if (!filePath.startsWith(absUploadDir)) {
                return Result.error("非法文件路径");
            }
            Files.copy(file.getInputStream(), filePath);

            String fileUrl = BASE_URL + newFilename;
            return Result.success(fileUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败");
        }
    }

    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        // 注意：不允许 svg —— SVG 可内嵌脚本，作为静态资源返回时会触发存储型 XSS
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".webp") || lower.endsWith(".bmp") ||
               lower.endsWith(".avif");
    }
}