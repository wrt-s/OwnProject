package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.RewardTask;
import com.wrt.excel.order.entity.RewardTaskClaim;
import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.RewardTaskService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 赚币任务：点单方完成领币；做餐方可确认/管理
 */
@Slf4j
@RestController
@RequestMapping("/api/reward-task")
@RequiredArgsConstructor
public class RewardTaskController {

    private final RewardTaskService rewardTaskService;
    private final AuthService authService;

    /** 点单方可见的启用任务列表（含今日进度） */
    @GetMapping("/list")
    public Map<String, Object> list(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = authService.currentUser(session);
            if (user == null) {
                result.put("code", 401);
                result.put("message", "未登录");
                return result;
            }
            Long fid = user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID;
            rewardTaskService.ensureSeedTasks(fid);
            List<Map<String, Object>> data = rewardTaskService.listTasksForUser(fid, user.getId());
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", data);
        } catch (Exception e) {
            log.error("任务列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 做餐方：全部任务（含停用） */
    @GetMapping("/manage/list")
    public Map<String, Object> manageList(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = requireChef(session, result);
            if (user == null) return result;
            Long fid = user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID;
            rewardTaskService.ensureSeedTasks(fid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", rewardTaskService.listTasks(fid, false));
        } catch (Exception e) {
            log.error("管理任务列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 点单方提交完成 */
    @PostMapping("/claim")
    public Map<String, Object> claim(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = authService.currentUser(session);
            if (user == null) {
                result.put("code", 401);
                result.put("message", "未登录");
                return result;
            }
            if (user.isChef()) {
                result.put("code", 400);
                result.put("message", "做餐方请用管理入口，无需自己领任务币");
                return result;
            }
            Long taskId = body.get("taskId") != null ? Long.valueOf(body.get("taskId").toString()) : null;
            if (taskId == null) {
                result.put("code", 400);
                result.put("message", "缺少 taskId");
                return result;
            }
            String remark = body.get("remark") != null ? body.get("remark").toString() : null;
            Long fid = user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID;
            RewardTaskClaim claim = rewardTaskService.claim(taskId, user.getId(), fid, remark);
            result.put("code", 200);
            result.put("message", "pending".equals(claim.getStatus())
                    ? "已提交，等待做餐方确认后到账"
                    : ("完成！+" + claim.getCoinsAwarded() + " 爱心币"));
            result.put("data", claim);
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("领取任务失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/my-claims")
    public Map<String, Object> myClaims(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", rewardTaskService.listMyClaims(uid, 30));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/pending")
    public Map<String, Object> pending(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = requireChef(session, result);
            if (user == null) return result;
            Long fid = user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID;
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", rewardTaskService.listPending(fid));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirm(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = requireChef(session, result);
            if (user == null) return result;
            Long claimId = body.get("claimId") != null ? Long.valueOf(body.get("claimId").toString()) : null;
            boolean approve = body.get("approve") == null || Boolean.parseBoolean(body.get("approve").toString())
                    || "1".equals(String.valueOf(body.get("approve")))
                    || "true".equalsIgnoreCase(String.valueOf(body.get("approve")));
            // also accept approve:false
            if (body.get("approve") != null) {
                Object a = body.get("approve");
                if (a instanceof Boolean) approve = (Boolean) a;
                else {
                    String s = String.valueOf(a);
                    approve = "1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
                }
            }
            String remark = body.get("remark") != null ? body.get("remark").toString() : null;
            if (claimId == null) {
                result.put("code", 400);
                result.put("message", "缺少 claimId");
                return result;
            }
            RewardTaskClaim claim = rewardTaskService.confirm(claimId, user.getId(), approve, remark);
            result.put("code", 200);
            result.put("message", approve ? "已确认发币" : "已拒绝");
            result.put("data", claim);
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("确认任务失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody RewardTask task, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = requireChef(session, result);
            if (user == null) return result;
            Long fid = user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID;
            RewardTask saved = rewardTaskService.saveTask(task, fid);
            result.put("code", 200);
            result.put("message", "保存成功");
            result.put("data", saved);
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("保存任务失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/toggle")
    public Map<String, Object> toggle(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = requireChef(session, result);
            if (user == null) return result;
            Long taskId = body.get("taskId") != null ? Long.valueOf(body.get("taskId").toString()) : null;
            boolean enabled = body.get("enabled") == null || Boolean.parseBoolean(String.valueOf(body.get("enabled")))
                    || "1".equals(String.valueOf(body.get("enabled")));
            if (body.get("enabled") != null) {
                Object e = body.get("enabled");
                if (e instanceof Boolean) enabled = (Boolean) e;
                else {
                    String s = String.valueOf(e);
                    enabled = "1".equals(s) || "true".equalsIgnoreCase(s);
                }
            }
            Long fid = user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID;
            rewardTaskService.setEnabled(taskId, fid, enabled);
            result.put("code", 200);
            result.put("message", enabled ? "已启用" : "已停用");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private User requireChef(HttpSession session, Map<String, Object> result) {
        User user = authService.currentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("message", "未登录");
            return null;
        }
        if (!user.isChef()) {
            result.put("code", 403);
            result.put("message", "仅做餐方可操作");
            return null;
        }
        return user;
    }
}
