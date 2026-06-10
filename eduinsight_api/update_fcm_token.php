<?php
header('Content-Type: application/json');
include 'db.php';

$user_id = $_POST['user_id'] ?? null;
$token = $_POST['fcm_token'] ?? '';

if ($user_id && !empty($token)) {
    $stmt = $conn->prepare("UPDATE users SET fcm_token = ? WHERE id = ?");
    $stmt->bind_param("si", $token, $user_id);
    if ($stmt->execute()) {
        echo json_encode(["status" => "success"]);
    } else {
        echo json_encode(["status" => "error", "message" => $conn->error]);
    }
    $stmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Missing parameters"]);
}
$conn->close();
?>