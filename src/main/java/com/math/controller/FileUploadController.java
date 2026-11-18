package com.math.controller;

import com.math.config.WebConfig;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 富文本编辑器图片上传接口
 *
 *  - 前端通过隐藏 file input 选择图片后，POST 到 /upload/image
 *  - 服务端保存到项目根目录 uploads/yyyy/MM/ 下，返回 { errno: 0, url: '/uploads/...' }
 *  - errno=0 表示成功（与常见富文本插件约定的字段名保持一致）
 *
 * 安全限制：
 *  - 仅允许 jpg/png/gif/webp/bmp
 *  - 单文件最大 5MB
 */
@RestController
public class FileUploadController {

    private static final long MAX_SIZE = 5L * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXT = {"jpg", "jpeg", "png", "gif", "webp", "bmp"};

    @PostMapping("/upload/image")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        if (file == null || file.isEmpty()) {
            result.put("errno", 1);
            result.put("message", "请选择图片文件");
            return result;
        }
        if (file.getSize() > MAX_SIZE) {
            result.put("errno", 2);
            result.put("message", "图片不能超过 5MB");
            return result;
        }
        String originName = file.getOriginalFilename();
        String ext = getExtension(originName);
        if (!isAllowed(ext)) {
            result.put("errno", 3);
            result.put("message", "仅支持 jpg/png/gif/webp/bmp 格式");
            return result;
        }
        // 按日期分目录：uploads/2026/11/xxx.jpg
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File dir = new File(WebConfig.getUploadDir(), datePath);
        if (!dir.exists() && !dir.mkdirs()) {
            result.put("errno", 4);
            result.put("message", "创建上传目录失败");
            return result;
        }
        File dest = new File(dir, fileName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            result.put("errno", 5);
            result.put("message", "保存失败：" + e.getMessage());
            return result;
        }
        // 返回可访问的 URL（context-path 为 /，直接 /uploads 开头）
        String url = "/uploads/" + datePath + "/" + fileName;
        result.put("errno", 0);
        result.put("url", url);
        result.put("message", "上传成功");
        return result;
    }

    private String getExtension(String name) {
        if (name == null) return "";
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) return "";
        return name.substring(idx + 1).toLowerCase();
    }

    private boolean isAllowed(String ext) {
        for (String a : ALLOWED_EXT) {
            if (a.equals(ext)) return true;
        }
        return false;
    }
}
