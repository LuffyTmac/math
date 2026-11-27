package com.math.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.math.entity.DistributiveAnswer;
import com.math.entity.EquationAnswer;
import com.math.entity.ShelfAnswer;
import com.math.entity.SysUser;
import com.math.entity.VariationAnswer;
import com.math.entity.VariationQuestion;
import com.math.mapper.DistributiveAnswerMapper;
import com.math.mapper.EquationAnswerMapper;
import com.math.mapper.ShelfAnswerMapper;
import com.math.mapper.SysUserMapper;
import com.math.mapper.VariationAnswerMapper;
import com.math.mapper.VariationQuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 「我的旅程」：按四个模块的完成情况展示进度。
 *
 * <p>分配律 / 自定义等式 / 自定义书架：有提交即完成；
 * 变式练习：需要把所有题目都提交过才算完成。</p>
 */
@Controller
public class JourneyController {

    @Autowired
    private DistributiveAnswerMapper distributiveAnswerMapper;

    @Autowired
    private EquationAnswerMapper equationAnswerMapper;

    @Autowired
    private ShelfAnswerMapper shelfAnswerMapper;

    @Autowired
    private VariationQuestionMapper variationQuestionMapper;

    @Autowired
    private VariationAnswerMapper variationAnswerMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @GetMapping("/journey")
    public String journey(Principal principal, Model model) {
        SysUser me = currentUser(principal);

        boolean stepDistributive = false;
        boolean stepEquation = false;
        boolean stepShelf = false;
        boolean stepVariation = false;
        int variationTotal = 0;
        int variationDone = 0;

        if (me != null) {
            stepDistributive = countDistributive(me.getId()) > 0;
            stepEquation = countEquation(me.getId()) > 0;
            stepShelf = countShelf(me.getId()) > 0;

            List<VariationQuestion> questions = variationQuestionMapper.selectList(
                    new LambdaQueryWrapper<>());
            Set<Long> allIds = new HashSet<>();
            for (VariationQuestion q : questions) {
                allIds.add(q.getId());
            }
            variationTotal = allIds.size();

            List<VariationAnswer> answers = variationAnswerMapper.selectList(
                    new LambdaQueryWrapper<VariationAnswer>()
                            .eq(VariationAnswer::getStudentId, me.getId()));
            Set<Long> answeredIds = new HashSet<>();
            for (VariationAnswer a : answers) {
                answeredIds.add(a.getQuestionId());
            }
            answeredIds.retainAll(allIds);
            variationDone = answeredIds.size();
            stepVariation = variationTotal > 0 && variationDone >= variationTotal;
        }

        int doneCount = (stepDistributive ? 1 : 0) + (stepEquation ? 1 : 0)
                + (stepShelf ? 1 : 0) + (stepVariation ? 1 : 0);

        model.addAttribute("me", me);
        model.addAttribute("stepDistributive", stepDistributive);
        model.addAttribute("stepEquation", stepEquation);
        model.addAttribute("stepShelf", stepShelf);
        model.addAttribute("stepVariation", stepVariation);
        model.addAttribute("variationTotal", variationTotal);
        model.addAttribute("variationDone", variationDone);
        model.addAttribute("doneCount", doneCount);
        model.addAttribute("allDone", doneCount == 4);
        return "student/journey";
    }

    private long countDistributive(Long studentId) {
        Long count = distributiveAnswerMapper.selectCount(
                new LambdaQueryWrapper<DistributiveAnswer>()
                        .eq(DistributiveAnswer::getStudentId, studentId));
        return count == null ? 0L : count;
    }

    private long countEquation(Long studentId) {
        Long count = equationAnswerMapper.selectCount(
                new LambdaQueryWrapper<EquationAnswer>()
                        .eq(EquationAnswer::getStudentId, studentId));
        return count == null ? 0L : count;
    }

    private long countShelf(Long studentId) {
        Long count = shelfAnswerMapper.selectCount(
                new LambdaQueryWrapper<ShelfAnswer>()
                        .eq(ShelfAnswer::getStudentId, studentId));
        return count == null ? 0L : count;
    }

    private SysUser currentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, principal.getName()));
    }
}
