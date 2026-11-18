package com.math.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置
 *  - 把项目根目录下的 uploads/ 文件夹映射到 /uploads/** URL，
 *    供富文本编辑器上传的图片静态访问。
 *
 * 说明：
 *  不存到 classpath:static/uploads/ 是因为打包成 jar 后无法写入。
 *  放到项目根目录的 uploads/ 下，开发与部署都可用，并可通过 -Dmath.upload-dir=xxx 覆盖。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 上传文件根目录，默认为项目工作目录下的 uploads */
    private static final String UPLOAD_DIR =
            System.getProperty("math.upload-dir",
                    System.getProperty("user.dir") + File.separator + "uploads");

    public static String getUploadDir() {
        return UPLOAD_DIR;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        // file:/// 协议需要结尾带分隔符
        String location = dir.toURI().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
