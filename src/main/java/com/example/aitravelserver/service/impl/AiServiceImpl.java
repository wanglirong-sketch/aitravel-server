package com.example.aitravelserver.service.impl;

import com.example.aitravelserver.common.LlmClient;
import com.example.aitravelserver.common.exception.BizException;
import com.example.aitravelserver.entity.Poi;
import com.example.aitravelserver.service.AiService;
import com.example.aitravelserver.service.PoiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiServiceImpl implements AiService {

    private static final String INTENT_SYSTEM =
            "你是旅游意图解析器。根据用户这句话抽取旅行需求，只输出一个 JSON 对象，"
                    + "不要解释，不要加代码块标记，不要输出任何 JSON 之外的内容。\n"
                    + "字段规则：\n"
                    + "city：用户提到的目的地城市，中文全称，如\"广州\"；没提城市则为空字符串。\n"
                    + "type：主诉求类型，只能是 1、2、3、4 或 0。1=景点观光，2=美食吃饭，"
                    + "3=游玩项目（游船、乐园、演出），4=拍照打卡（夜景、地标机位），无法判断给 0。\n"
                    + "keywords：最多 3 个，从用户原话提炼的具体名词或标签，如\"早茶\"\"日落\"\"长隆\"，"
                    + "不要放动词，不要放城市名。\n"
                    + "days：天数，整数，没提给 1。\n"
                    + "必须根据用户输入真实抽取，禁止输出模板占位值。\n"
                    + "示例（只示意格式，内容不要照抄）：\n"
                    + "输入\"成都三天想吃火锅顺便逛宽窄巷子\"，输出"
                    + "{\"city\":\"成都\",\"type\":2,\"keywords\":[\"火锅\",\"宽窄巷子\"],\"days\":3}";

    private final LlmClient llm;
    private final PoiService poiService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiServiceImpl(LlmClient llm, PoiService poiService) {
        this.llm = llm;
        this.poiService = poiService;
    }

    @Override
    public List<Poi> recommend(String query) {
        if (!StringUtils.hasText(query)) {
            return poiService.search("");
        }

        try {
            String raw = llm.chat(INTENT_SYSTEM, query);
            JsonNode node = mapper.readTree(extractJson(raw));

            String city = node.path("city").asText("");
            int type = node.path("type").asInt(0);

            StringBuilder key = new StringBuilder();
            node.path("keywords").forEach(k -> key.append(k.asText()).append(' '));

            if (!StringUtils.hasText(city) && !StringUtils.hasText(key.toString())) {
                return poiService.search(query);
            }

            String merged = (city + " " + mapTypeWord(type) + " " + key.toString()).trim();
            return poiService.search(merged);
        } catch (Exception e) {
            // 模型超时、密钥失效、输出不是 JSON，一律退回规则检索
            return poiService.search(query);
        }
    }

    /**
     * 把模型给的 type 数字翻回中文词，便于复用 search 的类型词典
     */
    private String mapTypeWord(int type) {
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
                return "";
        }
    }

    /**
     * 模型偶尔会把 JSON 包在 ``` 里，这里只取第一个花括号到最后一个花括号
     */
    private String extractJson(String s) {
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');

        if (start < 0 || end <= start) {
            throw new BizException(500, "模型输出不是 JSON");
        }

        return s.substring(start, end + 1);
    }
}
