package com.example.aitravelserver.service.impl;

import com.example.aitravelserver.common.config.AmapProperties;
import com.example.aitravelserver.common.exception.BizException;
import com.example.aitravelserver.entity.Poi;
import com.example.aitravelserver.service.AmapClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 调用高德关键字搜索 API，返回真实点位；不写库，每次实时查询
 */
@Service
public class AmapClientImpl implements AmapClient {

    private final AmapProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate rest;

    /**
     * 演示范围城市：全部 = 这三个城市合并
     */
    private static final List<String> DEFAULT_CITIES = Arrays.asList("广州", "珠海", "桂林");

    public AmapClientImpl(AmapProperties props) {
        this.props = props;

        // 强制直连，不读系统/JVM 代理，避免出口 IP 被判定为海外
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setProxy(Proxy.NO_PROXY);
        this.rest = new RestTemplate(factory);
    }

    @Override
    public List<Poi> search(String keyword, String city, Integer type) {
        // 指定城市：只查这个城市
        if (StringUtils.hasText(city)) {
            return doSearch(keyword, city, type);
        }

        // 全部：依次查三个演示城市，合并去重
        List<Poi> all = new ArrayList<>();
        for (String c : DEFAULT_CITIES) {
            try {
                all.addAll(doSearch(keyword, c, type));
            } catch (Exception e) {
                // 单个城市失败不影响其他城市结果
                System.err.println("[AMAP] 查询 " + c + " 失败: " + e.getMessage());
            }
        }

        Set<String> seen = new HashSet<>();
        List<Poi> result = new ArrayList<>();
        for (Poi p : all) {
            String uniqueKey = p.getName() + "|" + p.getAddress();
            if (seen.add(uniqueKey)) {
                result.add(p);
            }
        }

        return result.size() > 30 ? new ArrayList<>(result.subList(0, 30)) : result;
    }

    /**
     * 单次高德检索：一个关键词 + 一个城市 + 一个类型
     */
    private List<Poi> doSearch(String keyword, String city, Integer type) {
        String amapType = mapType(type);

        if (StringUtils.hasText(keyword)) {
            keyword = keyword.trim().replaceAll("\\s+", "");
        } else {
            keyword = defaultKeyword(type);
        }

        UriComponentsBuilder b = UriComponentsBuilder
                .fromHttpUrl("https://restapi.amap.com/v3/place/text")
                .queryParam("key", props.getKey())
                .queryParam("offset", 20)
                .queryParam("page", 1)
                .queryParam("extensions", "all");

        if (StringUtils.hasText(keyword)) {
            b.queryParam("keywords", keyword);
        }
        if (StringUtils.hasText(city)) {
            b.queryParam("city", city);
        }
        if (amapType != null) {
            b.queryParam("types", amapType);
        }

        URI uri = b.build().encode().toUri();
        System.out.println("[AMAP] " + uri);

        String resp = rest.getForObject(uri, String.class);

        JsonNode root;
        try {
            root = mapper.readTree(resp);
        } catch (Exception e) {
            throw new BizException(502, "高德响应解析失败: " + e.getMessage());
        }

        if (!"1".equals(root.path("status").asText())) {
            String infocode = root.path("infocode").asText("");
            String info = root.path("info").asText();
            throw new BizException(502, "高德接口[" + infocode + "]: " + info);
        }

        List<Poi> result = new ArrayList<>();
        for (JsonNode n : root.path("pois")) {
            Poi p = new Poi();
            p.setName(n.path("name").asText(""));
            p.setAddress(n.path("address").asText(""));
            p.setType(type == null ? 0 : type);
            p.setTags("");
            p.setBestTime("");
            p.setTips("");

            String cityName = n.path("cityname").asText(StringUtils.hasText(city) ? city : "");
            if (cityName.endsWith("市")) {
                cityName = cityName.substring(0, cityName.length() - 1);
            }
            p.setCity(cityName);

            String typeStr = n.path("type").asText("");
            String bizType = n.path("biz_type").asText("");
            String intro = StringUtils.hasText(bizType) ? bizType : typeStr;
            p.setIntro(intro);

            // 特色标签：取前两个，避免一长串菜名
            String atag = n.path("atag").asText("");
            if (StringUtils.hasText(atag)) {
                String[] tagArr = atag.split("[,，|]");
                int limit = Math.min(2, tagArr.length);
                p.setTags(String.join(" · ", Arrays.copyOfRange(tagArr, 0, limit)));
            }

            // 行政区，例如 越秀区
            p.setTips(n.path("adname").asText(""));

            String loc = n.path("location").asText("");
            if (loc.contains(",")) {
                String[] arr = loc.split(",");
                try {
                    p.setLng(new BigDecimal(arr[0]));
                    p.setLat(new BigDecimal(arr[1]));
                } catch (NumberFormatException ignored) {
                }
            }

            JsonNode biz = n.path("biz_ext");

            String rating = biz.path("rating").asText("");
            if (StringUtils.hasText(rating)) {
                try {
                    p.setRating(new BigDecimal(rating));
                } catch (NumberFormatException ignored) {
                }
            }

            String cost = biz.path("cost").asText("");
            if (StringUtils.hasText(cost)) {
                try {
                    p.setPrice(new BigDecimal(cost));
                } catch (NumberFormatException ignored) {
                }
            }

            JsonNode photos = n.path("photos");
            if (photos != null && photos.size() > 0) {
                String cover = photos.get(0).path("url").asText("");
                if (StringUtils.hasText(cover)) {
                    p.setCover(cover);
                }
            }

            result.add(p);
        }
        return result;
    }

    private String defaultKeyword(Integer t) {
        if (t == null) {
            return null;
        }
        switch (t) {
            case 1:
                return "景点";
            case 2:
                return "美食";
            case 3:
                return "休闲娱乐";
            case 4:
                return "购物";
            default:
                return null;
        }
    }

    private String mapType(Integer t) {
        if (t == null) {
            return null;
        }
        switch (t) {
            case 1:
                return "110000";
            case 2:
                return "050000";
            case 3:
                return "080000";
            case 4:
                return "060000";
            default:
                return null;
        }
    }
}
