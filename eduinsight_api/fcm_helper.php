<?php
/**
 * Generates an OAuth2 Access Token using the service account JSON file.
 * This is required for the Firebase Cloud Messaging (FCM) v1 API.
 */
function getAccessToken($serviceAccountFile)
{
    if (!file_exists($serviceAccountFile)) {
        return null;
    }

    $data = json_decode(file_get_contents($serviceAccountFile), true);
    $privateKey = $data['private_key'];
    $clientEmail = $data['client_email'];
    $tokenUri = 'https://oauth2.googleapis.com/token';

    // Create JWT Header
    $header = json_encode(['alg' => 'RS256', 'typ' => 'JWT']);

    // Create JWT Payload
    $now = time();
    $payload = json_encode([
        'iss' => $clientEmail,
        'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
        'aud' => $tokenUri,
        'iat' => $now,
        'exp' => $now + 3600 // Token valid for 1 hour
    ]);

    // Encode Header and Payload to Base64Url
    $base64UrlHeader = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($header));
    $base64UrlPayload = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($payload));

    // Sign the JWT
    $signatureInput = $base64UrlHeader . "." . $base64UrlPayload;
    openssl_sign($signatureInput, $signature, $privateKey, 'SHA256');
    $base64UrlSignature = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($signature));

    $jwt = $base64UrlHeader . "." . $base64UrlPayload . "." . $base64UrlSignature;

    // Exchange the signed JWT for an Access Token
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $tokenUri);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/x-www-form-urlencoded']);
    curl_setopt($ch, CURLOPT_POSTFIELDS, "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=" . $jwt);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false); 

    $result = curl_exec($ch);
    $tokenData = json_decode($result, true);
    curl_close($ch);

    return isset($tokenData['access_token']) ? $tokenData['access_token'] : null;
}

/**
 * Sends a push notification via FCM v1 API.
 * Includes all necessary metadata for the app to correctly handle the notification.
 */
function sendFCM($token, $title, $message, $type, $target_id, $sender_id = "-1", $sender_role = "", $target_role = "all")
{
    $serviceAccountFile = 'service-account.json';

    if (!file_exists($serviceAccountFile)) {
        return "Error: service-account.json not found on server.";
    }

    $accessToken = getAccessToken($serviceAccountFile);
    if (!$accessToken) {
        return "Error: Could not retrieve access token.";
    }

    $data = json_decode(file_get_contents($serviceAccountFile), true);
    $projectId = $data['project_id'];

    $url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send";

    // Build the final FCM v1 payload (Data-only for consistent background processing)
    $payload = [
        'message' => [
            'token' => $token,
            'data' => [
                'title' => $title,
                'message' => $message,
                'type' => $type,
                'target_id' => (string) $target_id,
                'sender_id' => (string) $sender_id,
                'sender_role' => (string) $sender_role,
                'target_role' => (string) $target_role
            ],
            'android' => [
                'priority' => 'high'
            ]
        ]
    ];

    $headers = [
        'Authorization: Bearer ' . $accessToken,
        'Content-Type: application/json'
    ];

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));

    $result = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    // Logging for Debugging
    $log_entry = date('Y-m-d H:i:s') . " | Type: $type | To: $token | Status: $http_code | Resp: $result\n";
    file_put_contents('fcm_log.txt', $log_entry, FILE_APPEND);

    return $result;
}
?>