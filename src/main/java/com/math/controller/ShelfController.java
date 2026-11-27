package com.math.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.math.entity.ShelfAnswer;
import com.math.entity.SysUser;
import com.math.mapper.ShelfAnswerMapper;
import com.math.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义书架 · 学生端
 *
 * <p>学生在页面上调整左右书架的层数与每层书本数后提交，
 * 后台把这两组配置和提交人一起入库；同一学生可以多次提交。</p>
 */
@Controller
public class ShelfController {

    /** 允许的层数范围（与前端一致） */
    private static final int MIN_LAYERS = 1;
    private static final int MAX_LAYERS = 10;
    /** 允许的每层书本数范围（前端不设上限，这里给一个合理上限） */
    private static final int MIN_BOOKS = 1;
    private static final int MAX_BOOKS = 100;

    @Autowired
    private ShelfAnswerMapper answerMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @GetMapping("/learn/bookshelf")
    public String page(Principal principal, Model model) {
        SysUser me = currentUser(principal);
        long count = 0L;
        if (me != null) {
            Long total = answerMapper.selectCount(new LambdaQueryWrapper<ShelfAnswer>()
                    .eq(ShelfAnswer::getStudentId, me.getId()));
            count = total == null ? 0L : total;
        }
        model.addAttribute("me", me);
        model.addAttribute("submitCount", count);
        return "student/bookshelf";
    }

    @PostMapping("/learn/bookshelf/submit")
    @ResponseBody
    public Map<String, Object> submit(@RequestParam(required = false) Integer leftLayers,
                                      @RequestParam(required = false) Integer leftBooks,
                                      @RequestParam(required = false) Integer rightLayers,
                                      @RequestParam(required = false) Integer rightBooks,
                                      Principal principal) {
        if (!validLayers(leftLayers) || !validLayers(rightLayers)
                || !validBooks(leftBooks) || !validBooks(rightBooks)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "层数要 1-10 层、每层 1-100 本哦～");
            return result;
        }

        SysUser me = currentUser(principal);
        ShelfAnswer answer = new ShelfAnswer();
        answer.setLeftLayers(leftLayers);
        answer.setLeftBooks(leftBooks);
        answer.setRightLayers(rightLayers);
        answer.setRightBooks(rightBooks);
        if (me != null) {
            answer.setStudentId(me.getId());
            answer.setStudentName(StringUtils.hasText(me.getNickname()) ? me.getNickname() : me.getUsername());
        }
        answerMapper.insert(answer);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "✅ 已保存：左 " + leftLayers + "层×" + leftBooks
                + "本，右 " + rightLayers + "层×" + rightBooks + "本");
        result.put("id", answer.getId());
        result.put("totalBooks", leftLayers * leftBooks + rightLayers * rightBooks);
        return result;
    }

    private boolean validLayers(Integer value) {
        return value != null && value >= MIN_LAYERS && value <= MAX_LAYERS;
    }

    private boolean validBooks(Integer value) {
        return value != null && value >= MIN_BOOKS && value <= MAX_BOOKS;
    }

    private SysUser currentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, principal.getName()));
    }
}
