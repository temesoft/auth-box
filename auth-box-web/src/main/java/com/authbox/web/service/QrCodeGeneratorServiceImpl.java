package com.authbox.web.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.val;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
public class QrCodeGeneratorServiceImpl implements QrCodeGeneratorService {

    @Override
    public BufferedImage generateQrCode(final String barcodeText) throws WriterException {
        val barcodeWriter = new QRCodeWriter();
        val bitMatrix = barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 300, 300);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
