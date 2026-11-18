package com.math.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.math.entity.BountyAnswer;
import com.math.entity.BountyQuestion;
import com.math.entity.SysUser;
import com.math.mapper.BountyAnswerMapper;
import com.math.mapper.BountyQuestionMapper;
import com.math.mapper.SysUserMapper;
import com.math.util.XssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 悬赏前台控制器（大厅公开 / 答题需登录）
 */
@Controller
@RequestMapping("/bounty")
public class BountyController {

    private static final long[] ALLOWED_SIZES = {6, 9, 12, 18};
    private static final long DEFAULT_SIZE = 9;

    @Autowired
    private BountyQuestionMapper questionMapper;
    @Autowired
    private BountyAnswerMapper answerMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 悬赏大厅（公开访问）
     * @param sort time=按发布时间(默认) / star=按星星数
     */
    @GetMapping("/hall")
    public String hall(@RequestParam(defaultValue = "1") long page,
                       @RequestParam(defaultValue = "9") long size,
                       @RequestParam(defaultValue = "time") String sort,
                       Model model) {
        if (!inAllowedSizes(size)) size = DEFAULT_SIZE;
        Page<BountyQuestion> p = new Page<>(Math.max(page, 1), size);
        LambdaQueryWrapper<BountyQuestion> w = new LambdaQueryWrapper<>();
        w.eq(BountyQuestion::getStatus, "PUBLISHED");
        if ("star".equalsIgnoreCase(sort)) {
            w.orderByDesc(BountyQuestion::getStarCount).orderByDesc(BountyQuestion::getPublishTime);
        } else {
            w.orderByDesc(BountyQuestion::getPublishTime);
        }
        IPage<BountyQuestion> result = questionMapper.selectPage(p, w);

        // 查每道题的答题人数（用于大厅展示热度）+ 生成纯文本预览
        Map<Long, Long> answerCounts = new HashMap<>();
        for (BountyQuestion q : result.getRecords()) {
            q.setTextPreview(previewText(q.getContent()));
            LambdaQueryWrapper<BountyAnswer> aw = new LambdaQueryWrapper<>();
            aw.eq(BountyAnswer::getQuestionId, q.getId());
            Long c = answerMapper.selectCount(aw);
            answerCounts.put(q.getId(), c != null ? c : 0L);
        }

        model.addAttribute("questions", result.getRecords());
        model.addAttribute("answerCounts", answerCounts);
        model.addAttribute("total", result.getTotal());
        model.addAttribute("current", (int) Math.max(result.getCurrent(), 1));
        model.addAttribute("pages", (int) result.getPages());
        model.addAttribute("sort", sort);
        model.addAttribute("size", (int) size);
        return "bounty/hall";
    }

    /** 答题表单（需登录） */
    @GetMapping("/answer")
    public String answerForm(@RequestParam Long questionId, Principal principal,
                             Model model, RedirectAttributes ra) {
        BountyQuestion q = questionMapper.selectById(questionId);
        if (q == null || !"PUBLISHED".equals(q.getStatus())) {
            ra.addFlashAttribute("msg", "❌ 题目不存在或未发布！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/hall";
        }
        SysUser me = getCurrentUser(principal);
        // 同一题同一用户只能答一次
        LambdaQueryWrapper<BountyAnswer> w = new LambdaQueryWrapper<>();
        w.eq(BountyAnswer::getQuestionId, questionId).eq(BountyAnswer::getAnswerUserId, me.getId());
        Long cnt = answerMapper.selectCount(w);
        boolean hasAnswered = cnt != null && cnt > 0;
        if (hasAnswered) {
            ra.addFlashAttribute("msg", "❌ 你已经答过这道题啦，去「我的答题」看看吧！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/my-answers";
        }
        // 提示展示条件：用户已答过此题 且 题目设置了展示提示 且 有提示内容
        // 答题页用户必然未答过，所以这里恒为 false，但保留逻辑以便统一判断条件
        model.addAttribute("hasAnswered", hasAnswered);
        model.addAttribute("question", q);
        return "bounty/answer";
    }

    /** 提交答题（需登录） */
    @PostMapping("/answer")
    public String answerSave(@RequestParam Long questionId,
                             @RequestParam String answerContent,
                             Principal principal,
                             RedirectAttributes ra) {
        BountyQuestion q = questionMapper.selectById(questionId);
        if (q == null || !"PUBLISHED".equals(q.getStatus())) {
            ra.addFlashAttribute("msg", "❌ 题目不存在或未发布！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/hall";
        }
        if (!StringUtils.hasText(answerContent) || answerContent.trim().length() < 5) {
            ra.addFlashAttribute("msg", "❌ 答题内容太短啦，至少写 5 个字哦！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/answer?questionId=" + questionId;
        }
        SysUser me = getCurrentUser(principal);
        // 重复答题兜底检查
        LambdaQueryWrapper<BountyAnswer> w = new LambdaQueryWrapper<>();
        w.eq(BountyAnswer::getQuestionId, questionId).eq(BountyAnswer::getAnswerUserId, me.getId());
        Long cnt = answerMapper.selectCount(w);
        if (cnt != null && cnt > 0) {
            ra.addFlashAttribute("msg", "❌ 你已经答过这道题啦！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/my-answers";
        }
        BountyAnswer a = new BountyAnswer();
        a.setQuestionId(questionId);
        a.setAnswerUserId(me.getId());
        a.setAnswerContent(XssUtil.clean(answerContent));
        a.setSubmitTime(LocalDateTime.now());
        a.setScoreStatus("UNSCORED");
        answerMapper.insert(a);
        ra.addFlashAttribute("msg", "✅ 答题提交成功！等待老师评分～");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/bounty/my-answers";
    }

    /** 我的答题列表（需登录） */
    @GetMapping("/my-answers")
    public String myAnswers(Principal principal, Model model) {
        SysUser me = getCurrentUser(principal);
        LambdaQueryWrapper<BountyAnswer> w = new LambdaQueryWrapper<>();
        w.eq(BountyAnswer::getAnswerUserId, me.getId()).orderByDesc(BountyAnswer::getSubmitTime);
        List<BountyAnswer> answers = answerMapper.selectList(w);
        // 关联题目信息
        Map<Long, BountyQuestion> qMap = new HashMap<>();
        for (BountyAnswer a : answers) {
            if (!qMap.containsKey(a.getQuestionId())) {
                qMap.put(a.getQuestionId(), questionMapper.selectById(a.getQuestionId()));
            }
        }
        model.addAttribute("answers", answers);
        model.addAttribute("qMap", qMap);
        return "bounty/my_answers";
    }

    /** 我的答题详情（需登录）
     *  详情页下方展示"同学们的回答"模块：
     *  仅当用户已答过此题（能进入详情页即代表已答）时，才展示同一道题其他用户已评分的答题，
     *  让小朋友互相学习。未评分的答题不公开。
     */
    @GetMapping("/my-answer")
    public String myAnswerDetail(@RequestParam Long id, Principal principal,
                                 Model model, RedirectAttributes ra) {
        BountyAnswer a = answerMapper.selectById(id);
        if (a == null) {
            ra.addFlashAttribute("msg", "❌ 答题记录不存在！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/my-answers";
        }
        SysUser me = getCurrentUser(principal);
        if (!a.getAnswerUserId().equals(me.getId())) {
            ra.addFlashAttribute("msg", "❌ 这不是你的答题记录哦！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/my-answers";
        }
        BountyQuestion q = questionMapper.selectById(a.getQuestionId());
        model.addAttribute("answer", a);
        model.addAttribute("question", q);
        // 提示展示条件：用户已答过此题（进入详情页必然为 true，因校验 answerUserId==me.id）且题目设置了展示提示
        model.addAttribute("hasAnswered", true);

        // ---- 同学们的回答：同一道题、其他用户、已评分的答题（按评分时间倒序）----
        // 只有用户自己已答过此题（进入详情页的前提）才展示，符合"答题结束后才看得到"的需求
        LambdaQueryWrapper<BountyAnswer> ow = new LambdaQueryWrapper<>();
        ow.eq(BountyAnswer::getQuestionId, a.getQuestionId())
          .ne(BountyAnswer::getAnswerUserId, me.getId())
          .eq(BountyAnswer::getScoreStatus, "SCORED")
          .orderByDesc(BountyAnswer::getScoreTime);
        List<BountyAnswer> otherAnswers = answerMapper.selectList(ow);
        // 关联答题人信息 + 生成纯文本预览
        Map<Long, SysUser> otherUserMap = new HashMap<>();
        for (BountyAnswer oa : otherAnswers) {
            if (!otherUserMap.containsKey(oa.getAnswerUserId())) {
                otherUserMap.put(oa.getAnswerUserId(), sysUserMapper.selectById(oa.getAnswerUserId()));
            }
            oa.setTextPreview(previewText(oa.getAnswerContent()));
        }
        model.addAttribute("otherAnswers", otherAnswers);
        model.addAttribute("otherUserMap", otherUserMap);
        return "bounty/my_answer_detail";
    }

    /** 富文本转纯文本预览（去标签 + 截断 100 字） */
    private String previewText(String html) {
        if (html == null || html.isEmpty()) return "";
        String text = html.replaceAll("<[^>]+>", "")
                          .replaceAll("&nbsp;", " ")
                          .replaceAll("&amp;", "&")
                          .replaceAll("&lt;", "<")
                          .replaceAll("&gt;", ">")
                          .replaceAll("&quot;", "\"")
                          .replaceAll("\\s+", " ")
                          .trim();
        if (text.length() > 100) text = text.substring(0, 100) + "...";
        return text;
    }

    private SysUser getCurrentUser(Principal principal) {
        if (principal == null) return null;
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getUsername, principal.getName());
        return sysUserMapper.selectOne(w);
    }

    private boolean inAllowedSizes(long size) {
        for (long s : ALLOWED_SIZES) if (s == size) return true;
        return false;
    }
}
