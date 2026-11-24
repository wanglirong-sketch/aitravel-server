package com.example.aitravelserver.controller;

import com.example.aitravelserver.common.LlmClient;
import com.example.aitravelserver.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第 8 步验收专用：只看模型本身调不调得通，不查库。
 * 验收完可以保留当自检接口，也可以在项目收尾时删掉。
 */
@RestController
@RequestMapping("/api/ai")
public class AiTestController {

    private static final String SYSTEM =
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

    public AiTestController(LlmClient llm) {
        this.llm = llm;
    }

    @PostMapping("/test")
    public Result<Map<String, Object>> test(@RequestBody Map<String, String> req) {
        String prompt = req.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            prompt = "广州两天想看日落顺便吃早茶";
        }

        long start = System.currentTimeMillis();
        String content = llm.chat(SYSTEM, prompt);
        long cost = System.currentTimeMillis() - start;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("costMs", cost);
        data.put("length", content.length());
        data.put("content", content);
        return Result.ok(data);
    }
}