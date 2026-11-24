package com.example.aitravelserver.controller;

import com.example.aitravelserver.common.Result;
import com.example.aitravelserver.entity.Itinerary;
import com.example.aitravelserver.service.ItineraryService;
import com.example.aitravelserver.vo.ItineraryDetailVO;
import com.example.aitravelserver.vo.ItineraryPlanVO;
import com.example.aitravelserver.vo.PlanItemVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    /** 保存生成结果 */
    @PostMapping
    public Result<Long> save(@RequestBody ItineraryPlanVO plan) {
        return Result.ok(itineraryService.save(plan));
    }

    /** 我的行程列表 */
    @GetMapping
    public Result<List<Itinerary>> list() {
        return Result.ok(itineraryService.myList());
    }

    /** 行程详情 */
    @GetMapping("/{id}")
    public Result<ItineraryDetailVO> detail(@PathVariable Long id) {
        return Result.ok(itineraryService.detail(id));
    }

    /** 改一个节点 */
    @PutMapping("/{id}/items/{itemId}")
    public Result<String> updateItem(@PathVariable Long id, @PathVariable Long itemId,
                                     @RequestBody PlanItemVO patch) {
        itineraryService.updateItem(id, itemId, patch);
        return Result.ok("已保存");
    }

    /** 删一个节点 */
    @DeleteMapping("/{id}/items/{itemId}")
    public Result<String> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        itineraryService.deleteItem(id, itemId);
        return Result.ok("已删除");
    }

    /** 删整份行程 */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        itineraryService.delete(id);
        return Result.ok("已删除");
    }

    /** 把微调后的结果覆盖到这份行程上 */
    @PutMapping("/{id}")
    public Result<String> replace(@PathVariable Long id, @RequestBody ItineraryPlanVO plan) {
        itineraryService.replace(id, plan);
        return Result.ok("已更新");
    }

}