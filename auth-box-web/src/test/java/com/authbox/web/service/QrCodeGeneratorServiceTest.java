package com.authbox.web.service;

import com.google.zxing.WriterException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class QrCodeGeneratorServiceTest {

    @Test
    public void testQrCodeGenerator() throws WriterException, IOException {
        final QrCodeGeneratorService service = new QrCodeGeneratorServiceImpl();
        final BufferedImage bufferedImage = service.generateQrCode("test");
        assertThat(bufferedImage)
                .isNotNull();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpg", baos);
        assertThat(baos.toByteArray())
                .isNotNull()
                .hasSizeGreaterThan(100)
                .contains("JFIF".getBytes(UTF_8));
    }
}