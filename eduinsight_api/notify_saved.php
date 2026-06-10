<?php
$conn = new mysqli("localhost", "root", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");
$message_id = $_POST['message_id'];

if (!empty($message_id)) {
    $conn->query("UPDATE messages SET is_saved = 1 WHERE id = $message_id");
    echo "success";
}
$conn->close();
?>