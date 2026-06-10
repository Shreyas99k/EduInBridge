<?php
header('Content-Type: text/plain; charset=utf-8');
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) die("connection_error");

$message_id = $_POST['message_id'] ?? null;
$user_id = $_POST['user_id'] ?? null;
$new_text = $_POST['new_text'] ?? '';

if (!$message_id || !$user_id || empty($new_text)) die("missing_parameters");

// Check if user is the sender and message is not deleted
$check = $conn->query("SELECT sender_id, is_deleted FROM mentorship_chats WHERE id = '$message_id'");
$row = $check->fetch_assoc();

if ($row && $row['sender_id'] == $user_id && $row['is_deleted'] == 0) {
    $sql = "UPDATE mentorship_chats SET message = ?, is_edited = 1, image_url = NULL, audio_url = NULL WHERE id = ?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("si", $new_text, $message_id);
    if ($stmt->execute()) echo "success";
    else echo "db_error";
    $stmt->close();
} else {
    echo "unauthorized";
}
$conn->close();
?>