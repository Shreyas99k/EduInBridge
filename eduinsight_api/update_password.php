<?php
header('Content-Type: application/json');

$conn = new mysqli("localhost", "root", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

// --- HIGHLIGHT: DATA FROM APP ---
$identity = $_POST['identity']; // The Email or Mobile used
$new_password = $_POST['password'];

if (empty($identity) || empty($new_password)) {
    echo json_encode(["status" => "error", "message" => "Invalid request"]);
    exit();
}

// --- HIGHLIGHT: DYNAMIC UPDATE QUERY ---
// This updates the password where either email OR mobile matches
$sql = "UPDATE users SET password = '$new_password' WHERE email = '$identity' OR mobile = '$identity'";

if ($conn->query($sql) === TRUE) {
    // Check if any row was actually changed
    if ($conn->affected_rows > 0) {
        echo json_encode(["status" => "success", "message" => "Password updated successfully"]);
    } else {
        echo json_encode(["status" => "error", "message" => "User not found or password same"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Database error: " . $conn->error]);
}

$conn->close();
?>