<?php
header('Content-Type: application/json');
// Disable error reporting for clean JSON
error_reporting(0);
ini_set('display_errors', 0);

$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Database Connection Failed"]));
}

$user_id = $_GET['user_id'] ?? 0;

if ($user_id > 0) {
    // Robust SQL to fetch stats
    $sql = "SELECT u.streak, i.name as institution_name, d.name as department_name,
            (SELECT COUNT(*) FROM doubts WHERE student_id = u.id AND status = 'solved') as solved,
            (SELECT COUNT(*) FROM doubts WHERE student_id = u.id AND status = 'pending') as pending,
            (SELECT COUNT(*) FROM doubts WHERE student_id = u.id AND has_new_message = 1) as unread_messages
            FROM users u
            LEFT JOIN users i ON u.institution_id = i.id
            LEFT JOIN departments d ON u.department_id = d.id
            WHERE u.id = ?";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($row = $result->fetch_assoc()) {
        echo json_encode([
            "status" => "success",
            "streak" => (int)$row['streak'],
            "institution_name" => $row['institution_name'] ?? "Independent Learner",
            "department_name" => $row['department_name'] ?? "",
            "solved" => (int)$row['solved'],
            "pending" => (int)$row['pending'],
            "unread_messages" => (int)$row['unread_messages']
        ]);
    } else {
        echo json_encode(["status" => "error", "message" => "User ID $user_id not found"]);
    }
    $stmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Invalid User ID"]);
}
$conn->close();
?>