package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.RewardTask;
import com.wrt.excel.order.entity.RewardTaskClaim;
import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.mapper.RewardTaskClaimMapper;
import com.wrt.excel.order.mapper.RewardTaskMapper;
import com.wrt.excel.order.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardTaskService {

    private final RewardTaskMapper rewardTaskMapper;
    private final RewardTaskClaimMapper claimMapper;
    private final CoinService coinService;
    private final UserMapper userMapper;

    public List<RewardTask> listTasks(Long familyId, boolean enabledOnly) {
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        LambdaQueryWrapper<RewardTask> w = new LambdaQueryWrapper<>();
        w.eq(RewardTask::getFamilyId, fid);
        if (enabledOnly) {
            w.eq(RewardTask::getEnabled, 1);
        }
        w.orderByAsc(RewardTask::getSortOrder).orderByAsc(RewardTask::getId);
        return rewardTaskMapper.selectList(w);
    }

    public List<Map<String, Object>> listTasksForUser(Long familyId, Long userId) {
        List<RewardTask> tasks = listTasks(familyId, true);
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = LocalDate.now().atTime(LocalTime.MAX);
        return tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("description", t.getDescription());
            m.put("coinReward", t.getCoinReward());
            m.put("requireConfirm", t.getRequireConfirm() != null && t.getRequireConfirm() == 1);
            m.put("dailyLimit", t.getDailyLimit() != null ? t.getDailyLimit() : 1);
            int todayCount = countClaimsToday(t.getId(), userId, dayStart, dayEnd);
            m.put("todayCompleted", todayCount);
            int limit = t.getDailyLimit() == null ? 1 : t.getDailyLimit();
            boolean canClaim = limit <= 0 || todayCount < limit;
            m.put("canClaim", canClaim);
            m.put("remainToday", limit <= 0 ? null : Math.max(0, limit - todayCount));
            return m;
        }).collect(Collectors.toList());
    }

    private int countClaimsToday(Long taskId, Long userId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<RewardTaskClaim> w = new LambdaQueryWrapper<>();
        w.eq(RewardTaskClaim::getTaskId, taskId)
                .eq(RewardTaskClaim::getUserId, userId)
                .ne(RewardTaskClaim::getStatus, "rejected")
                .ge(RewardTaskClaim::getCreatedAt, start)
                .le(RewardTaskClaim::getCreatedAt, end);
        return Math.toIntExact(claimMapper.selectCount(w));
    }

    @Transactional
    public RewardTaskClaim claim(Long taskId, Long userId, Long familyId, String remark) {
        RewardTask task = rewardTaskMapper.selectById(taskId);
        if (task == null || task.getEnabled() == null || task.getEnabled() != 1) {
            throw new IllegalArgumentException("任务不存在或已停用");
        }
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        if (!Objects.equals(task.getFamilyId(), fid)) {
            throw new IllegalStateException("无权领取该任务");
        }
        int limit = task.getDailyLimit() == null ? 1 : task.getDailyLimit();
        if (limit > 0) {
            int today = countClaimsToday(taskId, userId, LocalDate.now().atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX));
            if (today >= limit) {
                throw new IllegalArgumentException("今日该任务已达完成上限（" + limit + "次）");
            }
        }
        boolean needConfirm = task.getRequireConfirm() != null && task.getRequireConfirm() == 1;
        RewardTaskClaim claim = new RewardTaskClaim();
        claim.setTaskId(taskId);
        claim.setUserId(userId);
        claim.setFamilyId(fid);
        claim.setRemark(remark);
        claim.setCreatedAt(LocalDateTime.now());
        claim.setCoinsAwarded(0);

        if (needConfirm) {
            claim.setStatus("pending");
            claimMapper.insert(claim);
            claim.setTaskName(task.getName());
            return claim;
        }

        // 立即发币
        int coins = task.getCoinReward() != null ? task.getCoinReward() : 0;
        if (coins > 0) {
            String desc = "完成任务「" + task.getName() + "」奖励";
            coinService.addCoins(userId, coins, desc, "task_" + task.getName());
        }
        claim.setStatus("completed");
        claim.setCoinsAwarded(coins);
        claimMapper.insert(claim);
        // 回写 relatedId 到流水较难，已用 description 标明
        claim.setTaskName(task.getName());
        return claim;
    }

    public List<RewardTaskClaim> listPending(Long familyId) {
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        LambdaQueryWrapper<RewardTaskClaim> w = new LambdaQueryWrapper<>();
        w.eq(RewardTaskClaim::getFamilyId, fid)
                .eq(RewardTaskClaim::getStatus, "pending")
                .orderByDesc(RewardTaskClaim::getCreatedAt);
        List<RewardTaskClaim> list = claimMapper.selectList(w);
        enrichClaims(list);
        return list;
    }

    public List<RewardTaskClaim> listMyClaims(Long userId, int limit) {
        LambdaQueryWrapper<RewardTaskClaim> w = new LambdaQueryWrapper<>();
        w.eq(RewardTaskClaim::getUserId, userId)
                .orderByDesc(RewardTaskClaim::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100)));
        List<RewardTaskClaim> list = claimMapper.selectList(w);
        enrichClaims(list);
        return list;
    }

    private void enrichClaims(List<RewardTaskClaim> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> taskIds = list.stream().map(RewardTaskClaim::getTaskId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = list.stream().map(RewardTaskClaim::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, RewardTask> tasks = taskIds.isEmpty() ? Map.of() :
                rewardTaskMapper.selectBatchIds(taskIds).stream().collect(Collectors.toMap(RewardTask::getId, t -> t, (a, b) -> a));
        Map<Long, User> users = userIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        for (RewardTaskClaim c : list) {
            RewardTask t = tasks.get(c.getTaskId());
            if (t != null) c.setTaskName(t.getName());
            User u = users.get(c.getUserId());
            if (u != null) c.setUserNickname(u.getNickname());
        }
    }

    @Transactional
    public RewardTaskClaim confirm(Long claimId, Long chefUserId, boolean approve, String remark) {
        RewardTaskClaim claim = claimMapper.selectById(claimId);
        if (claim == null) throw new IllegalArgumentException("记录不存在");
        if (!"pending".equals(claim.getStatus())) {
            throw new IllegalArgumentException("该申请已处理");
        }
        claim.setConfirmedBy(chefUserId);
        claim.setConfirmedAt(LocalDateTime.now());
        if (StringUtils.hasText(remark)) {
            claim.setRemark(remark);
        }
        if (!approve) {
            claim.setStatus("rejected");
            claim.setCoinsAwarded(0);
            claimMapper.updateById(claim);
            return claim;
        }
        RewardTask task = rewardTaskMapper.selectById(claim.getTaskId());
        if (task == null) throw new IllegalArgumentException("任务不存在");
        int coins = task.getCoinReward() != null ? task.getCoinReward() : 0;
        if (coins > 0) {
            String desc = "完成任务「" + task.getName() + "」奖励（做餐方确认）";
            coinService.addCoins(claim.getUserId(), coins, desc, "task_" + task.getName());
        }
        claim.setStatus("approved");
        claim.setCoinsAwarded(coins);
        claimMapper.updateById(claim);
        claim.setTaskName(task.getName());
        return claim;
    }

    @Transactional
    public RewardTask saveTask(RewardTask task, Long familyId) {
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        if (!StringUtils.hasText(task.getName())) {
            throw new IllegalArgumentException("请填写任务名称");
        }
        if (task.getCoinReward() == null || task.getCoinReward() <= 0) {
            throw new IllegalArgumentException("奖励币数须大于0");
        }
        task.setFamilyId(fid);
        if (task.getEnabled() == null) task.setEnabled(1);
        if (task.getRequireConfirm() == null) task.setRequireConfirm(0);
        if (task.getDailyLimit() == null) task.setDailyLimit(1);
        if (task.getSortOrder() == null) task.setSortOrder(100);
        LocalDateTime now = LocalDateTime.now();
        if (task.getId() == null) {
            task.setCreatedAt(now);
            task.setUpdatedAt(now);
            rewardTaskMapper.insert(task);
        } else {
            RewardTask old = rewardTaskMapper.selectById(task.getId());
            if (old == null || !Objects.equals(old.getFamilyId(), fid)) {
                throw new IllegalStateException("无权修改该任务");
            }
            task.setUpdatedAt(now);
            rewardTaskMapper.updateById(task);
        }
        return task;
    }

    @Transactional
    public void setEnabled(Long taskId, Long familyId, boolean enabled) {
        RewardTask old = rewardTaskMapper.selectById(taskId);
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        if (old == null || !Objects.equals(old.getFamilyId(), fid)) {
            throw new IllegalStateException("无权操作");
        }
        old.setEnabled(enabled ? 1 : 0);
        old.setUpdatedAt(LocalDateTime.now());
        rewardTaskMapper.updateById(old);
    }

    /** 种子：若家庭无任务则插入默认（含「爱爱」） */
    @Transactional
    public void ensureSeedTasks(Long familyId) {
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        Long cnt = rewardTaskMapper.selectCount(new LambdaQueryWrapper<RewardTask>().eq(RewardTask::getFamilyId, fid));
        if (cnt != null && cnt > 0) return;
        Object[][] seeds = new Object[][]{
                {"爱爱", "完成一次亲密互动～", 52, 0, 3, 1},
                {"夸夸做餐方", "真诚夸一句今天的饭", 10, 0, 2, 2},
                {"帮忙洗碗", "主动洗碗或收拾厨房", 15, 1, 1, 3},
                {"早安吻", "早上给对方一个吻或暖心消息", 8, 0, 1, 4},
                {"按摩放松", "给做餐方按摩放松一下", 20, 1, 1, 5}
        };
        LocalDateTime now = LocalDateTime.now();
        for (Object[] s : seeds) {
            RewardTask t = new RewardTask();
            t.setFamilyId(fid);
            t.setName((String) s[0]);
            t.setDescription((String) s[1]);
            t.setCoinReward((Integer) s[2]);
            t.setRequireConfirm((Integer) s[3]);
            t.setDailyLimit((Integer) s[4]);
            t.setSortOrder((Integer) s[5]);
            t.setEnabled(1);
            t.setCreatedAt(now);
            t.setUpdatedAt(now);
            rewardTaskMapper.insert(t);
        }
        log.info("已为家庭 {} 初始化赚币任务", fid);
    }
}
