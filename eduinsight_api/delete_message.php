<?php
header('Content-Type: text/plain; charset=utf-8');
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) die("connection_error");

$message_id = $_POST['message_id'] ?? null;
$user_id = $_POST['user_id'] ?? null;
$type = $_POST['type'] ?? 'for_me'; // 'for_me' or 'for_everyone'

if (!$message_id || !$user_id) die("missing_parameters");

if ($type === 'for_everyone') {
    // Check if user is the sender
    $check = $conn->prepare("SELECT sender_id FROM mentorship_chats WHERE id = ?");
    $check->bind_param("i", $message_id);
    $check->execute();
    $row = $check->get_result()->fetch_assoc();

    if ($row && $row['sender_id'] == $user_id) {
        $sql = "UPDATE mentorship_chats SET is_deleted = 1, message = 'This message was deleted', image_url = NULL, audio_url = NULL WHERE id = ?";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("i", $message_id);
        if ($stmt->execute()) echo "success";
        else echo "db_error";
    } else {
        echo "unauthorized";
    }
} else {
    // Delete for me
    $sql = "UPDATE mentorship_chats SET deleted_by = ? WHERE id = ?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ii", $user_id, $message_id);
    if ($stmt->execute()) echo "success";
    else echo "db_error";
}
$conn->close();
?>