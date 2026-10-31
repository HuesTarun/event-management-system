package com.eventmanagement.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * QRCodeUtil - Helper class to generate QR Codes using Google ZXing.
 *
 * HOW ZXING WORKS:
 * 1. QRCodeWriter encodes text (like the ticket token) into a 2D BitMatrix (grid of dark/light modules).
 * 2. ErrorCorrectionLevel.M provides up to 15% error correction against damage or low scan quality.
 * 3. MatrixToImageWriter writes the BitMatrix as a standard image format (PNG) into an OutputStream.
 */
public class QRCodeUtil {

    /**
     * Generates a QR code PNG image and streams it directly to an OutputStream.
     *
     * @param text         Data to encode in the QR code (ticket token)
     * @param width        Width in pixels (e.g. 240)
     * @param height       Height in pixels (e.g. 240)
     * @param outputStream Target output stream (e.g. HttpServletResponse OutputStream)
     * @throws Exception If generation fails
     */
    public static void generateQRCodeStream(String text, int width, int height, OutputStream outputStream)
            throws Exception {

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
    }

    /**
     * Generates a QR code and returns it as a Base64 data URL string (data:image/png;base64,...).
     *
     * @param text   Data to encode
     * @param width  Width in pixels
     * @param height Height in pixels
     * @return Data URL string suitable for <img src="...">
     * @throws Exception If generation fails
     */
    public static String generateQRCodeBase64(String text, int width, int height) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        generateQRCodeStream(text, width, height, baos);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}

