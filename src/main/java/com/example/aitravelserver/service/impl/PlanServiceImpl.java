package com.example.aitravelserver.service.impl;

import com.example.aitravelserver.common.LlmClient;
import com.example.aitravelserver.common.exception.BizException;
import com.example.aitravelserver.entity.Poi;
import com.example.aitravelserver.service.ItineraryService;
import com.example.aitravelserver.service.PoiService;
import com.example.aitravelserver.service.PlanService;
import com.example.aitravelserver.vo.DayPlanVO;
import com.example.aitravelserver.vo.ItineraryDetailVO;
import com.example.aitravelserver.vo.ItineraryPlanVO;
import com.example.aitravelserver.vo.PlanItemVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 行程主链路：检索候选点位 → 交给模型按结构生成 → 校验后返回。
 * 库内点位优先；库内没有的城市，允许模型按记忆自由推荐，poiId 置空。
 */
@Service
public class PlanServiceImpl implements PlanService {

    private static final String SYSTEM =
            "你是行程规划助手。根据候选点位和用户需求，生成逐日行程，只输出一个 JSON 对象，"
                    + "不要解释，不要加代码块标记。\n"
                    + "硬性规则：\n"
                    + "1. 优先使用候选清单里的点位，poiId 必须来自清单原值；若候选清单为空，"
                    + "可根据城市推荐真实存在的知名点位，此时 poiId 填 -1。\n"
                    + "2. 每天安排 3 到 5 个点位，同一天的点位要在地理上邻近，不要横穿城市来回跑。\n"
                    + "3. 时间连续合理，要含用餐时段，单项停留 60 到 180 分钟。\n"
                    + "4. 输出结构固定为：{\"title\":\"\",\"summary\":\"\",\"city\":\"\",\"totalCost\":0,"
                    + "\"days\":[{\"day\":1,\"items\":[{\"time\":\"09:00\",\"poiId\":2,\"name\":\"\""
                    + ",\"type\":2,\"stayMinutes\":90,\"transport\":\"步行\",\"cost\":80,\"note\":\"\"}]}]}\n"
                    + "5. type 取值 1景点 2美食 3项目 4打卡。\n"
                    + "6. transport 取值只能是：步行、地铁、公交、打车。"
                    + "7. 任何字段值内部都不要使用英文双引号和英文逗号；需要引用时用中文引号「」，note 控制在 30 字以内。\n";

    private static final String REFINE_SYSTEM =
            "你是行程修订助手。用户会给你一份已有的行程 JSON 和一个修改要求，"
                    + "你要在保留原有结构的前提下，只改动用户明确要求的部分，其余内容尽量原样保留。"
                    + "只输出一个 JSON 对象，不要解释，不要加代码块标记。\n"
                    + "硬性规则：\n"
                    + "1. 输出结构与原行程完全一致：title、summary、city、totalCost、days[].day、days[].items[]。\n"
                    + "2. 每个 item 必须包含 time、poiId、name、type、stayMinutes、transport、cost、note。\n"
                    + "3. poiId 只能取自候选点位清单；清单外的新点位，poiId 填 -1。\n"
                    + "4. 用户没提到的天、没提到的点位，保持原样不动，不允许优化、不允许重排。\n"
                    + "5. 天数不变，除非用户明确要求增减天数。\n"
                    + "6. transport 取值只能是：步行、地铁、公交、打车。"
                    + "7. 字段值内部不要使用英文双引号，需要引用用「」。\n";

    private static final int MAX_CANDIDATES = 20;

    private final LlmClient llm;
    private final PoiService poiService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ItineraryService itineraryService;

    public PlanServiceImpl(LlmClient llm,
                           PoiService poiService,
                           ItineraryService itineraryService) {
        this.llm = llm;
        this.poiService = poiService;
        this.itineraryService = itineraryService;
    }

    @Override
    public ItineraryPlanVO plan(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BizException(400, "请输入行程需求");
        }

        List<Poi> candidates = poiService.search(query);
        if (candidates.isEmpty()) {
            candidates = new ArrayList<>();
        }
        if (candidates.size() > MAX_CANDIDATES) {
            candidates = candidates.subList(0, MAX_CANDIDATES);
        }

        String candidateText = candidates.isEmpty()
                ? "候选清单为空，请根据用户提到的城市，推荐真实存在的知名点位。\n"
                : buildCandidateText(candidates);

        String userPrompt = "用户需求：" + query + "\n\n候选点位（id | 名称 | 类型 | 城市 | 地址 | 评分 | 人均）：\n"
                + candidateText;

        String raw = llm.chat(SYSTEM, userPrompt, 0.3, 3500);
        return parse(raw, candidates);
    }

    @Override
    public List<Poi> searchCandidates(String query) {
        List<Poi> candidates = poiService.search(query);
        if (candidates.size() > MAX_CANDIDATES) {
            candidates = candidates.subList(0, MAX_CANDIDATES);
        }
        return candidates;
    }

    @Override
    public String buildPrompt(String query, List<Poi> candidates) {
        String candidateText = candidates.isEmpty()
                ? "候选清单为空，请根据用户提到的城市，推荐真实存在的知名点位。\n"
                : buildCandidateText(candidates);

        return "用户需求：" + query + "\n\n候选点位（id | 名称 | 类型 | 城市 | 地址 | 评分 | 人均）：\n"
                + candidateText;
    }

    @Override
    public ItineraryPlanVO parseText(String text, List<Poi> candidates) {
        return parse(text, candidates);
    }

    @Override
    public ItineraryPlanVO refine(Long itineraryId, String instruction) {
        if (!StringUtils.hasText(instruction)) {
            throw new BizException(400, "请填写修改要求");
        }

        ItineraryDetailVO detail = itineraryService.detail(itineraryId);

        List<Poi> candidates = new ArrayList<>(
                poiService.list(detail.getCity(), null, null, null)
        );
        candidates.addAll(itineraryService.poisOf(itineraryId));

        Map<Long, Poi> unique = new HashMap<>();
        for (Poi p : candidates) {
            unique.put(p.getId(), p);
        }

        candidates = new ArrayList<>(unique.values());
        if (candidates.size() > MAX_CANDIDATES) {
            candidates = candidates.subList(0, MAX_CANDIDATES);
        }

        String originalJson = toJson(detail);

        String userPrompt = "原行程 JSON：\n" + originalJson
                + "\n\n用户修改要求：" + instruction
                + "\n\n候选点位（id | 名称 | 类型 | 城市 | 地址 | 评分 | 人均）：\n"
                + buildCandidateText(candidates);

        String raw = llm.chat(REFINE_SYSTEM, userPrompt, 0.3, 2500);
        return parse(raw, candidates);
    }

    @Override
    public String systemPrompt() {
        return SYSTEM;
    }

    /**
     * 候选清单转成文本，每行一条
     */
    private String buildCandidateText(List<Poi> list) {
        StringBuilder sb = new StringBuilder();
        for (Poi p : list) {
            sb.append(p.getId()).append(" | ")
                    .append(p.getName()).append(" | ")
                    .append(typeName(p.getType())).append(" | ")
                    .append(p.getCity()).append(" | ")
                    .append(p.getAddress() == null ? "" : p.getAddress()).append(" | ")
                    .append(p.getRating() == null ? "" : p.getRating()).append(" | ")
                    .append(p.getPrice() == null ? "" : p.getPrice())
                    .append('\n');
        }
        return sb.toString();
    }

    /**
     * 将类型数字转成中文
     */
    private String typeName(Integer type) {
        if (type == null) {
            return "其他";
        }
        switch (type) {
            case 1:
                return "景点";
            case 2:
                return "美食";
            case 3:
                return "项目";
            case 4:
                return "打卡";
            default:
                return "其他";
        }
    }

    /**
     * 解析模型输出；库内点位以库为准，库外点位保留模型名称、poiId 置空
     */
    private ItineraryPlanVO parse(String raw, List<Poi> candidates) {
        try {
            JsonNode root = mapper.readTree(extractJson(raw));

            Map<Long, Poi> allowed = new HashMap<>();
            for (Poi p : candidates) {
                allowed.put(p.getId(), p);
            }

            ItineraryPlanVO vo = new ItineraryPlanVO();
            vo.setTitle(root.path("title").asText(""));
            vo.setSummary(root.path("summary").asText(""));
            vo.setTotalCost(root.path("totalCost").asInt(0));

            vo.setCity(root.path("city").asText(""));
            if (vo.getCity().isEmpty() && !candidates.isEmpty()) {
                vo.setCity(candidates.get(0).getCity());
            }

            List<DayPlanVO> days = new ArrayList<>();
            int sum = 0;

            for (JsonNode dayNode : root.path("days")) {
                DayPlanVO day = new DayPlanVO();
                day.setDay(dayNode.path("day").asInt(days.size() + 1));

                List<PlanItemVO> items = new ArrayList<>();
                for (JsonNode itemNode : dayNode.path("items")) {
                    long poiId = itemNode.path("poiId").asLong(-1);
                    Poi poi = allowed.get(poiId);

                    PlanItemVO item = new PlanItemVO();
                    item.setTime(itemNode.path("time").asText(""));
                    item.setType(itemNode.path("type").asInt(0));
                    item.setStayMinutes(itemNode.path("stayMinutes").asInt(90));
                    item.setTransport(itemNode.path("transport").asText(""));
                    item.setCost(itemNode.path("cost").asInt(0));
                    item.setNote(itemNode.path("note").asText(""));

                    if (poi != null) {
                        item.setPoiId(poiId);
                        item.setName(itemNode.path("name").asText(poi.getName()));
                        item.setType(itemNode.path("type").asInt(poi.getType()));
                    } else {
                        String modelName = itemNode.path("name").asText("");
                        if (modelName.isEmpty()) {
                            continue;
                        }
                        item.setPoiId(null);
                        item.setName(modelName);
                    }

                    items.add(item);
                    sum += item.getCost();
                }

                if (items.isEmpty()) {
                    continue;
                }
                day.setItems(items);
                days.add(day);
            }

            if (days.isEmpty()) {
                throw new BizException(500, "模型没有生成有效行程，原始输出: " + raw);
            }

            vo.setDays(days);
            if (vo.getTotalCost() <= 0) {
                vo.setTotalCost(sum);
            }
            return vo;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "行程解析失败: " + e.getMessage());
        }
    }

    /**
     * 模型偶尔会把 JSON 包在 ``` 或解释文字里，只取首个花括号到末尾花括号
     */
    private String extractJson(String s) {
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BizException(500, "模型输出不是 JSON");
        }
        return s.substring(start, end + 1);
    }

    /**
     * 把详情 VO 转成行程 VO，用于喂给模型
     */
    private String toJson(ItineraryDetailVO detail) {
        try {
            ItineraryPlanVO vo = new ItineraryPlanVO();
            vo.setTitle(detail.getTitle());
            vo.setSummary(detail.getSummary());
            vo.setCity(detail.getCity());
            vo.setTotalCost(detail.getBudget());
            vo.setDays(detail.getDays());
            return mapper.writeValueAsString(vo);
        } catch (Exception e) {
            throw new BizException(500, "原行程转换失败");
        }
    }
}
