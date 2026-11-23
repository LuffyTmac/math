package com.math.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.math.dto.DistributiveQuestionForm;
import com.math.entity.DistributiveAnswer;
import com.math.entity.DistributiveQuestion;
import com.math.entity.SysUser;
import com.math.mapper.DistributiveAnswerMapper;
import com.math.mapper.DistributiveQuestionMapper;
import com.math.mapper.SysUserMapper;
import com.math.util.XssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 乘法分配律 · 后台管理控制器（ADMIN / TEACHER）
 *
 * <p>列表页是「一行四卡 + 科幻云海 + 四条甬道」的形态：题目只展示图片，
 * 所有学生算式以气泡飘在页面上，点「开始规律」后飞入对应题目的甬道。</p>
 */
@Controller
@RequestMapping("/distributive/admin")
public class AdminDistributiveController {

    /** 看板默认展示的题目数量（按 sort_order 取前 4 条） */
    private static final int BOARD_SIZE = 4;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private DistributiveQuestionMapper questionMapper;
    @Autowired
    private DistributiveAnswerMapper answerMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    /* ==================== 列表 ==================== */

    /**
     * 题目列表（不分页，按 sort_order 升序）。
     * 同时把所有学生算式按题目分组塞进 model，供前端云海与甬道使用。
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<DistributiveQuestion> questions = listBoardQuestions();
        List<Long> boardIds = idsOf(questions);

        // 云海只展示看板上这 4 道题的算式
        List<DistributiveAnswer> answers = boardIds.isEmpty()
                ? new ArrayList<DistributiveAnswer>()
                : answerMapper.selectList(new LambdaQueryWrapper<DistributiveAnswer>()
                        .in(DistributiveAnswer::getQuestionId, boardIds)
                        .orderByAsc(DistributiveAnswer::getCreateTime)
                        .orderByAsc(DistributiveAnswer::getId));

        Map<Long, Integer> answerCounts = new HashMap<>();
        Map<Long, List<DistributiveAnswer>> answerMap = new HashMap<>();
        long maxAnswerId = 0L;
        for (DistributiveAnswer answer : answers) {
            answerCounts.merge(answer.getQuestionId(), 1, Integer::sum);
            answerMap.computeIfAbsent(answer.getQuestionId(), k -> new ArrayList<>()).add(answer);
            if (answer.getId() != null && answer.getId() > maxAnswerId) {
                maxAnswerId = answer.getId();
            }
        }
        for (DistributiveQuestion question : questions) {
            question.setAnswerCount(answerCounts.getOrDefault(question.getId(), 0));
        }

        model.addAttribute("questions", questions);
        model.addAttribute("answers", answers);
        model.addAttribute("answerMap", answerMap);
        model.addAttribute("answerCounts", answerCounts);
        model.addAttribute("totalAnswers", answers.size());
        model.addAttribute("maxAnswerId", maxAnswerId);
        return "distributive/admin_list";
    }

    /* ==================== 题目管理（列表页只展示图，增删改在这里） ==================== */

    /**
     * 题目管理页：表格形式维护题目（编辑 / 发布 / 下架 / 删除）。
     * 科幻列表页不放任何按钮，保持画面干净。
     */
    @GetMapping("/manage")
    public String manage(Model model) {
        List<DistributiveQuestion> questions = listAllQuestions();
        List<DistributiveAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<DistributiveAnswer>());
        Map<Long, Integer> answerCounts = new HashMap<>();
        for (DistributiveAnswer answer : answers) {
            answerCounts.merge(answer.getQuestionId(), 1, Integer::sum);
        }
        for (DistributiveQuestion question : questions) {
            question.setAnswerCount(answerCounts.getOrDefault(question.getId(), 0));
        }
        model.addAttribute("questions", questions);
        model.addAttribute("totalAnswers", answers.size());
        return "distributive/admin_manage";
    }

    /* ==================== 新增 ==================== */

    @GetMapping("/add")
    public String addForm(Model model) {
        DistributiveQuestion question = new DistributiveQuestion();
        question.setStatus("DRAFT");
        question.setSortOrder(nextSortOrder());
        model.addAttribute("question", question);
        model.addAttribute("mode", "add");
        return "distributive/admin_edit";
    }

    @PostMapping("/add")
    public String addSave(@ModelAttribute DistributiveQuestionForm form,
                          Principal principal,
                          RedirectAttributes ra) {
        String titleError = validateTitle(form.getTitle());
        if (titleError != null) {
            return backToForm(ra, "redirect:/distributive/admin/add", titleError);
        }
        String imageError = validateImageUrl(form.getImageUrl());
        if (imageError != null) {
            return backToForm(ra, "redirect:/distributive/admin/add", imageError);
        }

        boolean publish = form.isPublish();
        SysUser me = getCurrentUser(principal);

        DistributiveQuestion question = new DistributiveQuestion();
        question.setTitle(form.getTitle().trim());
        question.setImageUrl(cleanImageUrl(form.getImageUrl()));
        question.setDescription(StringUtils.hasText(form.getDescription())
                ? XssUtil.clean(form.getDescription()) : null);
        question.setSortOrder(form.getSortOrder() == null ? nextSortOrder() : form.getSortOrder());
        question.setStatus(publish ? "PUBLISHED" : "DRAFT");
        question.setCreatorId(me == null ? null : me.getId());
        if (publish) {
            question.setPublishTime(LocalDateTime.now());
        }
        questionMapper.insert(question);

        ra.addFlashAttribute("msg", publish
                ? "✅ 题目「" + question.getTitle() + "」已发布到学习页！"
                : "✅ 草稿「" + question.getTitle() + "」已保存！");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/distributive/admin/list";
    }

    /* ==================== 编辑 ==================== */

    @GetMapping("/edit")
    public String editForm(@RequestParam Long id, Model model, RedirectAttributes ra) {
        DistributiveQuestion question = questionMapper.selectById(id);
        if (question == null) {
            return notFound(ra);
        }
        model.addAttribute("question", question);
        model.addAttribute("mode", "edit");
        return "distributive/admin_edit";
    }

    @PostMapping("/edit")
    public String editSave(@ModelAttribute DistributiveQuestionForm form,
                           RedirectAttributes ra) {
        if (form.getId() == null) {
            return notFound(ra);
        }
        Long id = form.getId();
        DistributiveQuestion old = questionMapper.selectById(id);
        if (old == null) {
            return notFound(ra);
        }
        String titleError = validateTitle(form.getTitle());
        if (titleError != null) {
            return backToForm(ra, "redirect:/distributive/admin/edit?id=" + id, titleError);
        }
        String imageError = validateImageUrl(form.getImageUrl());
        if (imageError != null) {
            return backToForm(ra, "redirect:/distributive/admin/edit?id=" + id, imageError);
        }

        boolean publish = form.isPublish();
        boolean wasPublished = "PUBLISHED".equals(old.getStatus());

        LambdaUpdateWrapper<DistributiveQuestion> update = new LambdaUpdateWrapper<>();
        update.eq(DistributiveQuestion::getId, id)
              .set(DistributiveQuestion::getTitle, form.getTitle().trim())
              .set(DistributiveQuestion::getImageUrl, cleanImageUrl(form.getImageUrl()))
              .set(DistributiveQuestion::getDescription,
                      StringUtils.hasText(form.getDescription()) ? XssUtil.clean(form.getDescription()) : null)
              .set(DistributiveQuestion::getSortOrder,
                      form.getSortOrder() == null ? old.getSortOrder() : form.getSortOrder())
              .set(DistributiveQuestion::getStatus, publish ? "PUBLISHED" : "DRAFT");
        if (publish && old.getPublishTime() == null) {
            update.set(DistributiveQuestion::getPublishTime, LocalDateTime.now());
        }
        questionMapper.update(null, update);

        if (publish) {
            ra.addFlashAttribute("msg", wasPublished
                    ? "✅ 修改已保存，题目保持「已发布」状态！"
                    : "✅ 题目「" + form.getTitle().trim() + "」已发布到学习页！");
        } else {
            ra.addFlashAttribute("msg", wasPublished
                    ? "📝 已改为「草稿」，题目已从学习页下架～"
                    : "📝 草稿修改已保存！");
        }
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/distributive/admin/list";
    }

    /* ==================== 发布 / 下架 / 删除 ==================== */

    @PostMapping("/publish")
    public String publish(@RequestParam Long id, RedirectAttributes ra) {
        DistributiveQuestion question = questionMapper.selectById(id);
        if (question == null) {
            return notFound(ra);
        }
        LambdaUpdateWrapper<DistributiveQuestion> update = new LambdaUpdateWrapper<>();
        update.eq(DistributiveQuestion::getId, id)
              .set(DistributiveQuestion::getStatus, "PUBLISHED");
        if (question.getPublishTime() == null) {
            update.set(DistributiveQuestion::getPublishTime, LocalDateTime.now());
        }
        questionMapper.update(null, update);
        ra.addFlashAttribute("msg", "✅ 题目「" + question.getTitle() + "」已发布到学习页！");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/distributive/admin/list";
    }

    @PostMapping("/unpublish")
    public String unpublish(@RequestParam Long id, RedirectAttributes ra) {
        DistributiveQuestion question = questionMapper.selectById(id);
        if (question == null) {
            return notFound(ra);
        }
        LambdaUpdateWrapper<DistributiveQuestion> update = new LambdaUpdateWrapper<>();
        update.eq(DistributiveQuestion::getId, id)
              .set(DistributiveQuestion::getStatus, "DRAFT");
        questionMapper.update(null, update);
        ra.addFlashAttribute("msg", "📝 题目「" + question.getTitle() + "」已下架，改为草稿状态～");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/distributive/admin/list";
    }

    /** 删除题目（逻辑删除，连带删除该题下所有算式） */
    @PostMapping("/delete")
    public String delete(@RequestParam Long id, RedirectAttributes ra) {
        DistributiveQuestion question = questionMapper.selectById(id);
        if (question == null) {
            return notFound(ra);
        }
        LambdaUpdateWrapper<DistributiveAnswer> answerUpdate = new LambdaUpdateWrapper<>();
        answerUpdate.eq(DistributiveAnswer::getQuestionId, id)
                    .set(DistributiveAnswer::getDeleted, 1);
        answerMapper.update(null, answerUpdate);
        questionMapper.deleteById(id);
        ra.addFlashAttribute("msg", "✅ 题目「" + question.getTitle() + "」及其算式已删除！");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/distributive/admin/list";
    }

    /** 删除单条学生算式（AJAX，返回 JSON，不刷新页面） */
    @PostMapping("/answer/delete-json")
    @ResponseBody
    public Map<String, Object> deleteAnswerJson(@RequestParam Long id) {
        Map<String, Object> result = new HashMap<>();
        DistributiveAnswer answer = answerMapper.selectById(id);
        if (answer == null) {
            result.put("success", false);
            result.put("message", "算式不存在或已删除");
            return result;
        }
        answerMapper.deleteById(id);
        result.put("success", true);
        result.put("message", "已删除算式「" + answer.getExpression() + "」");
        return result;
    }

    /** 删除单条学生算式（表单兜底，删除结果用页面中间的弹框提示） */
    @PostMapping("/answer/delete")
    public String deleteAnswer(@RequestParam Long id, RedirectAttributes ra) {
        DistributiveAnswer answer = answerMapper.selectById(id);
        if (answer == null) {
            ra.addFlashAttribute("toastMsg", "算式不存在或已删除");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/distributive/admin/list";
        }
        answerMapper.deleteById(id);
        ra.addFlashAttribute("toastMsg", "已删除算式「" + answer.getExpression() + "」");
        ra.addFlashAttribute("toastType", "success");
        return "redirect:/distributive/admin/list";
    }

    /* ==================== 轮询：增量算式 ==================== */

    /**
     * 教师端自动刷新用：返回 id 大于 sinceId 的新算式。
     * 返回结构：{ success, total, maxId, items: [{id, questionId, expression, time}] }
     */
    @GetMapping("/answers")
    @ResponseBody
    public Map<String, Object> answersSince(@RequestParam(defaultValue = "0") Long sinceId) {
        long since = sinceId == null ? 0L : sinceId;
        List<Long> boardIds = idsOf(listBoardQuestions());

        List<Map<String, Object>> items = new ArrayList<>();
        long maxId = since;
        long total = 0L;

        if (!boardIds.isEmpty()) {
            List<DistributiveAnswer> fresh = answerMapper.selectList(
                    new LambdaQueryWrapper<DistributiveAnswer>()
                            .gt(DistributiveAnswer::getId, since)
                            .in(DistributiveAnswer::getQuestionId, boardIds)
                            .orderByAsc(DistributiveAnswer::getId));
            for (DistributiveAnswer answer : fresh) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", answer.getId());
                item.put("questionId", answer.getQuestionId());
                item.put("expression", answer.getExpression());
                item.put("time", answer.getCreateTime() == null ? "" : answer.getCreateTime().format(TIME_FMT));
                items.add(item);
                if (answer.getId() != null && answer.getId() > maxId) {
                    maxId = answer.getId();
                }
            }
            Long count = answerMapper.selectCount(new LambdaQueryWrapper<DistributiveAnswer>()
                    .in(DistributiveAnswer::getQuestionId, boardIds));
            total = count == null ? 0L : count;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("total", total);
        result.put("maxId", maxId);
        result.put("items", items);
        return result;
    }

    /* ==================== 内部工具 ==================== */

    /** 全部题目（题目管理页用） */
    private List<DistributiveQuestion> listAllQuestions() {
        return questionMapper.selectList(new LambdaQueryWrapper<DistributiveQuestion>()
                .orderByAsc(DistributiveQuestion::getSortOrder)
                .orderByAsc(DistributiveQuestion::getId));
    }

    /** 看板展示的题目：按 sort_order 取前 4 条 */
    private List<DistributiveQuestion> listBoardQuestions() {
        return questionMapper.selectList(new LambdaQueryWrapper<DistributiveQuestion>()
                .orderByAsc(DistributiveQuestion::getSortOrder)
                .orderByAsc(DistributiveQuestion::getId)
                .last("LIMIT " + BOARD_SIZE));
    }

    private List<Long> idsOf(List<DistributiveQuestion> questions) {
        List<Long> ids = new ArrayList<>();
        for (DistributiveQuestion question : questions) {
            if (question.getId() != null) {
                ids.add(question.getId());
            }
        }
        return ids;
    }

    private Integer nextSortOrder() {
        DistributiveQuestion last = questionMapper.selectOne(
                new LambdaQueryWrapper<DistributiveQuestion>()
                        .orderByDesc(DistributiveQuestion::getSortOrder)
                        .last("LIMIT 1"));
        if (last == null || last.getSortOrder() == null) {
            return 1;
        }
        return last.getSortOrder() + 1;
    }

    private String validateTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return "❌ 请填写题目名称！";
        }
        if (title.trim().length() > 200) {
            return "❌ 题目名称不能超过 200 个字！";
        }
        return null;
    }

    /** 图片链接只允许 http(s):// 或站内绝对路径（如上传后的 /uploads/...） */
    private String validateImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        String url = imageUrl.trim();
        boolean ok = url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/");
        return ok ? null : "❌ 图片链接要以 http(s):// 开头，或用「上传图片」得到的站内地址。";
    }

    private String cleanImageUrl(String imageUrl) {
        return StringUtils.hasText(imageUrl) ? imageUrl.trim() : null;
    }

    private String backToForm(RedirectAttributes ra, String redirect, String message) {
        ra.addFlashAttribute("msg", message);
        ra.addFlashAttribute("msgType", "error");
        return redirect;
    }

    private String notFound(RedirectAttributes ra) {
        ra.addFlashAttribute("msg", "❌ 题目不存在或已删除！");
        ra.addFlashAttribute("msgType", "error");
        return "redirect:/distributive/admin/list";
    }

    private SysUser getCurrentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, principal.getName()));
    }
}

