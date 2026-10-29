package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.MoodLog;
import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.mapper.MoodLogMapper;
import com.wrt.excel.order.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 心情记录服务 - 家庭共享；每人每日一条 upsert
 */
@Slf4j
@Service
public class MoodService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** 固定 10 种心情：code=中文标签 */
    public static final List<Map<String, String>> MOOD_CATALOG = List.of(
            Map.of("code", "开心", "emoji", "😄", "label", "开心"),
            Map.of("code", "平静", "emoji", "😌", "label", "平静"),
            Map.of("code", "想你", "emoji", "🥰", "label", "想你"),
            Map.of("code", "累了", "emoji", "😮‍💨", "label", "累了"),
            Map.of("code", "低落", "emoji", "😢", "label", "低落"),
            Map.of("code", "生气", "emoji", "😠", "label", "生气"),
            Map.of("code", "想吃", "emoji", "😋", "label", "想吃"),
            Map.of("code", "无聊", "emoji", "😑", "label", "无聊"),
            Map.of("code", "焦虑", "emoji", "😰", "label", "焦虑"),
            Map.of("code", "睡意", "emoji", "😴", "label", "睡意")
    );

    private static final Set<String> ALLOWED = Set.of(
            "开心", "平静", "想你", "累了", "低落", "生气", "想吃", "无聊", "焦虑", "睡意"
    );

    @Autowired
    private MoodLogMapper moodLogMapper;

    @Autowired
    private UserMapper userMapper;

    public LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static String emojiOf(String mood) {
        if (mood == null) return "💭";
        for (Map<String, String> m : MOOD_CATALOG) {
            if (mood.equals(m.get("code"))) return m.get("emoji");
        }
        return "💭";
    }

    public void assertValidMood(String mood) {
        if (!StringUtils.hasText(mood) || !ALLOWED.contains(mood.trim())) {
            throw new IllegalArgumentException("心情不在可选列表中");
        }
    }

    public MoodLog getById(Long id) {
        return id == null ? null : moodLogMapper.selectById(id);
    }

    public MoodLog getUserMoodOnDate(Long userId, LocalDate date) {
        if (userId == null || date == null) return null;
        return moodLogMapper.selectOne(new LambdaQueryWrapper<MoodLog>()
                .eq(MoodLog::getUserId, userId)
                .eq(MoodLog::getMoodDate, date)
                .last("LIMIT 1"));
    }

    /**
     * 同家庭「对方」：family 内除自己以外的第一个成员（情侣二人场景）
     */
    public User findPartner(Long familyId, Long selfUserId) {
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        List<User> list = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getFamilyId, fid)
                .orderByAsc(User::getId));
        if (list == null || list.isEmpty()) return null;
        for (User u : list) {
            if (u != null && selfUserId != null && !Objects.equals(u.getId(), selfUserId)) {
                return u;
            }
        }
        return null;
    }

    /**
     * 保存/更新「今天」的心情（upsert by user_id + mood_date）
     */
    public MoodLog upsertToday(Long userId, Long familyId, String mood, String note) {
        assertValidMood(mood);
        LocalDate date = today();
        String cleanNote = note == null ? null : note.trim();
        if (cleanNote != null && cleanNote.length() > 200) {
            cleanNote = cleanNote.substring(0, 200);
        }
        MoodLog existing = getUserMoodOnDate(userId, date);
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (existing != null) {
            // 防御：绝不更新非本人行
            if (!Objects.equals(existing.getUserId(), userId)) {
                throw new IllegalStateException("不能修改对方的心情");
            }
            existing.setMood(mood.trim());
            existing.setNote(StringUtils.hasText(cleanNote) ? cleanNote : null);
            existing.setFamilyId(familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID);
            existing.setUpdatedAt(now);
            moodLogMapper.updateById(existing);
            log.info("更新今日心情 user={} mood={}", userId, mood);
            return existing;
        }
        MoodLog row = new MoodLog();
        row.setUserId(userId);
        row.setFamilyId(familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID);
        row.setMood(mood.trim());
        row.setNote(StringUtils.hasText(cleanNote) ? cleanNote : null);
        row.setMoodDate(date);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        moodLogMapper.insert(row);
        log.info("新增今日心情 user={} mood={}", userId, mood);
        return row;
    }

    public List<MoodLog> listFamilyRecent(Long familyId, int limit) {
        int lim = limit <= 0 ? 30 : Math.min(limit, 100);
        return moodLogMapper.selectList(new LambdaQueryWrapper<MoodLog>()
                .eq(MoodLog::getFamilyId, familyId)
                .orderByDesc(MoodLog::getMoodDate)
                .orderByDesc(MoodLog::getUpdatedAt)
                .last("LIMIT " + lim));
    }

    /** 「开心类」用于简单趋势：开心 / 想你 / 想吃 */
    public static final Set<String> HAPPY_MOODS = Set.of("开心", "想你", "想吃");

    public boolean isHappyMood(String mood) {
        return mood != null && HAPPY_MOODS.contains(mood.trim());
    }

    public List<User> listFamilyUsers(Long familyId) {
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        List<User> list = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getFamilyId, fid)
                .orderByAsc(User::getId));
        return list != null ? list : List.of();
    }

    public User findByRole(Long familyId, String role) {
        String want = role == null ? User.ROLE_ORDERER : role.trim().toUpperCase();
        if ("ADMIN".equals(want)) want = User.ROLE_CHEF;
        if ("USER".equals(want)) want = User.ROLE_ORDERER;
        for (User u : listFamilyUsers(familyId)) {
            if (u == null) continue;
            if (want.equals(u.normalizedRole())) return u;
        }
        return null;
    }

    /**
     * 家庭日期区间心情列表（含端点），按日期升序。
     */
    public List<MoodLog> listFamilyRange(Long familyId, LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("日期范围无效");
        if (to.isBefore(from)) {
            LocalDate tmp = from; from = to; to = tmp;
        }
        // 防止过大跨度
        if (from.plusDays(62).isBefore(to)) {
            throw new IllegalArgumentException("日期范围不能超过 62 天");
        }
        return moodLogMapper.selectList(new LambdaQueryWrapper<MoodLog>()
                .eq(MoodLog::getFamilyId, familyId)
                .ge(MoodLog::getMoodDate, from)
                .le(MoodLog::getMoodDate, to)
                .orderByAsc(MoodLog::getMoodDate)
                .orderByAsc(MoodLog::getUserId));
    }

    /**
     * 近 7 天（含今天）周历 + 简单趋势。
     * days[]：固定 LEFT=点单方 RIGHT=做餐方；每格有 mood/emoji 或 empty。
     */
    public Map<String, Object> weekOverview(Long familyId) {
        LocalDate end = today();
        LocalDate start = end.minusDays(6);
        User orderer = findByRole(familyId, User.ROLE_ORDERER);
        User chef = findByRole(familyId, User.ROLE_CHEF);
        Long ordererId = orderer != null ? orderer.getId() : null;
        Long chefId = chef != null ? chef.getId() : null;

        List<MoodLog> rows = listFamilyRange(familyId, start, end);
        Map<String, MoodLog> byKey = new HashMap<>();
        for (MoodLog r : rows) {
            if (r == null || r.getUserId() == null || r.getMoodDate() == null) continue;
            byKey.put(r.getUserId() + "|" + r.getMoodDate(), r);
        }

        List<Map<String, Object>> days = new ArrayList<>();
        int ordererHappy = 0, ordererOther = 0, ordererRecorded = 0;
        int chefHappy = 0, chefOther = 0, chefRecorded = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", d.toString());
            day.put("weekday", d.getDayOfWeek().getValue()); // 1=Mon .. 7=Sun
            day.put("label", weekdayShort(d));

            Map<String, Object> left = cellFor(byKey, ordererId, d);
            Map<String, Object> right = cellFor(byKey, chefId, d);
            day.put("orderer", left);
            day.put("chef", right);
            days.add(day);

            if (Boolean.TRUE.equals(left.get("recorded"))) {
                ordererRecorded++;
                if (isHappyMood(String.valueOf(left.get("mood")))) ordererHappy++;
                else ordererOther++;
            }
            if (Boolean.TRUE.equals(right.get("recorded"))) {
                chefRecorded++;
                if (isHappyMood(String.valueOf(right.get("mood")))) chefHappy++;
                else chefOther++;
            }
        }

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("from", start.toString());
        trend.put("to", end.toString());
        Map<String, Object> oTrend = new LinkedHashMap<>();
        oTrend.put("userId", ordererId);
        oTrend.put("nickname", orderer != null && orderer.getNickname() != null ? orderer.getNickname() : "点单方");
        oTrend.put("recorded", ordererRecorded);
        oTrend.put("happy", ordererHappy);
        oTrend.put("other", ordererOther);
        trend.put("orderer", oTrend);
        Map<String, Object> cTrend = new LinkedHashMap<>();
        cTrend.put("userId", chefId);
        cTrend.put("nickname", chef != null && chef.getNickname() != null ? chef.getNickname() : "做餐方");
        cTrend.put("recorded", chefRecorded);
        cTrend.put("happy", chefHappy);
        cTrend.put("other", chefOther);
        trend.put("chef", cTrend);
        trend.put("summary", buildTrendSummary(ordererHappy, ordererOther, ordererRecorded,
                chefHappy, chefOther, chefRecorded));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", start.toString());
        out.put("to", end.toString());
        out.put("ordererUserId", ordererId);
        out.put("chefUserId", chefId);
        out.put("ordererNickname", orderer != null && orderer.getNickname() != null ? orderer.getNickname() : "点单方");
        out.put("chefNickname", chef != null && chef.getNickname() != null ? chef.getNickname() : "做餐方");
        out.put("days", days);
        out.put("trend", trend);
        out.put("moods", MOOD_CATALOG);
        out.put("happyMoods", List.copyOf(HAPPY_MOODS));
        return out;
    }

    private Map<String, Object> cellFor(Map<String, MoodLog> byKey, Long userId, LocalDate d) {
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("userId", userId);
        if (userId == null) {
            cell.put("recorded", false);
            cell.put("mood", null);
            cell.put("emoji", null);
            cell.put("note", null);
            return cell;
        }
        MoodLog row = byKey.get(userId + "|" + d);
        if (row != null && StringUtils.hasText(row.getMood())) {
            cell.put("recorded", true);
            cell.put("mood", row.getMood());
            cell.put("emoji", emojiOf(row.getMood()));
            cell.put("note", row.getNote());
            cell.put("id", row.getId());
        } else {
            cell.put("recorded", false);
            cell.put("mood", null);
            cell.put("emoji", null);
            cell.put("note", null);
        }
        return cell;
    }

    private static String weekdayShort(LocalDate d) {
        return switch (d.getDayOfWeek()) {
            case MONDAY -> "一";
            case TUESDAY -> "二";
            case WEDNESDAY -> "三";
            case THURSDAY -> "四";
            case FRIDAY -> "五";
            case SATURDAY -> "六";
            case SUNDAY -> "日";
        };
    }

    private static String buildTrendSummary(int oH, int oO, int oR, int cH, int cO, int cR) {
        if (oR == 0 && cR == 0) return "这周还没人记心情，去点一颗小情绪吧～";
        String left = oR == 0 ? "点单方这周还没记"
                : ("点单方开心类 " + oH + "，其他 " + oO);
        String right = cR == 0 ? "做餐方这周还没记"
                : ("做餐方开心类 " + cH + "，其他 " + cO);
        return left + "；" + right + "。";
    }


    public void delete(Long id) {
        moodLogMapper.deleteById(id);
        log.info("删除心情记录 id={}", id);
    }

    public Map<String, Object> nicknameMap(Long familyId) {
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        Map<String, Object> map = new HashMap<>();
        List<User> list = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getFamilyId, fid));
        if (list != null) {
            for (User u : list) {
                if (u != null && u.getId() != null) {
                    map.put(String.valueOf(u.getId()),
                            u.getNickname() != null ? u.getNickname() : ("#" + u.getId()));
                }
            }
        }
        return map;
    }
}
