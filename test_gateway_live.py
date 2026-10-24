import urllib.request
import json

# 1. Login to Gateway to get session JWT
login_req = urllib.request.Request(
    'http://127.0.0.1:8000/api/v1/auth/login',
    data=json.dumps({'email': 'soc-responder@enterprise.local', 'password': 'pass'}).encode(),
    headers={'Content-Type': 'application/json'}
)
token = json.loads(urllib.request.urlopen(login_req).read())['access_token']

# 2. Query Endpoints via Gateway
ep_req = urllib.request.Request(
    'http://127.0.0.1:8000/api/v1/endpoints',
    headers={'Authorization': f'Bearer {token}'}
)
endpoints = json.loads(urllib.request.urlopen(ep_req).read())
print(f"Gateway returned {len(endpoints)} live endpoints from Sophos Central!")
for ep in endpoints[:8]:
    print(f" • {ep['hostname']} | OS: {ep['os']} | Health: {ep['health']} | IPs: {ep['ip_addresses']}")
