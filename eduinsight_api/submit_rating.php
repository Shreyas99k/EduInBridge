<?php
header('Content-Type: application/json; charset=utf-8');
// Disable direct error display to avoid breaking the response format
ini_set('display_errors', 0);
error_reporting(E_ALL);

// Database connection
$conn = new mysqli("localhost", "root", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    die(json_encode(array("status" => "error", "message" => "Connection failed")));
}

$conn->set_charset("utf8mb4");

// Get data from POST request (sent by HistoryAdapter.java)
$doubt_id = isset($_POST['doubt_id']) ? intval($_POST['doubt_id']) : 0;
$rating   = isset($_POST['rating']) ? intval($_POST['rating']) : 0;

if ($doubt_id > 0 && $rating >= 1 && $rating <= 5) {
    
    // Using Prepared Statement for security
    $sql = "UPDATE doubts SET rating = ? WHERE id = ?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ii", $rating, $doubt_id);
    
    if ($stmt->execute()) {
        // Return "success" so the Android app updates the UI colors
        echo "success"; 
    } else {
        echo "error";
    }
    
    $stmt->close();
} else {
    echo "invalid_data";
}

$conn->close();
?>