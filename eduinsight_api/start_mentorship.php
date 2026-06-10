<?php
header('Content-Type: application/json; charset=utf-8');
ini_set('display_errors', 0);
error_reporting(E_ALL);

$conn = new mysqli("localhost", "root", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    die(json_encode(array("status" => "error", "message" => "Connection failed")));
}

$conn->set_charset("utf8mb4");

$doubt_id = isset($_POST['doubt_id']) ? intval($_POST['doubt_id']) : 0;

if ($doubt_id > 0) {
    // FIXED: Removed 'updated_at' because it's missing in your database
    $sql = "UPDATE doubts SET is_mentorship = 1 WHERE id = ?";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $doubt_id);
    
    if ($stmt->execute()) {
        echo json_encode(array("status" => "success", "message" => "Mentorship activated"));
    } else {
        echo json_encode(array("status" => "error", "message" => "SQL Error"));
    }
    $stmt->close();
} else {
    echo json_encode(array("status" => "error", "message" => "Invalid ID"));
}
$conn->close();
?>