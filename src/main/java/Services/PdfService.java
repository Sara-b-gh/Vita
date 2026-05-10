package Services;

import Entites.CompteRendu;
import Entites.User;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class PdfService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void genererPdfCompteRendu(CompteRendu cr, User docteur, String nomPatient, String outputPath) throws IOException {

        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        // Titre
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
        contentStream.beginText();
        contentStream.newLineAtOffset(50, 750);
        contentStream.showText("COMPTE RENDU MÉDICAL");
        contentStream.endText();

        // Ligne de séparation
        contentStream.setLineWidth(1);
        contentStream.moveTo(50, 735);
        contentStream.lineTo(545, 735);
        contentStream.stroke();

        // Informations générales
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(50, 700);
        contentStream.showText("N° Compte Rendu : " + cr.getId_cr());
        contentStream.newLineAtOffset(0, -25);
        contentStream.showText("Rendez-vous N° : " + cr.getId_rdv());
        contentStream.newLineAtOffset(0, -25);
        contentStream.showText("Date de rédaction : " +
                (cr.getDate_creation() != null ? cr.getDate_creation().format(formatter) : "Non définie"));
        contentStream.endText();

        // Patient & Docteur
        contentStream.beginText();
        contentStream.newLineAtOffset(50, 620);
        contentStream.showText("Patient : " + (nomPatient != null ? nomPatient : "Non renseigné"));
        contentStream.newLineAtOffset(0, -25);
        contentStream.showText("Rédigé par : Dr. " + (docteur != null ? docteur.getPrenom() + " " + docteur.getNom() : "Inconnu"));
        contentStream.endText();

        // Diagnostic
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
        contentStream.beginText();
        contentStream.newLineAtOffset(50, 550);
        contentStream.showText("DIAGNOSTIC");
        contentStream.endText();

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(50, 525);
        contentStream.showText(cr.getDiagnostic() != null ? cr.getDiagnostic() : "Aucun diagnostic renseigné");
        contentStream.endText();

        // Contenu
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
        contentStream.beginText();
        contentStream.newLineAtOffset(50, 480);
        contentStream.showText("CONTENU");
        contentStream.endText();

        drawMultilineText(contentStream, cr.getContenu() != null ? cr.getContenu() : "", 50, 455, 480);

        // Traitement
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
        contentStream.beginText();
        contentStream.newLineAtOffset(50, 380);
        contentStream.showText("TRAITEMENT PRESCRIT");
        contentStream.endText();

        drawMultilineText(contentStream, cr.getTraitement() != null ? cr.getTraitement() : "", 50, 355, 480);

        // Prochain RDV
        if (cr.getProchain_rdv() != null) {
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(50, 250);
            contentStream.showText("Prochain rendez-vous : " + cr.getProchain_rdv());
            contentStream.endText();
        }

        contentStream.close();
        document.save(outputPath);
        document.close();

        System.out.println("✅ PDF généré avec succès : " + outputPath);
    }

    // Méthode utilitaire pour écrire du texte multiligne
    private static void drawMultilineText(PDPageContentStream contentStream, String text, float x, float y, float width) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);

        String[] lines = text.split("\n");
        for (String line : lines) {
            contentStream.showText(line);
            contentStream.newLineAtOffset(0, -18);
        }
        contentStream.endText();
    }
}