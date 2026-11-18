package com.math.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
 * 悬赏后台管理控制器（限 ADMIN 角色）
 * 路径 /bounty/admin/** 在 SecurityConfig 中限制为 ADMIN 角色才能访问。
 */
@Controller
@RequestMapping("/bounty/admin")
public class AdminBountyController {

    private static final long[] ALLOWED_SIZES = {5, 10, 20};
    private static final long DEFAULT_SIZE = 10;
    private static final int MAX_STAR = 5;

    @Autowired
    private BountyQuestionMapper questionMapper;
    @Autowired
    private BountyAnswerMapper answerMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    /** 题目管理列表（含草稿，含答题统计） */
    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "1") long page,
                       @RequestParam(defaultValue = "10") long size,
                       @RequestParam(required = false) String status,
                       Model model) {
        if (!inAllowedSizes(size)) size = DEFAULT_SIZE;
        Page<BountyQuestion> p = new Page<>(Math.max(page, 1), size);
        LambdaQueryWrapper<BountyQuestion> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            w.eq(BountyQuestion::getStatus, status);
        }
        w.orderByDesc(BountyQuestion::getCreateTime);
        IPage<BountyQuestion> result = questionMapper.selectPage(p, w);

        // 查每道题答题人数 + 纯文本预览
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
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("size", (int) size);
        return "bounty/admin_list";
    }

    /** 添加题目表单 */
    @GetMapping("/add")
    public String addForm(Model model) {
        BountyQuestion q = new BountyQuestion();
        q.setQuestionType("CALC");
        q.setStarCount(3);
        q.setStatus("DRAFT");
        model.addAttribute("question", q);
        model.addAttribute("mode", "add");
        return "bounty/admin_edit";
    }

    /** 保存题目（草稿或发布） */
    @PostMapping("/add")
    public String addSave(@RequestParam String title,
                          @RequestParam String content,
                          @RequestParam String questionType,
                          @RequestParam Integer starCount,
                          @RequestParam(required = false) String hintContent,
                          @RequestParam String saveAction,
                          Principal principal,
                          RedirectAttributes ra) {
        if (!StringUtils.hasText(title)) {
            ra.addFlashAttribute("msg", "❌ 请填写题目标题！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/admin/add";
        }
        if (!StringUtils.hasText(content) || content.trim().length() < 5) {
            ra.addFlashAttribute("msg", "❌ 题目内容太短啦，至少写 5 个字！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/admin/add";
        }
        SysUser me = getCurrentUser(principal);
        BountyQuestion q = new BountyQuestion();
        q.setTitle(title.trim());
        q.setContent(XssUtil.clean(content));
        q.setQuestionType(validType(questionType));
        q.setStarCount(validStar(starCount));
        q.setHintContent(StringUtils.hasText(hintContent) ? XssUtil.clean(hintContent) : null);
        q.setHintVisible(0);
        q.setStatus("PUBLISH".equals(saveAction) ? "PUBLISHED" : "DRAFT");
        q.setCreatorId(me != null ? me.getId() : null);
        if ("PUBLISHED".equals(saveAction)) {
            q.setPublishTime(LocalDateTime.now());
        }
        questionMapper.insert(q);
        ra.addFlashAttribute("msg", "PUBLISH".equals(saveAction)
                ? "✅ 题目「" + q.getTitle() + "」已发布到悬赏大厅！"
                : "✅ 草稿「" + q.getTitle() + "」已保存！");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/bounty/admin/list";
    }

    /** 编辑题目表单 */
    @GetMapping("/edit")
    public String editForm(@RequestParam Long id, Model model, RedirectAttributes ra) {
        BountyQuestion q = questionMapper.selectById(id);
        if (q == null) {
            ra.addFlashAttribute("msg", "❌ 题目不存在！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/admin/list";
        }
        model.addAttribute("question", q);
        model.addAttribute("mode", "edit");
        return "bounty/admin_edit";
    }

    /** 保存编辑 */
    @PostMapping("/edit")
    public String editSave(@RequestParam Long id,
                           @RequestParam String title,
                           @RequestParam String content,
                           @RequestParam String questionType,
                           @RequestParam Integer starCount,
                           @RequestParam(required = false) String hintContent,
                           @RequestParam String saveAction,
                           RedirectAttributes ra) {
        BountyQuestion q = questionMapper.selectById(id);
        if (q == null) {
            ra.addFlashAttribute("msg", "❌ 题目不存在！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/admin/list";
        }
        if (!StringUtils.hasText(title)) {
            ra.addFlashAttribute("msg", "❌ 请填写题目标题！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/admin/edit?id=" + id;
        }
        LambdaUpdateWrapper<BountyQuestion> uw = new LambdaUpdateWrapper<>();
        uw.eq(BountyQuestion::getId, id)
          .set(BountyQuestion::getTitle, title.trim())
          .set(BountyQuestion::getContent, XssUtil.clean(content))
          .set(BountyQuestion::getQuestionType, validType(questionType))
          .set(BountyQuestion::getStarCount, validStar(starCount))
          .set(BountyQuestion::getHintContent, StringUtils.hasText(hintContent) ? XssUtil.clean(hintContent) : null);
        // 状态完全由按钮决定：
        //   PUBLISH -> PUBLISHED（首次发布才设 publishTime，重新发布保留原发布时间）
        //   DRAFT   -> DRAFT（已发布题目点保存草稿会下架，从大厅移除）
        if ("PUBLISH".equals(saveAction)) {
            uw.set(BountyQuestion::getStatus, "PUBLISHED");
            if (q.getPublishTime() == null) {
                uw.set(BountyQuestion::getPublishTime, LocalDateTime.now());
            }
        } else {
            uw.set(BountyQuestion::getStatus, "DRAFT");
        }
        questionMapper.update(null, uw);
        boolean wasPublished = "PUBLISHED".equals(q.getStatus());
        if ("PUBLISH".equals(saveAction)) {
            ra.addFlashAttribute("msg", wasPublished
                    ? "✅ 题目修改已保存，仍处于「已发布」状态！"
                    : "✅ 题目「" + q.getTitle() + "」已发布到悬赏大厅！");
        } else {
            ra.addFlashAttribute("msg", wasPublished
                    ? "📝 题目已改为「草稿」状态，已从悬赏大厅下架～"
                    : "📝 草稿修改已保存！");
        }
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/bounty/admin/list";
    }

    /** 发布草稿 */
    @PostMapping("/publish")
    public String publish(@RequestParam Long id, RedirectAttributes ra) {
        BountyQuestion q = questionMapper.selectById(id);
        if (q == null) {
            ra.addFlashAttribute("msg", "❌ 题目不存在！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/admin/list";
        }
        if ("PUBLISHED".equals(q.getStatus())) {
            ra.addFlashAttribute("msg", "❌ 该题目已经是发布状态！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/admin/list";
        }
        LambdaUpdateWrapper<BountyQuestion> uw = new LambdaUpdateWrapper<>();
        uw.eq(BountyQuestion::getId, id)
          .set(BountyQuestion::getStatus, "PUBLISHED")
          .set(BountyQuestion::getPublishTime, LocalDateTime.now());
        questionMapper.update(null, uw);
        ra.addFlashAttribute("msg", "✅ 题目「" + q.getTitle() + "」已发布到悬赏大厅！");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/bounty/admin/list";
    }

    /** 切换提示展示状态（0→1 隐藏→展示 / 1→0 展示→隐藏）
     *  from=list 重定向回列表页；from=answers 重定向回答题详情页 */
    @PostMapping("/toggle-hint")
    public String toggleHint(@RequestParam Long id,
                             @RequestParam(defaultValue = "list") String from,
                             RedirectAttributes ra) {
        BountyQuestion q = questionMapper.selectById(id);
        if (q == null) {
            ra.addFlashAttribute("msg", "❌ 题目不存在！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/admin/list";
        }
        // 没有提示内容时不允许开启展示
        if (!StringUtils.hasText(q.getHintContent()) && (q.getHintVisible() == null || q.getHintVisible() == 0)) {
            ra.addFlashAttribute("msg", "❌ 该题目还没有填写提示与思路，请先编辑题目填写内容！");
            ra.addFlashAttribute("msgType", "error");
            return "answers".equals(from) ? "redirect:/bounty/admin/answers?questionId=" + id : "redirect:/bounty/admin/list";
        }
        int newVisible = (q.getHintVisible() != null && q.getHintVisible() == 1) ? 0 : 1;
        LambdaUpdateWrapper<BountyQuestion> uw = new LambdaUpdateWrapper<>();
        uw.eq(BountyQuestion::getId, id)
          .set(BountyQuestion::getHintVisible, newVisible);
        questionMapper.update(null, uw);
        ra.addFlashAttribute("msg", newVisible == 1
                ? "✅ 提示已展示！答题者现在可以看到提示与思路啦～"
                : "✅ 提示已隐藏！");
        ra.addFlashAttribute("msgType", "success");
        return "answers".equals(from) ? "redirect:/bounty/admin/answers?questionId=" + id : "redirect:/bounty/admin/list";
    }

    /** 删除题目（逻辑删除） */
    @PostMapping("/delete")
    public String delete(@RequestParam Long id, RedirectAttributes ra) {
        BountyQuestion q = questionMapper.selectById(id);
        if (q == null) {
            ra.addFlashAttribute("msg", "❌ 题目不存在！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/admin/list";
        }
        questionMapper.deleteById(id);
        ra.addFlashAttribute("msg", "✅ 题目「" + q.getTitle() + "」已删除！");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/bounty/admin/list";
    }

    /** 某题答题列表（含答题人信息，admin 可评分） */
    @GetMapping("/answers")
    public String answerList(@RequestParam Long questionId, Model model, RedirectAttributes ra) {
        BountyQuestion q = questionMapper.selectById(questionId);
        if (q == null) {
            ra.addFlashAttribute("msg", "❌ 题目不存在！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/admin/list";
        }
        LambdaQueryWrapper<BountyAnswer> w = new LambdaQueryWrapper<>();
        w.eq(BountyAnswer::getQuestionId, questionId).orderByDesc(BountyAnswer::getSubmitTime);
        List<BountyAnswer> answers = answerMapper.selectList(w);
        // 关联答题人用户名
        Map<Long, SysUser> userMap = new HashMap<>();
        for (BountyAnswer a : answers) {
            if (!userMap.containsKey(a.getAnswerUserId())) {
                userMap.put(a.getAnswerUserId(), sysUserMapper.selectById(a.getAnswerUserId()));
            }
        }
        model.addAttribute("question", q);
        model.addAttribute("answers", answers);
        model.addAttribute("userMap", userMap);
        return "bounty/admin_answers";
    }

    /** 评分提交 */
    @PostMapping("/score")
    public String score(@RequestParam Long answerId,
                        @RequestParam Integer score,
                        Principal principal,
                        RedirectAttributes ra) {
        BountyAnswer a = answerMapper.selectById(answerId);
        if (a == null) {
            ra.addFlashAttribute("msg", "❌ 答题记录不存在！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/admin/list";
        }
        if (score == null || score < 0 || score > MAX_STAR) {
            ra.addFlashAttribute("msg", "❌ 星星数必须在 0 ~ " + MAX_STAR + " 之间！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/bounty/admin/answers?questionId=" + a.getQuestionId();
        }
        SysUser me = getCurrentUser(principal);
        LambdaUpdateWrapper<BountyAnswer> uw = new LambdaUpdateWrapper<>();
        uw.eq(BountyAnswer::getId, answerId)
          .set(BountyAnswer::getScore, score)
          .set(BountyAnswer::getScoreStatus, "SCORED")
          .set(BountyAnswer::getScorerId, me != null ? me.getId() : null)
          .set(BountyAnswer::getScoreTime, LocalDateTime.now());
        answerMapper.update(null, uw);
        ra.addFlashAttribute("msg", "✅ 评分成功！给该答题 " + score + " 颗星⭐！");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/bounty/admin/answers?questionId=" + a.getQuestionId();
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

    private String validType(String type) {
        if ("APPLICATION".equals(type) || "ORAL".equals(type)) return type;
        return "CALC";
    }

    private Integer validStar(Integer star) {
        if (star == null || star < 1) return 1;
        if (star > MAX_STAR) return MAX_STAR;
        return star;
    }

    private boolean inAllowedSizes(long size) {
        for (long s : ALLOWED_SIZES) if (s == size) return true;
        return false;
    }
}
