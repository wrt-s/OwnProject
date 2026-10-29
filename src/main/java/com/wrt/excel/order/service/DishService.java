package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.Dish;
import com.wrt.excel.order.mapper.DishMapper;
import com.wrt.excel.order.vo.DishVO;
import com.wrt.excel.system.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜品服务
 */
@Slf4j
@Service
public class DishService {
    
    @Autowired
    private DishMapper dishMapper;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    @Lazy
    private DeepSeekService deepSeekService;

    @Autowired(required = false)
    private FileUploadService fileUploadService;

    /** Refresh MinIO signatures (or public URL) without persisting the temporary link. */
    public void refreshImage(Dish dish) {
        if (dish == null || fileUploadService == null) return;
        dish.setImageUrl(fileUploadService.toReadableUrl(dish.getImageUrl()));
    }

    public String readableImage(String stored) {
        if (fileUploadService == null) return stored;
        return fileUploadService.toReadableUrl(stored);
    }
    
    /**
     * 获取所有菜品（转换为VO）- 默认只显示在售菜品
     */
    public List<DishVO> getAllDishes() {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getIsSoldOut, 0)
               .orderByAsc(Dish::getSortOrder);
        List<Dish> dishes = dishMapper.selectList(wrapper);
        return convertToVOList(dishes);
    }
    
    /**
     * 获取所有菜品（包括下架的）- 用于后台管理
     */
    public List<DishVO> getAllDishesWithSoldOut() {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Dish::getSortOrder);
        List<Dish> dishes = dishMapper.selectList(wrapper);
        return convertToVOList(dishes);
    }
    
    /**
     * 添加菜品
     */
    public boolean addDish(Dish dish) {
        try {
            LocalDateTime now = LocalDateTime.now();
            if (dish.getCreatedAt() == null) {
                dish.setCreatedAt(now);
            }
            if (dish.getUpdatedAt() == null) {
                dish.setUpdatedAt(now);
            }
            int result = dishMapper.insert(dish);
            return result > 0;
        } catch (Exception e) {
            log.error("娣诲姞鑿滃搧澶辫触", e);
            return false;
        }
    }
    
    /**
     * 根据分类ID获取菜品 - 默认只显示在售菜品
     */
    public List<DishVO> getDishesByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getCategoryId, categoryId)
               .eq(Dish::getIsSoldOut, 0)
               .orderByAsc(Dish::getSortOrder);
        List<Dish> dishes = dishMapper.selectList(wrapper);
        return convertToVOList(dishes);
    }
    
    /**
     * 获取推荐菜品
     */
    public List<DishVO> getRecommendedDishes() {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getIsRecommended, 1)
               .eq(Dish::getIsSoldOut, 0)
               .orderByDesc(Dish::getCreatedAt)
               .last("LIMIT 10");
        List<Dish> dishes = dishMapper.selectList(wrapper);
        return convertToVOList(dishes);
    }
    
    /**
     * 获取拿手菜
     */
    public List<DishVO> getSignatureDishes() {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getIsSignature, 1)
               .eq(Dish::getIsSoldOut, 0);
        List<Dish> dishes = dishMapper.selectList(wrapper);
        return convertToVOList(dishes);
    }
    
    /**
     * 获取新品
     */
    public List<DishVO> getNewDishes() {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getIsNew, 1)
               .eq(Dish::getIsSoldOut, 0)
               .orderByDesc(Dish::getCreatedAt);
        List<Dish> dishes = dishMapper.selectList(wrapper);
        return convertToVOList(dishes);
    }
    
    /**
     * 根据ID获取菜品
     */
    public Dish getDishById(Long id) {
        return dishMapper.selectById(id);
    }
    
    /**
     * 随机获取一个菜品（用于惊喜功能）- 只从在售菜品中选择
     */

    /**
     * 今日惊喜推荐：优先 DeepSeek 从目录挑选并附短理由；无 key / 失败则随机回退。
     */
    public Map<String, Object> getSurpriseRecommendation() {
        Map<String, Object> out = new LinkedHashMap<>();
        Dish dish = null;
        String reason = null;
        String source = "random";
        try {
            Map<String, Object> ai = deepSeekService.pickSurpriseDish();
            if (ai != null && ai.get("dish") instanceof Dish) {
                dish = (Dish) ai.get("dish");
                Object r = ai.get("reason");
                reason = r == null ? null : String.valueOf(r);
                source = "ai";
            }
        } catch (Exception e) {
            log.warn("AI 惊喜推荐失败，回退随机: {}", e.toString());
        }
        if (dish == null) {
            dish = getRandomDish();
            source = "random";
            if (reason == null || reason.isBlank()) {
                reason = dish == null ? "暂时没有可推荐的菜哦" : ("今天就开盲盒试试「" + dish.getName() + "」吧～");
            }
        }
        out.put("dish", dish);
        out.put("reason", reason);
        out.put("source", source);
        return out;
    }

    public Dish getRandomDish() {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getIsSoldOut, 0);
        List<Dish> dishes = dishMapper.selectList(wrapper);
        if (dishes.isEmpty()) {
            return null;
        }
        int randomIndex = (int) (Math.random() * dishes.size());
        return dishes.get(randomIndex);
    }
    
    /**
     * 多条件筛选菜品 - 默认只显示在售菜品
     * @param keyword 搜索关键词（菜品名称或描述）
     * @param categoryId 分类ID
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param tag 标签筛选
     * @param sortBy 排序字段（price, createdAt）
     * @param sortOrder 排序顺序（asc, desc）
     * @return 筛选后的菜品列表
     */
    public List<DishVO> filterDishes(String keyword, Long categoryId, 
                                     Integer minPrice, Integer maxPrice, 
                                     String tag, String sortBy, String sortOrder) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        
        // 默认只显示在售菜品
        wrapper.eq(Dish::getIsSoldOut, 0);
        
        // 关键词搜索（名称或描述）
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Dish::getName, keyword)
                             .or()
                             .like(Dish::getDescription, keyword));
        }
        
        // 分类筛选
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Dish::getCategoryId, categoryId);
        }
        
        // 价格范围筛选
        if (minPrice != null && minPrice > 0) {
            wrapper.ge(Dish::getPrice, minPrice);
        }
        if (maxPrice != null && maxPrice > 0) {
            wrapper.le(Dish::getPrice, maxPrice);
        }
        
        // 标签筛选
        if (tag != null && !tag.trim().isEmpty()) {
            wrapper.like(Dish::getTags, tag);
        }
        
        // 排序
        if ("price".equals(sortBy)) {
            if ("desc".equals(sortOrder)) {
                wrapper.orderByDesc(Dish::getPrice);
            } else {
                wrapper.orderByAsc(Dish::getPrice);
            }
        } else if ("createdAt".equals(sortBy)) {
            if ("desc".equals(sortOrder)) {
                wrapper.orderByDesc(Dish::getCreatedAt);
            } else {
                wrapper.orderByAsc(Dish::getCreatedAt);
            }
        } else {
            // 默认按排序字段排序
            wrapper.orderByAsc(Dish::getSortOrder);
        }
        
        List<Dish> dishes = dishMapper.selectList(wrapper);
        return convertToVOList(dishes);
    }
    
    /**
     * 转换为VO列表
     */
    private List<DishVO> convertToVOList(List<Dish> dishes) {
        return dishes.stream().map(dish -> {
            DishVO vo = new DishVO();
            BeanUtils.copyProperties(dish, vo);
            vo.setImageUrl(readableImage(dish.getImageUrl()));
            return vo;
        }).collect(Collectors.toList());
    }
    
    /**
     * 更新菜品信息
     */
    public boolean updateDish(Dish dish) {
        try {
            if (dish != null && dish.getImageUrl() != null && dish.getImageUrl().trim().isEmpty()) {
                dish.setImageUrl(null); // ALWAYS strategy writes SQL NULL
            }
                        if (dish != null) {
                dish.setUpdatedAt(LocalDateTime.now());
            }
            int result = dishMapper.updateById(dish);
            return result > 0;
        } catch (Exception e) {
            log.error("更新菜品失败", e);
            return false;
        }
    }
    
    /**
     * 切换菜品售罄状态
     */
    public boolean toggleSoldOut(Long dishId, Integer currentStatus) {
        try {
            Dish existing = dishMapper.selectById(dishId);
            if (existing == null) {
                return false;
            }
            // 以数据库当前状态为准切换，忽略客户端可能过期的 currentStatus
            int dbStatus = existing.getIsSoldOut() == null ? 0 : existing.getIsSoldOut();
            Dish dish = new Dish();
            dish.setId(dishId);
            dish.setIsSoldOut(dbStatus == 1 ? 0 : 1);
            int result = dishMapper.updateById(dish);
            return result > 0;
        } catch (Exception e) {
            log.error("切换菜品售罄状态失败", e);
            return false;
        }
    }
    
    /**
     * 删除菜品
     */
    /**
     * 娓呴櫎鑿滃搧鍥剧墖锛坕mageUrl -> NULL锛夛紝渚濊禆 Dish.imageUrl FieldStrategy.ALWAYS
     */
    public boolean clearImage(Long dishId) {
        try {
            Dish existing = dishMapper.selectById(dishId);
            if (existing == null) {
                return false;
            }
            Dish dish = new Dish();
            dish.setId(dishId);
            dish.setImageUrl(null);
            dish.setUpdatedAt(LocalDateTime.now());
            int result = dishMapper.updateById(dish);
            return result > 0;
        } catch (Exception e) {
            log.error("娓呴櫎鑿滃搧鍥剧墖澶辫触", e);
            return false;
        }
    }
        public boolean deleteDish(Long dishId) {
        try {
            int result = dishMapper.deleteById(dishId);
            return result > 0;
        } catch (Exception e) {
            log.error("删除菜品失败", e);
            return false;
        }
    }
}
