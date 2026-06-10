<?php
header('Content-Type: application/json');
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

$student_id = $_GET['student_id'];
$today = date('Y-m-d');

// 1. Check if streak has expired (more than 3 days inactivity)
$res = $conn->query("SELECT streak_count, last_activity_date FROM users WHERE id = $student_id");
$user = $res->fetch_assoc();

$streak = $user['streak_count'];
$last_date = $user['last_activity_date'];

if ($last_date != null) {
    $diff = (strtotime($today) - strtotime($last_date)) / (60 * 60 * 24);
    if ($diff > 3) {
        $streak = 0; // Reset expired streak
        $conn->query("UPDATE users SET streak_count = 0 WHERE id = $student_id");
    }
}

// 2. Get doubt stats
$solved = $conn->query("SELECT COUNT(*) as c FROM doubts WHERE student_id = $student_id AND status = 'solved'")->fetch_assoc()['c'];
$pending = $conn->query("SELECT COUNT(*) as c FROM doubts WHERE student_id = $student_id AND status = 'pending'")->fetch_assoc()['c'];

echo json_encode([
    "solved" => (int)$solved,
    "pending" => (int)$pending,
    "streak" => (int)$streak
]);

$conn->close();
?>