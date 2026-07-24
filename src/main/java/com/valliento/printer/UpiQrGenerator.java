package com.valliento.printer;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates a real, scannable UPI payment QR code using the standard UPI
 * deep-link format that Google Pay / PhonePe / Paytm / BHIM all recognize:
 *
 *   upi://pay?pa=<payee_upi_id>&pn=<payee_name>&am=<amount>&cu=INR&tn=<note>
 *
 * The merchant details (pa/pn) come from SettingsDAO, so swapping the demo
 * UPI ID for a real one later requires no code changes.
 */
public class UpiQrGenerator {

    public static Image generateUpiQr(String upiId, String payeeName, double amount, String note, int sizePx) {
        String upiUri = buildUpiUri(upiId, payeeName, amount, note);
        return generateQrImage(upiUri, sizePx);
    }

    private static String buildUpiUri(String upiId, String payeeName, double amount, String note) {
        String encodedName = URLEncoder.encode(payeeName, StandardCharsets.UTF_8);
        String encodedNote = URLEncoder.encode(note == null ? "" : note, StandardCharsets.UTF_8);
        return String.format(
            "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=%s",
            upiId, encodedName, amount, encodedNote
        );
    }

    private static Image generateQrImage(String data, int sizePx) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

            WritableImage image = new WritableImage(sizePx, sizePx);
            PixelWriter pixelWriter = image.getPixelWriter();
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    pixelWriter.setColor(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return image;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}