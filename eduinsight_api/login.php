<?php
error_reporting(0);
ini_set('display_errors', 0);
header('Content-Type: application/json');

include 'db.php';

$response = ["status" => "error", "message" => "Unknown error"];

// 2. Get POST data
$email = $_POST['email'] ?? "";
$password = $_POST['password'] ?? "";
$fcm_token = $_POST['fcm_token'] ?? "";

if (empty($email) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "Email and Password required"]);
    exit();
}

// 3. Authenticate with JOIN to get Institution and Department names
$sql = "SELECT u.id, u.name, u.email, u.role, u.status, u.institution_id, u.department_id,
               i.name as institution_name, d.name as department_name
        FROM users u
        LEFT JOIN users i ON u.institution_id = i.id
        LEFT JOIN departments d ON u.department_id = d.id
        WHERE u.email = ? AND u.password = ?";

$stmt = $conn->prepare($sql);
$stmt->bind_param("ss", $email, $password);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    $user_id = $row['id'];
    
    // Update FCM if needed
    if (!empty($fcm_token)) {
        $up = $conn->prepare("UPDATE users SET fcm_token = ? WHERE id = ?");
        $up->bind_param("si", $fcm_token, $user_id);
        $up->execute();
    }

    $response = [
        "status" => "success",
        "user_id" => (int)$user_id,
        "name" => $row['name'],
        "email" => $row['email'],
        "role" => $row['role'],
        "user_status" => $row['status'],
        "institution_id" => $row['institution_id'] ? (int)$row['institution_id'] : -1,
        "institution_name" => $row['institution_name'] ?? "",
        "department_name" => $row['department_name'] ?? ""
    ];
} else {
    $response = ["status" => "error", "message" => "Invalid Email or Password"];
}

echo json_encode($response);
$conn->close();
?>