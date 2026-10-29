package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.Dish;
import com.wrt.excel.order.service.DishService;
import com.wrt.excel.order.vo.DishVO;
import com.wrt.excel.system.interfance.OperationLogAnnotation;
import com.wrt.excel.system.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜品控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/dish")
@CrossOrigin(origins = "*")
public class DishController {
    
    @Autowired
    private DishService dishService;
    
    @Autowired
    private FileUploadService fileUploadService;
    
    /**
     * 获取所有菜品
     */
    @GetMapping("/list")
    public Map<String, Object> getAllDishes() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DishVO> dishes = dishService.getAllDishes();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dishes);
        } catch (Exception e) {
            log.error("获取菜品失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取所有菜品（包括下架的）- 用于后台管理
     */
    @GetMapping("/all")
    public Map<String, Object> getAllDishesWithSoldOut() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DishVO> dishes = dishService.getAllDishesWithSoldOut();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dishes);
        } catch (Exception e) {
            log.error("获取所有菜品失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 添加菜品
     */
    @PostMapping("/add")
    @OperationLogAnnotation(
        module = "菜品管理",
        operationType = "新增",
        description = "添加新菜品",
        recordParams = true,
        recordResult = false
    )
    public Map<String, Object> addDish(@RequestBody Dish dish) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 设置默认值
            if (dish.getIsSoldOut() == null) {
                dish.setIsSoldOut(0);
            }
            if (dish.getIsSignature() == null) {
                dish.setIsSignature(0);
            }
            if (dish.getIsNew() == null) {
                dish.setIsNew(0);
            }
            if (dish.getIsRecommended() == null) {
                dish.setIsRecommended(0);
            }
            if (dish.getSortOrder() == null) {
                dish.setSortOrder(0);
            }
            // MetaObjectHandler missing: FieldFill does not run; set timestamps for recommended ORDER BY created_at
            LocalDateTime now = LocalDateTime.now();
            if (dish.getCreatedAt() == null) {
                dish.setCreatedAt(now);
            }
            if (dish.getUpdatedAt() == null) {
                dish.setUpdatedAt(now);
            }
            
            boolean success = dishService.addDish(dish);
            if (success) {
                result.put("code", 200);
                result.put("message", "添加菜品成功");
            } else {
                result.put("code", 500);
                result.put("message", "添加菜品失败");
            }
        } catch (Exception e) {
            log.error("添加菜品失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 根据分类获取菜品
     */
    @GetMapping("/category/{categoryId}")
    public Map<String, Object> getDishesByCategory(@PathVariable Long categoryId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DishVO> dishes = dishService.getDishesByCategoryId(categoryId);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dishes);
        } catch (Exception e) {
            log.error("获取分类菜品失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取推荐菜品
     */
    @GetMapping("/recommended")
    public Map<String, Object> getRecommendedDishes() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DishVO> dishes = dishService.getRecommendedDishes();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dishes);
        } catch (Exception e) {
            log.error("获取推荐菜品失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 鑾峰彇鏂板搧
     */
    @GetMapping("/new")
    public Map<String, Object> getNewDishes() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DishVO> dishes = dishService.getNewDishes();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dishes);
        } catch (Exception e) {
            log.error("鑾峰彇鏂板搧澶辫触", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
        /**
     * 获取拿手菜
     */
    @GetMapping("/signature")
    public Map<String, Object> getSignatureDishes() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DishVO> dishes = dishService.getSignatureDishes();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dishes);
        } catch (Exception e) {
            log.error("获取拿手菜失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 随机惊喜菜品
     */
    @GetMapping("/surprise")
    public Map<String, Object> getSurpriseDish() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> rec = dishService.getSurpriseRecommendation();
            Dish dish = rec == null ? null : (Dish) rec.get("dish");
            String reason = rec == null || rec.get("reason") == null ? "" : String.valueOf(rec.get("reason"));
            String source = rec == null || rec.get("source") == null ? "random" : String.valueOf(rec.get("source"));
            if (dish == null) {
                result.put("code", 404);
                result.put("message", "暂时没有可推荐的菜品");
                return result;
            }
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dish);
            result.put("message_tip", reason);
            result.put("reason", reason);
            result.put("source", source);
        } catch (Exception e) {
            log.error("获取惊喜菜品失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 多条件筛选菜品
     * @param keyword 搜索关键词
     * @param categoryId 分类ID
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param tag 标签
     * @param sortBy 排序字段
     * @param sortOrder 排序顺序
     */
    @GetMapping("/filter")
    public Map<String, Object> filterDishes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false, defaultValue = "sortOrder") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DishVO> dishes = dishService.filterDishes(keyword, categoryId, minPrice, maxPrice, tag, sortBy, sortOrder);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dishes);
            result.put("count", dishes.size());
        } catch (Exception e) {
            log.error("筛选菜品失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 更新菜品信息
     */
    @PostMapping("/update")
    public Map<String, Object> updateDish(@RequestBody Dish dish) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = dishService.updateDish(dish);
            if (success) {
                result.put("code", 200);
                result.put("message", "更新菜品成功");
            } else {
                result.put("code", 500);
                result.put("message", "更新菜品失败");
            }
        } catch (Exception e) {
            log.error("更新菜品失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 切换菜品售罄状态
     */
    @PostMapping("/toggleSoldOut")
    public Map<String, Object> toggleSoldOut(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long dishId = Long.valueOf(params.get("dishId").toString());
            Integer currentStatus = Integer.valueOf(params.get("isSoldOut").toString());
            
            boolean success = dishService.toggleSoldOut(dishId, currentStatus);
            if (success) {
                result.put("code", 200);
                result.put("message", currentStatus == 1 ? "上架成功" : "下架成功");
            } else {
                result.put("code", 500);
                result.put("message", "操作失败");
            }
        } catch (Exception e) {
            log.error("切换菜品售罄状态失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 清除菜品图片 (set imageUrl NULL)
     * POST /api/dish/clearImage  body: {dishId:1}
     * DELETE /api/dish/{id}/image
     */
    @PostMapping("/clearImage")
    public Map<String, Object> clearImagePost(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long dishId = Long.valueOf(params.get("dishId").toString());
            boolean success = dishService.clearImage(dishId);
            if (success) {
                result.put("code", 200);
                result.put("message", "图片已清除");
            } else {
                result.put("code", 500);
                result.put("message", "清除图片失败");
            }
        } catch (Exception e) {
            log.error("清除菜品图片失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{id}/image")
    public Map<String, Object> clearImageDelete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = dishService.clearImage(id);
            if (success) {
                result.put("code", 200);
                result.put("message", "图片已清除");
            } else {
                result.put("code", 500);
                result.put("message", "清除图片失败");
            }
        } catch (Exception e) {
            log.error("清除菜品图片失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 上传菜品图片
     */
    @PostMapping("/uploadImage")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            String imageUrl = fileUploadService.uploadImage(file);
            result.put("code", 200);
            result.put("message", "图片上传成功");
            result.put("data", imageUrl);
        } catch (Exception e) {
            log.error("图片上传失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 删除菜品
     */
    @DeleteMapping("/delete/{id}")
    @OperationLogAnnotation(
        module = "菜品管理",
        operationType = "删除",
        description = "删除菜品",
        recordParams = true,
        recordResult = false
    )
    public Map<String, Object> deleteDish(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = dishService.deleteDish(id);
            if (success) {
                result.put("code", 200);
                result.put("message", "删除菜品成功");
            } else {
                result.put("code", 500);
                result.put("message", "删除菜品失败");
            }
        } catch (Exception e) {
            log.error("删除菜品失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
