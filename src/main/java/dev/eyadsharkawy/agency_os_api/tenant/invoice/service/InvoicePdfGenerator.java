package dev.eyadsharkawy.agency_os_api.tenant.invoice.service;

import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.Invoice;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.TimeEntry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class InvoicePdfGenerator {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public static byte[] generate(
      Invoice invoice, String agencyName, String contactEmail, List<TimeEntry> billedEntries)
      throws IOException {
    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage();
      document.addPage(page);

      // Open the content stream for the first page
      PDPageContentStream contentStream = new PDPageContentStream(document, page);

      PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
      PDType1Font standardFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      PDType1Font standardBoldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

      // --- BRAND COLORS ---
      float brandR = 55 / 255f;
      float brandG = 120 / 255f;
      float brandB = 195 / 255f;

      // Draw top colored band on page 1
      contentStream.setNonStrokingColor(brandR, brandG, brandB);
      contentStream.addRect(0, 770, 612, 22);
      contentStream.fill();

      // --- FROM SECTION (SENDER) ---
      contentStream.beginText();
      contentStream.setFont(titleFont, 16);
      contentStream.setNonStrokingColor(0, 0, 0); // Black text
      contentStream.newLineAtOffset(50, 725);
      contentStream.showText(agencyName);
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardFont, 10);
      contentStream.setNonStrokingColor(100 / 255f, 100 / 255f, 100 / 255f); // Gray text
      contentStream.newLineAtOffset(50, 708);
      contentStream.showText("Contact: " + contactEmail);
      contentStream.endText();

      // --- BILLING INFO GRID ---
      float gridLabelY = 660f;
      float gridValueY = 645f;

      // Column 1: BILL TO
      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 9);
      contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
      contentStream.newLineAtOffset(50, gridLabelY);
      contentStream.showText("BILL TO");
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 10);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(50, gridValueY);
      contentStream.showText(invoice.getClient().getName());
      contentStream.newLineAtOffset(0, -13);
      contentStream.setFont(standardFont, 9);
      contentStream.setNonStrokingColor(100 / 255f, 100 / 255f, 100 / 255f);
      contentStream.showText(
          invoice.getClient().getEmail() != null ? invoice.getClient().getEmail() : "N/A");
      contentStream.endText();

      // Column 2: PROJECT
      String projectName = "General Billing";
      if (billedEntries != null && !billedEntries.isEmpty()) {
        long uniqueProjects =
            billedEntries.stream()
                .map(entry -> entry.getTask().getProject().getId())
                .distinct()
                .count();

        if (uniqueProjects == 1) {
          projectName = billedEntries.get(0).getTask().getProject().getName();
        } else if (uniqueProjects > 1) {
          projectName = "Multiple Projects";
        }
      }
      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 9);
      contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
      contentStream.newLineAtOffset(210, gridLabelY);
      contentStream.showText("PROJECT");
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardFont, 10);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(210, gridValueY);
      contentStream.showText(projectName);
      contentStream.endText();

      // Column 3: INVOICE DETAILS
      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 9);
      contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
      contentStream.newLineAtOffset(350, gridLabelY);
      contentStream.showText("INVOICE #");
      contentStream.newLineAtOffset(0, -15);
      contentStream.showText("DATE");
      contentStream.newLineAtOffset(0, -15);
      contentStream.showText("DUE DATE");
      contentStream.endText();

      Instant dueDate = invoice.getCreatedAt().plus(Duration.ofDays(30)); // Net 30

      // Render full UUID at size 8
      contentStream.beginText();
      contentStream.setFont(standardFont, 8);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(425, gridLabelY);
      contentStream.showText(invoice.getId().toString());
      contentStream.endText();

      // Render Dates at size 9
      contentStream.beginText();
      contentStream.setFont(standardFont, 9);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(425, gridLabelY - 15f);
      contentStream.showText(
          invoice.getCreatedAt().atZone(ZoneOffset.UTC).format(DATE_FORMATTER).substring(0, 10));
      contentStream.newLineAtOffset(0, -15);
      contentStream.showText(
          dueDate.atZone(ZoneOffset.UTC).format(DATE_FORMATTER).substring(0, 10));
      contentStream.endText();

      // --- INVOICE TOTAL HIGHLIGHT BLOCK ---
      contentStream.setLineWidth(1f);
      contentStream.setStrokingColor(220 / 255f, 220 / 255f, 220 / 255f);
      contentStream.moveTo(50, 580);
      contentStream.lineTo(562, 580);
      contentStream.stroke();

      contentStream.moveTo(50, 530);
      contentStream.lineTo(562, 530);
      contentStream.stroke();

      contentStream.beginText();
      contentStream.setFont(titleFont, 20);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(50, 548);
      contentStream.showText("Invoice Total");
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(titleFont, 22);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(450, 548);
      contentStream.showText("$" + invoice.getTotalAmount().setScale(2).toString());
      contentStream.endText();

      // --- TABLE HEADERS ---
      int tableHeaderY = 495;
      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 9);
      contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
      contentStream.newLineAtOffset(50, tableHeaderY);
      contentStream.showText("ACTIVITY");
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 9);
      contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
      contentStream.newLineAtOffset(390, tableHeaderY);
      contentStream.showText("RATE");
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 9);
      contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
      contentStream.newLineAtOffset(480, tableHeaderY);
      contentStream.showText("AMOUNT");
      contentStream.endText();

      contentStream.setLineWidth(0.8f);
      contentStream.setStrokingColor(230 / 255f, 230 / 255f, 230 / 255f);
      contentStream.moveTo(50, 485);
      contentStream.lineTo(562, 485);
      contentStream.stroke();

      // --- DRAW TABLE ITEMS (WITH MULTI-PAGE FLOW) ---
      float yPosition = 465f;
      for (TimeEntry entry : billedEntries) {
        // Page break check: if Y position gets too close to the bottom (below Y=80)
        if (yPosition < 80) {
          contentStream.close(); // Close current page stream

          page = new PDPage();
          document.addPage(page);
          contentStream = new PDPageContentStream(document, page);

          // Draw top colored band on page 2
          contentStream.setNonStrokingColor(brandR, brandG, brandB);
          contentStream.addRect(0, 770, 612, 22);
          contentStream.fill();

          // Re-draw Table Headers on the new page
          float newPageHeaderY = 730f;
          contentStream.beginText();
          contentStream.setFont(standardBoldFont, 9);
          contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
          contentStream.newLineAtOffset(50, newPageHeaderY);
          contentStream.showText("ACTIVITY");
          contentStream.endText();

          contentStream.beginText();
          contentStream.setFont(standardBoldFont, 9);
          contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
          contentStream.newLineAtOffset(390, newPageHeaderY);
          contentStream.showText("RATE");
          contentStream.endText();

          contentStream.beginText();
          contentStream.setFont(standardBoldFont, 9);
          contentStream.setNonStrokingColor(120 / 255f, 120 / 255f, 120 / 255f);
          contentStream.newLineAtOffset(480, newPageHeaderY);
          contentStream.showText("AMOUNT");
          contentStream.endText();

          // Draw separator line under headers on new page
          contentStream.setLineWidth(0.8f);
          contentStream.setStrokingColor(230 / 255f, 230 / 255f, 230 / 255f);
          contentStream.moveTo(50, newPageHeaderY - 10f);
          contentStream.lineTo(562, newPageHeaderY - 10f);
          contentStream.stroke();

          yPosition = newPageHeaderY - 30f; // Reset Y drawing coordinate
        }

        double hoursVal = entry.getDurationMinutes() / 60.0;
        BigDecimal billingRate = entry.getTask().getProject().getBillingRate();
        BigDecimal entryCost =
            BigDecimal.valueOf(hoursVal).multiply(billingRate).setScale(2, RoundingMode.HALF_UP);

        // DESCRIPTION (Project - Task title + hours logged)
        contentStream.beginText();
        contentStream.setFont(standardFont, 10);
        contentStream.setNonStrokingColor(0, 0, 0);
        contentStream.newLineAtOffset(50, yPosition);
        String projectNameVal = entry.getTask().getProject().getName();
        String taskTitleVal = entry.getTask().getTitle();
        String itemDesc =
            String.format("%s - %s (%.1f hrs)", projectNameVal, taskTitleVal, hoursVal);
        contentStream.showText(itemDesc);
        contentStream.endText();

        // UNIT PRICE (Rate)
        contentStream.beginText();
        contentStream.setFont(standardFont, 10);
        contentStream.newLineAtOffset(390, yPosition);
        contentStream.showText("$" + billingRate.setScale(2).toString());
        contentStream.endText();

        // AMOUNT (Cost)
        contentStream.beginText();
        contentStream.setFont(standardFont, 10);
        contentStream.newLineAtOffset(480, yPosition);
        contentStream.showText("$" + entryCost.setScale(2).toString());
        contentStream.endText();

        yPosition -= 22f;
      }

      // Table bottom separator line
      contentStream.setLineWidth(0.8f);
      contentStream.setStrokingColor(230 / 255f, 230 / 255f, 230 / 255f);
      contentStream.moveTo(50, yPosition + 12f);
      contentStream.lineTo(562, yPosition + 12f);
      contentStream.stroke();

      // --- 7. TOTAL DUE & TERMS PAGE BREAK PROTECTION ---
      // If there's not enough room left on the current page to fit the totals and terms (requires
      // ~130pt)
      if (yPosition < 130) {
        contentStream.close();

        page = new PDPage();
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);

        // Draw top colored band on final overflow page
        contentStream.setNonStrokingColor(brandR, brandG, brandB);
        contentStream.addRect(0, 770, 612, 22);
        contentStream.fill();

        yPosition = 740; // Reset Y position to top
      }

      // Draw Total Due
      float totalY = yPosition - 5f;
      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 11);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(350, totalY);
      contentStream.showText("Total Due");
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 11);
      contentStream.setNonStrokingColor(0, 0, 0);
      contentStream.newLineAtOffset(480, totalY);
      contentStream.showText("$" + invoice.getTotalAmount().setScale(2).toString());
      contentStream.endText();

      // --- 8. TERMS & CONDITIONS ---
      float termsY = totalY - 50f;
      // If terms get pushed too close to the bottom band, float them at Y = 85
      if (termsY < 85) {
        termsY = 85f;
      }
      contentStream.beginText();
      contentStream.setFont(standardBoldFont, 10);
      contentStream.setNonStrokingColor(brandR, brandG, brandB);
      contentStream.newLineAtOffset(50, termsY);
      contentStream.showText("TERMS & CONDITIONS");
      contentStream.endText();

      contentStream.beginText();
      contentStream.setFont(standardFont, 9);
      contentStream.setNonStrokingColor(100 / 255f, 100 / 255f, 100 / 255f);
      contentStream.newLineAtOffset(50, termsY - 16f);
      contentStream.showText("Payment is due within 30 days of issue.");
      contentStream.endText();

      // --- 9. BOTTOM BRAND BAND ---
      contentStream.setNonStrokingColor(brandR, brandG, brandB);
      contentStream.addRect(0, 0, 612, 22); // Bottom colored band
      contentStream.fill();

      // Close the final content stream
      contentStream.close();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      document.save(baos);
      return baos.toByteArray();
    }
  }
}
