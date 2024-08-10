package com.authbox.web.service;

import com.google.zxing.WriterException;

import java.awt.image.BufferedImage;

public interface QrCodeGeneratorService {

    BufferedImage generateQrCode(String barcodeText) throws WriterException;

}
