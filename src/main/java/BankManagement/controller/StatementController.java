
package BankManagement.controller;

import BankManagement.entity.Transaction;
import BankManagement.service.TransactionService;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/statements")
@CrossOrigin
public class StatementController {

    private final TransactionService transactionService;

    public StatementController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/pdf/{accountNumber}")
    public void downloadStatement(
            @PathVariable String accountNumber,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        List<Transaction> transactions =
                transactionService.getTransactionHistory(
                        accountNumber, authentication);

        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=BankEase-Statement.pdf"
        );

        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        try {
            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 18
            );

            Paragraph title = new Paragraph(
                    "BankEase - Mini Statement", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(
                    "Account Number: " + accountNumber));

            document.add(new Paragraph(
                    "Generated on: " +
                    java.time.LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern(
                                    "dd-MM-yyyy HH:mm")
                    )));

            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 3, 2, 2});

            addHeader(table, "Date");
            addHeader(table, "Type");
            addHeader(table, "Amount (INR)");
            addHeader(table, "Status");

            for (Transaction transaction : transactions) {
                table.addCell(
                        transaction.getTransactionDate() == null
                                ? "—"
                                : transaction.getTransactionDate().format(
                                        DateTimeFormatter.ofPattern(
                                                "dd-MM-yyyy HH:mm")
                                ));

                table.addCell(transaction.getTransactionType());
                table.addCell(
                        transaction.getAmount().toPlainString());
                table.addCell(transaction.getStatus());
            }

            document.add(table);

            BigDecimal totalDeposits = BigDecimal.ZERO;
            BigDecimal totalWithdrawals = BigDecimal.ZERO;

            for (Transaction transaction : transactions) {
                if ("DEPOSIT".equalsIgnoreCase(
                        transaction.getTransactionType())) {

                    totalDeposits = totalDeposits.add(
                            transaction.getAmount());

                } else if ("WITHDRAWAL".equalsIgnoreCase(
                        transaction.getTransactionType())
                        || "WITHDRAW".equalsIgnoreCase(
                        transaction.getTransactionType())) {

                    totalWithdrawals = totalWithdrawals.add(
                            transaction.getAmount());
                }
            }

            document.add(new Paragraph(" "));

            document.add(new Paragraph(
                    "Total Deposits: INR " +
                    totalDeposits.toPlainString()));

            document.add(new Paragraph(
                    "Total Withdrawals: INR " +
                    totalWithdrawals.toPlainString()));

        } finally {
            document.close();
        }
    }

    private void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        table.addCell(cell);
    }
}