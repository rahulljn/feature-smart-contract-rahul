-- V7: Email Templates table

CREATE TABLE email_templates (
    template_id     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL UNIQUE,
    subject         TEXT         NOT NULL,
    html_body       TEXT         NOT NULL,
    segment         VARCHAR(100) DEFAULT 'ALL',
    is_active       BOOLEAN      NOT NULL DEFAULT FALSE,
    last_edited_by  UUID         REFERENCES users(user_id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_edited_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Seed with the live template from EmailNotification.java
INSERT INTO email_templates (name, subject, html_body, segment, is_active)
VALUES (
    'Geojit Contract Note - Default',
    'Contract Note - Geojit Investments Ltd',
    '<!DOCTYPE html>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
</head>
<body style="font-family: Arial, sans-serif; font-size: 10pt; color: #333;">
Dear {{name}},
<br/><br/>
Warm Greetings from Geojit Investments Ltd !
<br/><br/>
We hope your experience with Geojit Investments Ltd has been pleasant.
We are herewith sending you your digitally signed contract note (PDF Document).
<br/><br/>
To open your attachment you require Adobe Acrobat Reader 6.0 or above or Foxit Reader.
<br/><br/>
<strong>Instructions for Opening the attachment:-</strong>
<br/><br/>
1. Click on the attachment provided with this mail. If you are prompted for a password, please follow the below steps.
<br/><br/>
<strong>INDIVIDUAL</strong> clients may enter the first four characters of your PAN (in CAPITAL letters) followed by first four characters of your DATE OF BIRTH (DOB) [in DDMM format] as the password for the PDF attachment.
<br/>
For Eg. PAN: BDPBV2015Z and DOB: 31.01.1979 then password will be <strong>BDPB3101</strong>
<br/><br/>
For <strong>NON-INDIVIDUAL</strong> clients you may enter your PAN (in CAPITAL letters) as the password for the PDF attachment.
<br/>
For Eg. PAN: BDPBV2015Z then password will be <strong>BDPBV2015Z</strong>
<br/><br/>
<strong>Security Notice:</strong> We will never ask for your login ID, password, or OTP. Please refrain from sharing this information with anyone.
<br/><br/>
For all queries, kindly contact <a href="mailto:customercare@geojit.com">customercare@geojit.com</a>.
<br/>
Toll Free No: 1800-571-5501, 1800-103-5501. Paid Line: +91-484-3911777
</body>
</html>',
    'ALL',
    TRUE
);

CREATE INDEX idx_et_segment    ON email_templates(segment);
CREATE INDEX idx_et_is_active  ON email_templates(is_active);
