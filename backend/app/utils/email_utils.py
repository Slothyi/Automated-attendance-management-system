import os
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart


# ── Public logo URL (hosted on GitHub, no attachment needed) ──
LOGO_URL = "https://raw.githubusercontent.com/Slothyi/Automated-attendance-management-system/main/backend/app/assets/logo.png"


def send_verification_email_smtp(to_email: str, verification_url: str) -> bool:
    smtp_server = os.getenv("SMTP_SERVER", "smtp.gmail.com")
    smtp_port = int(os.getenv("SMTP_PORT", "587"))
    smtp_user = os.getenv("SMTP_USERNAME")
    smtp_pass = os.getenv("SMTP_PASSWORD")
    smtp_from = os.getenv("SMTP_FROM", smtp_user)

    if not smtp_user or not smtp_pass:
        print("[SMTP] SMTP credentials not set. Mocking email send.")
        print(f"[SMTP] VERIFICATION EMAIL to {to_email}: {verification_url}")
        return False

    try:
        msg = MIMEMultipart("alternative")
        msg["Subject"] = "Verify Your Email - AttendancePRO"
        msg["From"] = f"AttendancePRO <{smtp_from}>"
        msg["To"] = to_email

        html = f"""\
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1.0">
</head>
<body style="margin:0;padding:0;background:#0f172a;font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#0f172a;padding:32px 0;">
    <tr><td align="center">
      <table width="560" cellpadding="0" cellspacing="0"
             style="max-width:560px;width:100%;background:#1e293b;
                    border-radius:20px;border:1px solid #06b6d4;
                    box-shadow:0 8px 32px rgba(6,182,212,0.2);overflow:hidden;">

        <!-- ── HEADER ── -->
        <tr>
          <td align="center"
              style="background:linear-gradient(160deg,#0f172a 0%,#1a3a5c 100%);
                     padding:36px 40px 28px;">
            <img src="{LOGO_URL}"
                 alt="AttendancePRO"
                 width="90" height="90"
                 style="display:block;border-radius:18px;
                        box-shadow:0 0 24px rgba(6,182,212,0.45);
                        border:2.5px solid #06b6d4;" />
            <h1 style="margin:16px 0 4px;font-size:24px;font-weight:800;
                        color:#ffffff;letter-spacing:0.5px;">AttendancePRO</h1>
            <p style="margin:0;font-size:11px;color:#06b6d4;
                      letter-spacing:3px;text-transform:uppercase;">
              Automated Attendance System
            </p>
          </td>
        </tr>

        <!-- ── GRADIENT DIVIDER ── -->
        <tr>
          <td style="height:3px;
                     background:linear-gradient(90deg,#06b6d4,#3b82f6,#06b6d4);">
          </td>
        </tr>

        <!-- ── BODY ── -->
        <tr>
          <td align="center" style="padding:40px 40px 32px;">
            <h2 style="margin:0 0 14px;font-size:21px;font-weight:700;color:#f8fafc;">
              Verify Your Email Address
            </h2>
            <p style="margin:0 0 30px;font-size:15px;line-height:1.75;color:#94a3b8;">
              Thank you for registering with
              <strong style="color:#06b6d4;">AttendancePRO</strong>.<br>
              Click the button below to verify your email and complete your registration.
            </p>

            <!-- BUTTON -->
            <a href="{verification_url}"
               style="display:inline-block;padding:15px 44px;
                      font-size:16px;font-weight:700;color:#ffffff;
                      background:linear-gradient(135deg,#06b6d4,#0284c7);
                      border-radius:50px;text-decoration:none;
                      box-shadow:0 6px 20px rgba(6,182,212,0.45);
                      letter-spacing:0.4px;">
              Verify Email Address
            </a>

            <p style="margin:28px 0 0;font-size:12px;color:#475569;line-height:1.7;">
              This link is single-use and will expire once clicked.<br>
              If you did not create an account, you can safely ignore this email.
            </p>
          </td>
        </tr>

        <!-- ── FOOTER ── -->
        <tr>
          <td align="center"
              style="background:#0f172a;padding:18px 40px;
                     border-top:1px solid #1e293b;">
            <p style="margin:0;font-size:11px;color:#334155;">
              &copy; 2026 AttendancePRO &nbsp;&bull;&nbsp;
              Automated Attendance Management System
            </p>
          </td>
        </tr>

      </table>
    </td></tr>
  </table>
</body>
</html>"""

        msg.attach(MIMEText(html, "html"))

        # ── Send (no attachments — logo loaded from public URL) ──
        server = smtplib.SMTP(smtp_server, smtp_port)
        server.starttls()
        server.login(smtp_user, smtp_pass)
        server.sendmail(smtp_from, to_email, msg.as_string())
        server.quit()
        print(f"[SMTP] Verification email sent to {to_email} successfully.")
        return True

    except Exception as e:
        print(f"[SMTP] Error sending email to {to_email}: {e}")
        print(f"[SMTP] VERIFICATION LINK: {verification_url}")
        return False


def send_password_reset_email_smtp(to_email: str, reset_url: str) -> bool:
    smtp_server = os.getenv("SMTP_SERVER", "smtp.gmail.com")
    smtp_port = int(os.getenv("SMTP_PORT", "587"))
    smtp_user = os.getenv("SMTP_USERNAME")
    smtp_pass = os.getenv("SMTP_PASSWORD")
    smtp_from = os.getenv("SMTP_FROM", smtp_user)

    if not smtp_user or not smtp_pass:
        print("[SMTP] SMTP credentials not set. Mocking email send.")
        print(f"[SMTP] PASSWORD RESET EMAIL to {to_email}: {reset_url}")
        return False

    try:
        msg = MIMEMultipart("alternative")
        msg["Subject"] = "Confirm Password Reset - AttendancePRO"
        msg["From"] = f"AttendancePRO <{smtp_from}>"
        msg["To"] = to_email

        html = f"""\
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1.0">
</head>
<body style="margin:0;padding:0;background:#0f172a;font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#0f172a;padding:32px 0;">
    <tr><td align="center">
      <table width="560" cellpadding="0" cellspacing="0"
             style="max-width:560px;width:100%;background:#1e293b;
                    border-radius:20px;border:1px solid #06b6d4;
                    box-shadow:0 8px 32px rgba(6,182,212,0.2);overflow:hidden;">

        <!-- ── HEADER ── -->
        <tr>
          <td align="center"
              style="background:linear-gradient(160deg,#0f172a 0%,#1a3a5c 100%);
                     padding:36px 40px 28px;">
            <img src="{LOGO_URL}"
                 alt="AttendancePRO"
                 width="90" height="90"
                 style="display:block;border-radius:18px;
                        box-shadow:0 0 24px rgba(6,182,212,0.45);
                        border:2.5px solid #06b6d4;" />
            <h1 style="margin:16px 0 4px;font-size:24px;font-weight:800;
                        color:#ffffff;letter-spacing:0.5px;">AttendancePRO</h1>
            <p style="margin:0;font-size:11px;color:#06b6d4;
                      letter-spacing:3px;text-transform:uppercase;">
              Automated Attendance System
            </p>
          </td>
        </tr>

        <!-- ── GRADIENT DIVIDER ── -->
        <tr>
          <td style="height:3px;
                     background:linear-gradient(90deg,#06b6d4,#3b82f6,#06b6d4);">
          </td>
        </tr>

        <!-- ── BODY ── -->
        <tr>
          <td align="center" style="padding:40px 40px 32px;">
            <h2 style="margin:0 0 14px;font-size:21px;font-weight:700;color:#f8fafc;">
              Confirm Password Reset
            </h2>
            <p style="margin:0 0 30px;font-size:15px;line-height:1.75;color:#94a3b8;">
              You requested a password reset for your
              <strong style="color:#06b6d4;">AttendancePRO</strong> account.<br>
              Click the button below to confirm the password change.
            </p>

            <!-- BUTTON -->
            <a href="{reset_url}"
               style="display:inline-block;padding:15px 44px;
                      font-size:16px;font-weight:700;color:#ffffff;
                      background:linear-gradient(135deg,#06b6d4,#0284c7);
                      border-radius:50px;text-decoration:none;
                      box-shadow:0 6px 20px rgba(6,182,212,0.45);
                      letter-spacing:0.4px;">
              Confirm Password Reset
            </a>

            <p style="margin:28px 0 0;font-size:12px;color:#475569;line-height:1.7;">
              This link is single-use and will expire once clicked.<br>
              If you did not request this, you can safely ignore this email and your password will remain unchanged.
            </p>
          </td>
        </tr>

        <!-- ── FOOTER ── -->
        <tr>
          <td align="center"
              style="background:#0f172a;padding:18px 40px;
                     border-top:1px solid #1e293b;">
            <p style="margin:0;font-size:11px;color:#334155;">
              &copy; 2026 AttendancePRO &nbsp;&bull;&nbsp;
              Automated Attendance Management System
            </p>
          </td>
        </tr>

      </table>
    </td></tr>
  </table>
</body>
</html>"""

        msg.attach(MIMEText(html, "html"))

        # ── Send ──
        server = smtplib.SMTP(smtp_server, smtp_port)
        server.starttls()
        server.login(smtp_user, smtp_pass)
        server.sendmail(smtp_from, to_email, msg.as_string())
        server.quit()
        print(f"[SMTP] Password reset email sent to {to_email} successfully.")
        return True

    except Exception as e:
        print(f"[SMTP] Error sending password reset email to {to_email}: {e}")
        print(f"[SMTP] RESET LINK: {reset_url}")
        return False
