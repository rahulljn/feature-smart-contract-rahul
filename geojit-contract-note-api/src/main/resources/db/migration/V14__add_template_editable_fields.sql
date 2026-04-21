-- V14: Add editable field columns to email_templates
-- These store the individual editable zones separately so the backend
-- can rebuild html_body via buildHtml() when fields change.

ALTER TABLE email_templates
    ADD COLUMN IF NOT EXISTS greeting_text TEXT,
    ADD COLUMN IF NOT EXISTS body_intro    TEXT,
    ADD COLUMN IF NOT EXISTS logo_url      VARCHAR(500),
    ADD COLUMN IF NOT EXISTS body_color    VARCHAR(20),
    ADD COLUMN IF NOT EXISTS footer_color  VARCHAR(20);

-- Populate existing default template with extracted field values and
-- rebuild html_body to use [NAME] placeholder (matching Lambda convention).
UPDATE email_templates
SET
    greeting_text = 'Warm Greetings from Geojit Investments Ltd !',
    body_intro    = 'We hope your experience with Geojit Investments Ltd has been pleasant. We are herewith sending you your digitally signed contract note (PDF Document).',
    body_color    = '#333333',
    footer_color  = '#666666',
    logo_url      = NULL,
    html_body     = '<!DOCTYPE html>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
</head>
<body style="font-family: Arial, sans-serif; font-size: 10pt; color: #333333;">
Dear [NAME],
<br/><br/>
Warm Greetings from Geojit Investments Ltd !
<br/><br/>
We hope your experience with Geojit Investments Ltd has been pleasant. We are herewith sending you your digitally signed contract note (PDF Document).
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
2. To view details regarding the digital signature, please click on the icon of a pen, on the left hand side frame of Adobe acrobat.
<br/><br/>
<strong>Security Notice:</strong> We will never ask for your login ID, password, or OTP. Please refrain from sharing this information with anyone. Your security is our top priority.
<br/><br/>
To download Adobe Reader, please visit <a href="http://get.adobe.com/reader/otherversions">http://get.adobe.com/reader/otherversions</a>
<br/>
To download Foxit Reader, please visit <a href="http://www.foxitsoftware.com/downloads/">http://www.foxitsoftware.com/downloads/</a>
<br/><br/>
For all queries, kindly contact <a href="mailto:customercare@geojit.com">customercare@geojit.com</a>.
<br/>
Toll Free No: 1800-571-5501, 1800-103-5501. Paid Line: +91-484-3911777
<br/><br/>
<pre style="font-family: monospace; font-size: 9pt; color: #666666;">---------------------------------------------------------------------------
The information contained in this electronic message and its attachments (the "message")
is intended solely for the addressees and is confidential and privileged.
If you are not the intended recipient, please notify the sender by reply e-mail
and then destroy the message. Any dissemination, distribution, forwarding, copying,
printing or disclosure, either whole or partial, is prohibited and may be unlawful.
Equity/Mutual Fund investments are subject to market risks.
Past performance does not guarantee future returns.
We do not offer any product which gives guaranteed returns.
WARNING: Computer viruses can be transmitted via email. The recipient should check
this email and any attachments for the presence of viruses. The company accepts no
liability for any damage caused by any virus transmitted by this email.
-----------------------------------------------------------------------</pre>
</body>
</html>'
WHERE name = 'Geojit Contract Note - Default';
