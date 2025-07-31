package com.aqryuz.auth.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.springframework.stereotype.Service;
import com.aqryuz.auth.config.AppProperties;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TotpService {

    private final AppProperties appProperties;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String generateQrCodeData(String secret, String username) {
        QrData data = new QrData.Builder().label(username).secret(secret)
                .issuer(appProperties.totp().issuer()).digits(appProperties.totp().digits())
                .period(appProperties.totp().period()).build();

        return data.getUri();
    }

    public String generateQrCodeImage(String qrCodeData) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeData, BarcodeFormat.QR_CODE, 200, 200);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] qrCodeBytes = outputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodeBytes);

        } catch (WriterException | IOException e) {
            log.error("Error generating QR code image: {}", e.getMessage());
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }

    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    public String getCurrentCode(String secret) {
        try {
            return codeGenerator.generate(secret, timeProvider.getTime());
        } catch (Exception e) {
            log.error("Error generating TOTP code: {}", e.getMessage());
            throw new IllegalStateException("Failed to generate TOTP code", e);
        }
    }
}
