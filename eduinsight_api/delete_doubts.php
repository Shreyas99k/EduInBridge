<?php
header('Content-Type: text/plain; charset=utf-8');

$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    die("connection_failed");
}

$ids = isset($_POST['ids']) ? $_POST['ids'] : '';

if (empty($ids)) {
    die("no_ids_provided");
}

// 1. Security: Remove any characters except numbers and commas
$safe_ids = preg_replace('/[^0-9,]/', '', $ids);
$safe_ids = trim($safe_ids, ',');

if (empty($safe_ids)) {
    die("invalid_ids");
}

// 2. Delete from both message tables to ensure cleanup
$conn->query("DELETE FROM messages WHERE doubt_id IN ($safe_ids)");
$conn->query("DELETE FROM mentorship_chats WHERE doubt_id IN ($safe_ids)");

// 3. Delete the doubts
$sqlDoubts = "DELETE FROM doubts WHERE id IN ($safe_ids)";

if ($conn->query($sqlDoubts) === TRUE) {
    echo "success";
} else {
    echo "error: " . $conn->error;
}

$conn->close();
?>