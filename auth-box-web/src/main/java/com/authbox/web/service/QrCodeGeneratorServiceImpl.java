package com.authbox.web.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

import static com.google.zxing.BarcodeFormat.QR_CODE;

@Service
public class QrCodeGeneratorServiceImpl implements QrCodeGeneratorService {

    @Override
    public BufferedImage generateQrCode(final String barcodeText) throws WriterException {
        final QRCodeWriter barcodeWriter = new QRCodeWriter();
        final BitMatrix bitMatrix = barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 300, 300);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
