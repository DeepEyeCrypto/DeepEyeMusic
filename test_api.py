import urllib.request
import json

url = "https://www.youtube.com/youtubei/v1/player"
headers = {"Content-Type": "application/json"}
data = {
    "context": {
        "client": {
            "clientName": "TVHTML5",
            "clientVersion": "7.20230405.08.01"
        }
    },
    "videoId": "ZvXN0TcZLfQ"
}
req = urllib.request.Request(url, headers=headers, data=json.dumps(data).encode('utf-8'))
try:
    response = urllib.request.urlopen(req)
    res = json.loads(response.read())
    streaming_data = res.get("streamingData", {})
    formats = streaming_data.get("formats", [])
    adaptive = streaming_data.get("adaptiveFormats", [])
    
    print("Formats:", len(formats))
    for f in formats:
        print(f.get("itag"), "Cipher:", "signatureCipher" in f, "Url:", "url" in f)
        
    print("Adaptive:", len(adaptive))
    for f in adaptive:
        print(f.get("itag"), "Cipher:", "signatureCipher" in f, "Url:", "url" in f)
except Exception as e:
    print(e)
