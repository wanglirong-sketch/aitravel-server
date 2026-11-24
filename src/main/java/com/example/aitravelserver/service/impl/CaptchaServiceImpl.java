package com.example.aitravelserver.service.impl;

import com.example.aitravelserver.dto.request.CaptchaVO;
import com.example.aitravelserver.service.CaptchaService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaServiceImpl implements CaptchaService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final int CODE_LENGTH = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;
    private static final long EXPIRE_MINUTES = 5;

    private static final char[] CHARS =
            "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();

    public CaptchaServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public CaptchaVO createCaptcha() {
        String code = randomCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");

        stringRedisTemplate.opsForValue().set(
                CAPTCHA_PREFIX + captchaId,
                code.toLowerCase(),
                EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        String imageBase64 = createImageBase64(code);
        return new CaptchaVO(captchaId, imageBase64);
    }

    @Override
    public boolean verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.trim().isEmpty()) {
            return false;
        }
        if (captchaCode == null || captchaCode.trim().isEmpty()) {
            return false;
        }

        String key = CAPTCHA_PREFIX + captchaId;
        String savedCode = stringRedisTemplate.opsForValue().get(key);

        if (savedCode == null) {
            return false;
        }

        boolean matched = savedCode.equals(captchaCode.trim().toLowerCase());
        stringRedisTemplate.delete(key);
        return matched;
    }

    private String randomCode() {
        Random random = new Random();
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return builder.toString();
    }

    private String createImageBase64(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Random random = new Random();

        g.setColor(new Color(248, 250, 252));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(191, 219, 254));
            g.drawLine(
                    random.nextInt(WIDTH),
                    random.nextInt(HEIGHT),
                    random.nextInt(WIDTH),
                    random.nextInt(HEIGHT)
            );
        }

        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(148, 163, 184));
            g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
        }

        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(30, 64, 175));
            int x = 18 + i * 24;
            int y = 30 + random.nextInt(6) - 3;
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }

        g.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }
}
