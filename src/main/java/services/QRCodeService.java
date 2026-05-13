package services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;

public class QRCodeService {


    /**
     * Génère un QR Code à partir d'un texte
     * @param text Le texte à encoder (URL, ID, etc.)
     * @param width Largeur de l'image
     * @param height Hauteur de l'image
     * @return ImageView contenant le QR Code
     */
    public static ImageView generateQRCode(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
            return new ImageView(image);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Génère un QR Code pour une réservation
     * @param reservationId ID de la réservation
     * @return ImageView
     */
    public static ImageView generateReservationQRCode(int reservationId) {
        String data = "VITA_CHECKIN:" + reservationId;
        return generateQRCode(data, 200, 200);
    }

    /**
     * Génère un QR Code pour un événement
     * @param eventId ID de l'événement
     * @return ImageView
     */
    public static ImageView generateEventQRCode(int eventId) {
        String data = "VITA_EVENT:" + eventId;
        return generateQRCode(data, 200, 200);
    }

    /**
     * Vérifie si un QR code est valide et retourne les données
     */
    public static QRData parseQRCode(String data) {
        if (data == null || data.isBlank()) return null;
        String normalizedData = data.trim();

        try {
            if (normalizedData.startsWith("VITA_CHECKIN:")) {
                int id = Integer.parseInt(normalizedData.substring("VITA_CHECKIN:".length()).trim());
                return new QRData("CHECKIN", id);
            } else if (normalizedData.startsWith("VITA_EVENT:")) {
                int id = Integer.parseInt(normalizedData.substring("VITA_EVENT:".length()).trim());
                return new QRData("EVENT", id);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    public static class QRData {
        public String type;
        public int id;

        public QRData(String type, int id) {
            this.type = type;
            this.id = id;
        }
    }
}
