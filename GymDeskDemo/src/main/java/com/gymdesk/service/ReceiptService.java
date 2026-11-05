package com.gymdesk.service;

import com.gymdesk.domain.GymConfig;
import com.gymdesk.domain.Payment;
import com.gymdesk.domain.ReportSummary;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Servicio de infraestructura para la emisión de comprobantes internos y reportes en PDF.
 * Cumple con la normativa tributaria peruana aclarando que son recibos administrativos no válidos para SUNAT.
 */
public class ReceiptService {

    private static final Color PRIMARY_COLOR = new Color(20, 31, 48);
    private static final Color ACCENT_COLOR = new Color(40, 112, 220);
    private static final Color GRAY_TEXT = new Color(110, 115, 125);

    /**
     * Genera un archivo PDF con formato de recibo interno (tamaño A6 ticket).
     */
    public void exportReceiptPdf(Payment p, GymConfig config, File targetFile) throws IOException {
        Document document = new Document(PageSize.A6, 18, 18, 18, 18);
        try (FileOutputStream out = new FileOutputStream(targetFile)) {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY_COLOR);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 8, GRAY_TEXT);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_COLOR);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font warningFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.RED);

            // Encabezado comercial
            Paragraph gymTitle = new Paragraph(config.gymName().toUpperCase(), titleFont);
            gymTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(gymTitle);

            Paragraph gymDetails = new Paragraph(
                    "RUC/ID: " + config.taxId() + "\n" +
                    config.address() + "\nTel: " + config.phone() + " · " + config.email(),
                    subtitleFont
            );
            gymDetails.setAlignment(Element.ALIGN_CENTER);
            gymDetails.setSpacingAfter(8);
            document.add(gymDetails);

            // Título del comprobante
            Paragraph receiptHeader = new Paragraph("COMPROBANTE INTERNO DE CAJA", boldFont);
            receiptHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(receiptHeader);

            Paragraph numCtrl = new Paragraph("N.º Control: " + p.invoiceNumber(), boldFont);
            numCtrl.setAlignment(Element.ALIGN_CENTER);
            numCtrl.setSpacingAfter(10);
            document.add(numCtrl);

            // Datos del cliente y operación
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(8);

            addCell(infoTable, "Fecha de emisión:", boldFont, Element.ALIGN_LEFT);
            addCell(infoTable, p.date(), normalFont, Element.ALIGN_RIGHT);

            addCell(infoTable, "Cliente / Socio:", boldFont, Element.ALIGN_LEFT);
            addCell(infoTable, p.memberName(), normalFont, Element.ALIGN_RIGHT);

            addCell(infoTable, "Concepto:", boldFont, Element.ALIGN_LEFT);
            addCell(infoTable, p.concept(), normalFont, Element.ALIGN_RIGHT);

            addCell(infoTable, "Método de pago:", boldFont, Element.ALIGN_LEFT);
            addCell(infoTable, p.method(), normalFont, Element.ALIGN_RIGHT);

            document.add(infoTable);

            // Desglose de importes
            PdfPTable amountsTable = new PdfPTable(2);
            amountsTable.setWidthPercentage(100);
            amountsTable.setSpacingAfter(12);

            addCell(amountsTable, "Precio regular base:", normalFont, Element.ALIGN_LEFT);
            addCell(amountsTable, String.format(Locale.US, "%s %.2f", config.currency(), p.originalAmount()), normalFont, Element.ALIGN_RIGHT);

            if (p.discountApplied() > 0) {
                addCell(amountsTable, "Promoción aplicada:", normalFont, Element.ALIGN_LEFT);
                addCell(amountsTable, p.promotionName(), normalFont, Element.ALIGN_RIGHT);

                addCell(amountsTable, "Descuento otorgado:", normalFont, Element.ALIGN_LEFT);
                addCell(amountsTable, String.format(Locale.US, "- %s %.2f", config.currency(), p.discountApplied()), normalFont, Element.ALIGN_RIGHT);
            }

            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL PAGADO:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, ACCENT_COLOR)));
            totalLabel.setBorder(PdfPCell.TOP);
            amountsTable.addCell(totalLabel);

            PdfPCell totalVal = new PdfPCell(new Phrase(String.format(Locale.US, "%s %.2f", config.currency(), p.amount()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, ACCENT_COLOR)));
            totalVal.setBorder(PdfPCell.TOP);
            totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            amountsTable.addCell(totalVal);

            document.add(amountsTable);

            if (p.isCancelled()) {
                Paragraph cancelledNotice = new Paragraph("¡COBRO ANULADO!\nMotivo: " + p.cancellationReason(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.RED));
                cancelledNotice.setAlignment(Element.ALIGN_CENTER);
                cancelledNotice.setSpacingAfter(8);
                document.add(cancelledNotice);
            }

            // Pie de página y leyenda tributaria
            Paragraph footer = new Paragraph(config.receiptFooter(), subtitleFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingAfter(6);
            document.add(footer);

            Paragraph legalNotice = new Paragraph(
                    "DOCUMENTO DE CONTROL INTERNO ADMINISTRATIVO.\n" +
                    "NO CONSTITUYE BOLETA O FACTURA ELECTRÓNICA TRIBUTARIA SUNAT.",
                    warningFont
            );
            legalNotice.setAlignment(Element.ALIGN_CENTER);
            document.add(legalNotice);

            document.close();
        }
    }

    /**
     * Genera un reporte consolidado en PDF de operaciones de caja (tamaño A4).
     */
    public void exportReportPdf(ReportSummary report, GymConfig config, File targetFile) throws IOException {
        Document document = new Document(PageSize.A4, 25, 25, 25, 25);
        try (FileOutputStream out = new FileOutputStream(targetFile)) {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 9, GRAY_TEXT);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR);
            Font headerTableFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);

            // Encabezado
            Paragraph title = new Paragraph(config.gymName().toUpperCase(), titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            document.add(title);

            Paragraph reportTitle = new Paragraph("REPORTE CONSOLIDADO DE CAJA Y ACTIVIDAD", boldFont);
            document.add(reportTitle);

            Paragraph period = new Paragraph("Período evaluado: " + report.startDate() + " al " + report.endDate(), subtitleFont);
            period.setSpacingAfter(14);
            document.add(period);

            // Tarjetas / Tabla de resumen KPI
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(15);

            addKpiCell(kpiTable, "INGRESOS NETOS", String.format(Locale.US, "%s %.2f", config.currency(), report.grossIncome()), ACCENT_COLOR);
            addKpiCell(kpiTable, "TOTAL COBROS", String.valueOf(report.activePaymentsCount()), PRIMARY_COLOR);
            addKpiCell(kpiTable, "DESCUENTOS DADOS", String.format(Locale.US, "%s %.2f", config.currency(), report.totalDiscounts()), GRAY_TEXT);
            addKpiCell(kpiTable, "ANULACIONES", String.valueOf(report.cancelledPaymentsCount()), Color.RED);

            document.add(kpiTable);

            // Tabla de operaciones detalladas
            Paragraph detailTitle = new Paragraph("Detalle de operaciones registradas:", boldFont);
            detailTitle.setSpacingAfter(6);
            document.add(detailTitle);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 3.0f, 2.5f, 1.8f, 1.8f, 1.8f, 1.5f});

            String[] headers = {"N.º Interno", "Socio", "Concepto", "Base", "Desc.", "Total", "Estado"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerTableFont));
                cell.setBackgroundColor(PRIMARY_COLOR);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (Payment p : report.payments()) {
                addTableCell(table, p.invoiceNumber(), normalFont);
                addTableCell(table, p.memberName(), normalFont);
                addTableCell(table, p.concept(), normalFont);
                addTableCell(table, String.format(Locale.US, "%.2f", p.originalAmount()), normalFont);
                addTableCell(table, String.format(Locale.US, "%.2f", p.discountApplied()), normalFont);
                addTableCell(table, String.format(Locale.US, "%.2f", p.amount()), normalFont);
                addTableCell(table, p.status(), normalFont);
            }

            document.add(table);
            document.close();
        }
    }

    private void addCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(2);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addKpiCell(PdfPTable table, String title, String value, Color valueColor) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBackgroundColor(new Color(245, 247, 250));
        cell.setBorderColor(new Color(220, 225, 230));

        Paragraph p1 = new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, GRAY_TEXT));
        Paragraph p2 = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, valueColor));
        cell.addElement(p1);
        cell.addElement(p2);
        table.addCell(cell);
    }
}

