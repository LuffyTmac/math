package com.math.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.math.entity.EquationAnswer;
import com.math.entity.SysUser;
import com.math.mapper.EquationAnswerMapper;
import com.math.mapper.SysUserMapper;
import com.math.util.EquationValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义等式 · 学生端
 *
 * <p>学生写一个形如 {@code A×(B+C)=A×B+A×C} 的等式提交；
 * 正确、错误都会入库，用 {@code is_correct} 标记，且可以反复提交。</p>
 */
@Controller
public class EquationController {

    /** 页面上「我交过的等式」最多展示多少条 */
    private static final int HISTORY_LIMIT = 10;

    @Autowired
    private EquationAnswerMapper answerMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @GetMapping("/learn/equation")
    public String page(Principal principal, Model model) {
        SysUser me = currentUser(principal);
        List<EquationAnswer> recent = new ArrayList<>();
        if (me != null) {
            recent = answerMapper.selectList(new LambdaQueryWrapper<EquationAnswer>()
                    .eq(EquationAnswer::getStudentId, me.getId())
                    .orderByDesc(EquationAnswer::getId)
                    .last("LIMIT " + HISTORY_LIMIT));
        }
        model.addAttribute("me", me);
        model.addAttribute("recent", recent);
        return "student/equation";
    }

    @PostMapping("/learn/equation/submit")
    @ResponseBody
    public Map<String, Object> submit(@RequestParam(required = false) String expression,
                                      Principal principal) {
        EquationValidator.Result check = EquationValidator.validate(expression);
        if (check.getDisplay().isEmpty()) {
            return fail(check.getMessage());
        }

        SysUser me = currentUser(principal);
        EquationAnswer answer = new EquationAnswer();
        answer.setExpression(check.getDisplay());
        answer.setCorrect(check.isCorrect() ? 1 : 0);
        if (me != null) {
            answer.setStudentId(me.getId());
            answer.setStudentName(StringUtils.hasText(me.getNickname()) ? me.getNickname() : me.getUsername());
        }
        answerMapper.insert(answer);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("correct", check.isCorrect());
        result.put("message", check.getMessage());
        result.put("expression", check.getDisplay());
        result.put("id", answer.getId());
        return result;
    }

    private SysUser currentUser(Principal principal) {
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
