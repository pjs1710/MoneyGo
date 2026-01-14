package com.study.moneygo.pdf.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.study.moneygo.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    /**
     * 단일 거래 영수증 PDF 생성
     */
    public byte[] generateReceiptPdf(Transaction transaction, String accountNumber, String counterpartyName) {
        String html = buildReceiptHtml(transaction, accountNumber, counterpartyName);
        return convertHtmlToPdf(html);
    }

    /**
     * 거래 내역서 PDF 생성 (월별/연도별)
     */
    public byte[] generateStatementPdf(
            List<Transaction> transactions,
            String accountNumber,
            String accountHolder,
            Integer year,
            Integer month) {
        String html = buildStatementHtml(transactions, accountNumber, accountHolder, year, month);
        return convertHtmlToPdf(html);
    }

    private byte[] convertHtmlToPdf(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            HtmlConverter.convertToPdf(html, writer);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF 생성 실패", e);
            throw new RuntimeException("PDF 생성에 실패했습니다.", e);
        }
    }

    private String buildReceiptHtml(Transaction transaction, String accountNumber, String counterpartyName) {
        String transactionType = getTransactionTypeKorean(transaction.getType().name());
        String formattedAmount = formatCurrency(transaction.getAmount());
        String formattedDate = transaction.getCreatedAt().format(DATE_TIME_FORMATTER);

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Malgun Gothic', sans-serif; padding: 40px; }
                        .header { text-align: center; margin-bottom: 40px; }
                        .header h1 { font-size: 28px; color: #333; margin-bottom: 10px; }
                        .header p { color: #666; font-size: 14px; }
                        .content { border: 2px solid #333; padding: 30px; margin-bottom: 30px; }
                        .row { display: flex; margin-bottom: 15px; padding: 10px 0; border-bottom: 1px solid #eee; }
                        .label { font-weight: bold; width: 150px; color: #555; }
                        .value { flex: 1; color: #333; }
                        .amount { font-size: 24px; font-weight: bold; color: #2196F3; text-align: right; }
                        .footer { text-align: center; color: #666; font-size: 12px; margin-top: 40px; }
                        .stamp { text-align: right; margin-top: 30px; font-size: 14px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>거래 영수증</h1>
                        <p>Transaction Receipt</p>
                    </div>
                    
                    <div class="content">
                        <div class="row">
                            <div class="label">거래 번호</div>
                            <div class="value">%s</div>
                        </div>
                        <div class="row">
                            <div class="label">거래 유형</div>
                            <div class="value">%s</div>
                        </div>
                        <div class="row">
                            <div class="label">거래 금액</div>
                            <div class="value amount">%s</div>
                        </div>
                        <div class="row">
                            <div class="label">보낸 계좌</div>
                            <div class="value">%s</div>
                        </div>
                        <div class="row">
                            <div class="label">받는 계좌</div>
                            <div class="value">%s</div>
                        </div>
                        <div class="row">
                            <div class="label">상대방</div>
                            <div class="value">%s</div>
                        </div>
                        <div class="row">
                            <div class="label">거래 내용</div>
                            <div class="value">%s</div>
                        </div>
                        <div class="row">
                            <div class="label">거래 일시</div>
                            <div class="value">%s</div>
                        </div>
                        <div class="row">
                            <div class="label">거래 상태</div>
                            <div class="value">완료</div>
                        </div>
                    </div>
                    
                    <div class="stamp">
                        발행일: %s<br>
                        발행처: MoneyGo
                    </div>
                    
                    <div class="footer">
                        본 영수증은 전자 거래 영수증으로 법적 효력을 갖습니다.<br>
                        MoneyGo | 고객센터: 1588-0000 | www.moneygo.com
                    </div>
                </body>
                </html>
                """.formatted(
                transaction.getId(),
                transactionType,
                formattedAmount,
                transaction.getFromAccount() != null ? transaction.getFromAccount().getAccountNumber() : "-",
                transaction.getToAccount() != null ? transaction.getToAccount().getAccountNumber() : "-",
                counterpartyName != null ? counterpartyName : "내 계좌",
                transaction.getDescription(),
                formattedDate,
                DATE_FORMATTER.format(transaction.getCreatedAt())
        );
    }

    private String buildStatementHtml(
            List<Transaction> transactions,
            String accountNumber,
            String accountHolder,
            Integer year,
            Integer month) {

        String period = month != null
                ? String.format("%d년 %d월", year, month)
                : String.format("%d년", year);

        BigDecimal totalDeposit = BigDecimal.ZERO;
        BigDecimal totalWithdraw = BigDecimal.ZERO;

        StringBuilder transactionRows = new StringBuilder();
        for (Transaction tx : transactions) {
            boolean isDeposit = tx.getToAccount() != null &&
                    tx.getToAccount().getAccountNumber().equals(accountNumber);

            if (isDeposit) {
                totalDeposit = totalDeposit.add(tx.getAmount());
            } else {
                totalWithdraw = totalWithdraw.add(tx.getAmount());
            }

            transactionRows.append(String.format("""
                    <tr>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td style="text-align: right; color: %s;">%s</td>
                        <td>%s</td>
                    </tr>
                    """,
                    DATE_FORMATTER.format(tx.getCreatedAt()),
                    getTransactionTypeKorean(tx.getType().name()),
                    tx.getDescription(),
                    isDeposit ? "#2196F3" : "#F44336",
                    isDeposit ? "+" + formatCurrency(tx.getAmount()) : "-" + formatCurrency(tx.getAmount()),
                    tx.getStatus().name()
            ));
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Malgun Gothic', sans-serif; padding: 40px; }
                        .header { text-align: center; margin-bottom: 40px; }
                        .header h1 { font-size: 28px; color: #333; margin-bottom: 10px; }
                        .info { margin-bottom: 30px; }
                        .info-row { display: flex; margin-bottom: 10px; }
                        .info-label { font-weight: bold; width: 120px; }
                        table { width: 100%%; border-collapse: collapse; margin-bottom: 30px; }
                        th { background-color: #f5f5f5; padding: 12px; text-align: left; border: 1px solid #ddd; }
                        td { padding: 10px; border: 1px solid #ddd; }
                        .summary { border: 2px solid #333; padding: 20px; margin-top: 30px; }
                        .summary-row { display: flex; justify-content: space-between; margin-bottom: 10px; }
                        .summary-label { font-weight: bold; }
                        .summary-value { font-size: 18px; font-weight: bold; }
                        .footer { text-align: center; color: #666; font-size: 12px; margin-top: 40px; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>거래 내역서</h1>
                        <p>Transaction Statement</p>
                    </div>
                    
                    <div class="info">
                        <div class="info-row">
                            <div class="info-label">발급 대상:</div>
                            <div>%s (%s)</div>
                        </div>
                        <div class="info-row">
                            <div class="info-label">조회 기간:</div>
                            <div>%s</div>
                        </div>
                        <div class="info-row">
                            <div class="info-label">발급 일자:</div>
                            <div>%s</div>
                        </div>
                    </div>
                    
                    <table>
                        <thead>
                            <tr>
                                <th>거래일시</th>
                                <th>거래유형</th>
                                <th>거래내용</th>
                                <th style="text-align: right;">금액</th>
                                <th>상태</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                    
                    <div class="summary">
                        <div class="summary-row">
                            <div class="summary-label">총 입금액:</div>
                            <div class="summary-value" style="color: #2196F3;">%s</div>
                        </div>
                        <div class="summary-row">
                            <div class="summary-label">총 출금액:</div>
                            <div class="summary-value" style="color: #F44336;">%s</div>
                        </div>
                        <div class="summary-row">
                            <div class="summary-label">거래 건수:</div>
                            <div class="summary-value">%d건</div>
                        </div>
                    </div>
                    
                    <div class="footer">
                        본 내역서는 세금 신고용으로 사용 가능합니다.<br>
                        MoneyGo | 고객센터: 1588-0000 | www.moneygo.com
                    </div>
                </body>
                </html>
                """.formatted(
                accountHolder,
                accountNumber,
                period,
                DATE_FORMATTER.format(java.time.LocalDateTime.now()),
                transactionRows.toString(),
                formatCurrency(totalDeposit),
                formatCurrency(totalWithdraw),
                transactions.size()
        );
    }

    private String getTransactionTypeKorean(String type) {
        return switch (type) {
            case "TRANSFER" -> "송금";
            case "DEPOSIT" -> "입금";
            case "WITHDRAW" -> "출금";
            case "QR_PAYMENT" -> "QR결제";
            default -> type;
        };
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("%,d원", amount.longValue());
    }
}
