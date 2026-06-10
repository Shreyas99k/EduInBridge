<?php
header('Content-Type: application/json');

// Database Connection
$conn = new mysqli("localhost", "root", "YOUR_DB_PASSWORD", "DB_NAME");

if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Connection failed"]);
    exit();
}

$user_id = $_POST['user_id'];
$mobile = $_POST['mobile'];

if (empty($user_id) || empty($mobile)) {
    echo json_encode(["status" => "error", "message" => "Missing parameters"]);
    exit();
}

// Update the mobile number for the specific user
$sql = "UPDATE users SET mobile = '$mobile' WHERE id = '$user_id'";

if ($conn->query($sql) === TRUE) {
    echo "success"; // Android app looks for this exact string
} else {
    echo json_encode(["status" => "error", "message" => "Database error: " . $conn->error]);
}

$conn->close();
?>
