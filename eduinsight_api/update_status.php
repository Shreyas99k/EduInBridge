<?php
header('Content-Type: text/plain; charset=utf-8');

$conn = new mysqli("localhost", "root", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    die("error");
}

$user_id = $_POST['user_id'] ?? null;
$status = $_POST['status'] ?? '0'; // 1 for online, 0 for offline

if (!$user_id) {
    die("missing_parameters");
}

// Check if column exists, if not add it
$check_col = $conn->query("SHOW COLUMNS FROM users LIKE 'is_online'");
if ($check_col->num_rows == 0) {
    $conn->query("ALTER TABLE users ADD COLUMN is_online TINYINT(1) DEFAULT 0");
}

$stmt = $conn->prepare("UPDATE users SET is_online = ? WHERE id = ?");
$stmt->bind_param("ii", $status, $user_id);

if ($stmt->execute()) {
    echo "success";
} else {
    echo "error";
}

$stmt->close();
$conn->close();
?>
