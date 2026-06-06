import smtplib

EMAIL = "attendancepro.app@gmail.com"
APP_PASSWORD = "utrx crzu qtco yvrf"

server = smtplib.SMTP("smtp.gmail.com", 587)
server.starttls()
server.login(EMAIL, APP_PASSWORD)

print("SMTP Login Successful!")

server.quit()