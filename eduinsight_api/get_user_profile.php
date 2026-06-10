<?php
header('Content-Type: application/json');

// 1. Database Connection
$conn = new mysqli("localhost", "root", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Database connection failed"]);
    exit();
}

// 2. Get User ID from Request
$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;

if ($user_id > 0) {
    try {
        // Prepare statement to prevent SQL injection
        $stmt = $conn->prepare("SELECT name, email, mobile, role FROM users WHERE id = ?");
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $result = $stmt->get_result();
        
        if ($row = $result->fetch_assoc()) {
            // Handle the mobile number field carefully
            $raw_mobile = $row['mobile'];
            
            // This logic checks if the value is actually set and not just a placeholder
            if ($raw_mobile !== null && strlen(trim($raw_mobile)) > 0 && strtolower(trim($raw_mobile)) !== "null") {
                $display_mobile = trim($raw_mobile);
            } else {
                $display_mobile = "Not Provided";
            }

            echo json_encode([
                "status" => "success",
                "name" => $row['name'],
                "email" => $row['email'],
                "mobile" => $display_mobile,
                "role" => strtoupper($row['role'] ?? 'USER') // Ensure role is uppercase for UI consistency
            ]);
        } else {
            echo json_encode(["status" => "error", "message" => "User record not found"]);
        }
        $stmt->close();
    } catch (Exception $e) {
        echo json_encode(["status" => "error", "message" => "Server error: " . $e->getMessage()]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Valid User ID is required"]);
}

$conn->close();
?>