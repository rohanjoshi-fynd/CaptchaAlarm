from http.server import HTTPServer, SimpleHTTPRequestHandler
import socket

class APKHandler(SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/' or self.path == '':
            self.send_response(200)
            self.send_header('Content-Type', 'text/html')
            self.end_headers()
            self.wfile.write(b'''<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Captcha Alarm</title>
    <style>
        body { font-family: -apple-system, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; background: #1a1a2e; color: white; }
        .card { text-align: center; padding: 40px; }
        h1 { font-size: 28px; margin-bottom: 8px; }
        p { color: #aaa; margin-bottom: 32px; }
        a { display: inline-block; padding: 18px 48px; background: #6200ee; color: white; text-decoration: none; border-radius: 12px; font-size: 20px; font-weight: bold; }
    </style>
</head>
<body>
    <div class="card">
        <h1>Captcha Alarm</h1>
        <p>Tap below to download and install</p>
        <a href="/CaptchaAlarm.apk" download>Download APK</a>
    </div>
</body>
</html>''')
        elif self.path == '/CaptchaAlarm.apk':
            with open('CaptchaAlarm.apk', 'rb') as f:
                data = f.read()
            self.send_response(200)
            self.send_header('Content-Type', 'application/vnd.android.package-archive')
            self.send_header('Content-Disposition', 'attachment; filename="CaptchaAlarm.apk"')
            self.send_header('Content-Length', str(len(data)))
            self.end_headers()
            self.wfile.write(data)
        else:
            self.send_error(404)

import subprocess
try:
    ip = subprocess.check_output(['ipconfig', 'getifaddr', 'en0'], text=True).strip()
except Exception:
    ip = '0.0.0.0'
port = 8080
print(f"\n  Open this on your phone:\n")
print(f"  http://{ip}:{port}\n")
print(f"  (Make sure phone is on the same Wi-Fi)\n")
HTTPServer(('0.0.0.0', port), APKHandler).serve_forever()
