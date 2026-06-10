<?php
header('Content-Type: application/json');
include 'db.php';

$email = isset($_GET['email']) ? $_GET['email'] : '';

if (!empty($email)) {
    $stmt = $conn->prepare("SELECT id FROM users WHERE email = ?");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows > 0) {
        echo json_encode(["status" => "exists"]);
    } else {
        echo json_encode(["status" => "not_exists"]);
    }
    $stmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Email is required"]);
}
$conn->close();
?>