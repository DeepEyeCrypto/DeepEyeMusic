import java.net.URL
import java.net.HttpURLConnection

fun main() {
    val url = URL("https://www.youtube.com/youtubei/v1/player")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json")
    conn.doOutput = true
    
    val payload = """
    {
        "context": {
            "client": {
                "clientName": "ANDROID",
                "clientVersion": "19.29.37",
                "androidSdkVersion": 33
            }
        },
        "videoId": "ZvXN0TcZLfQ",
        "playbackContext": {
            "contentPlaybackContext": {
                "signatureTimestamp": 19890
            }
        }
    }
    """.trimIndent()
    
    conn.outputStream.write(payload.toByteArray())
    
    try {
        val response = conn.inputStream.bufferedReader().readText()
        println(response.take(1000))
        if (response.contains("signatureCipher")) {
            println("HAS CIPHER")
        }
        if (response.contains("\"url\"")) {
            println("HAS URL")
        }
    } catch (e: Exception) {
        println("ERROR: ${conn.responseCode} ${conn.responseMessage}")
    }
}
