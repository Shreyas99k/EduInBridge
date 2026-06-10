<?php
header('Content-Type: application/json');require_once 'fcm_helper.php'; 

// 1. DATABASE CONFIGURATION
$host = "localhost";
$user = "root";
$pass = "YOUR_DB_PASSWORD"; 
$db   = "YOUR_DB_NAME"; 

$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Database connection failed"]);
    exit();
}

// 2. GET DATA FROM ANDROID APP
$title       = $_POST['title']     ?? 'Announcement';
$message     = $_POST['message']   ?? '';
$target_role = $_POST['target']    ?? 'all'; 
$type        = $_POST['type']      ?? 'broadcast';
$sender_id   = $_POST['sender_id'] ?? '-1';
$sender_role = $_POST['sender_role'] ?? 'institution';

// 3. FETCH TOKENS (Using DISTINCT to prevent duplicates)
if ($target_role === 'all') {
    $sql = "SELECT DISTINCT fcm_token FROM users WHERE fcm_token IS NOT NULL AND fcm_token != ''";
} else {
    $role = $conn->real_escape_string($target_role);
    $sql = "SELECT DISTINCT fcm_token FROM users WHERE role = '$role' AND fcm_token IS NOT NULL AND fcm_token != ''";
}

$result = $conn->query($sql);
$successCount = 0;

if ($result && $result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        $token = $row['fcm_token'];
        
        // 4. CALL YOUR FCM HELPER WITH THE TARGET ROLE
        // This passes the target ('student', 'teacher', or 'all') to the helper
        sendFCM($token, $title, $message, $type, "0", $sender_id, $sender_role, $target_role);
        $successCount++;
    }
}

// 5. RETURN RESPONSE
echo json_encode([
    "status" => "success",
    "message" => "Announcement sent to $successCount unique devices."
]);

$conn->close();
?>