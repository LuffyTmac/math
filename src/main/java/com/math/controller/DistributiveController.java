package com.math.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.math.entity.DistributiveAnswer;
import com.math.entity.DistributiveQuestion;
import com.math.entity.SysUser;
import com.math.mapper.DistributiveAnswerMapper;
import com.math.mapper.DistributiveQuestionMapper;
import com.math.mapper.SysUserMapper;
import com.math.util.ExpressionValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.security.Principal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 乘法分配律 · 学生端控制器
 *
 * <p>学习页与算式提交需要登录（SecurityConfig 中已要求认证）；提交时记录学生 id/姓名；
 * 学习页只展示已发布（PUBLISHED）的题目，按 sort_order 升序。</p>
 */
@Controller
@RequestMapping("/distributive")
public class DistributiveController {

    /** 卡片页默认展示的题目数量 */
    private static final int BOARD_SIZE = 4;

    @Autowired
    private DistributiveQuestionMapper questionMapper;

    @Autowired
    private DistributiveAnswerMapper answerMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /** 分配律学习页（Pad 版式，游客/学生均可访问） */
    @GetMapping("/learn")
    public String learn(Model model) {
        // 卡片页默认最多展示 4 道题，按 sort_order 升序
        List<DistributiveQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<DistributiveQuestion>()
                        .eq(DistributiveQuestion::getStatus, "PUBLISHED")
                        .orderByAsc(DistributiveQuestion::getSortOrder)
                        .orderByAsc(DistributiveQuestion::getId)
                        .last("LIMIT " + BOARD_SIZE));
        model.addAttribute("questions", questions);
        return "distributive/learn";
    }

    /**
     * 提交算式（AJAX，返回 JSON）。
     * 记录提交学生 id / 姓名，校验通过即入库，教师端可实时看到。
     */
    @PostMapping("/submit")
    @ResponseBody
    public Map<String, Object> submit(@RequestParam(required = false) Long questionId,
                                      @RequestParam(required = false) String expression,
                                      Principal principal) {
        if (questionId == null) {
            return fail("题目信息丢失，刷新页面再试试吧～");
        }
        DistributiveQuestion question = questionMapper.selectById(questionId);
        if (question == null || !"PUBLISHED".equals(question.getStatus())) {
            return fail("题目不存在或已下架，刷新页面再试试吧～");
        }

        ExpressionValidator.Result check = ExpressionValidator.validate(expression);
        if (!check.isValid()) {
            return fail(check.getMessage());
        }

        SysUser me = getCurrentUser(principal);

        DistributiveAnswer answer = new DistributiveAnswer();
        answer.setQuestionId(questionId);
        // 入库时用真实的乘号「×」展示（校验内部仍是 * 写法）
        answer.setExpression(check.getNormalized().replace('*', '×'));
        if (me != null) {
            answer.setStudentId(me.getId());
            answer.setStudentName(StringUtils.hasText(me.getNickname()) ? me.getNickname() : me.getUsername());
        }
        answerMapper.insert(answer);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "太棒了！已经交给老师啦～");
        return result;
    }

    /** 取当前登录学生（未登录返回 null） */
    private SysUser getCurrentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, principal.getName()));
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
