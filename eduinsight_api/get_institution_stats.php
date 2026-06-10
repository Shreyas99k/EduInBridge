<?php
header('Content-Type: application/json');

// 1. Database Connection
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Database connection failed"]);
    exit();
}

// 2. Capture the institution_id from the Android request
$institution_id = isset($_GET['institution_id']) ? (int)$_GET['institution_id'] : 0;

if ($institution_id <= 0) {
    echo json_encode(["status" => "error", "message" => "Invalid Institution ID"]);
    exit();
}

// 3. Initialize Response
$response = [
    "status" => "success",
    "total_students" => 0,
    "total_teachers" => 0
];

/**
 * 4. Query to fetch counts
 * We count users where the role is 'student' or 'teacher' 
 * and they belong to this specific institution.
 */
$query = "SELECT 
            SUM(CASE WHEN role = 'student' THEN 1 ELSE 0 END) AS student_count,
            SUM(CASE WHEN role = 'teacher' THEN 1 ELSE 0 END) AS teacher_count
          FROM users 
          WHERE institution_id = ?";

$stmt = $conn->prepare($query);
$stmt->bind_param("i", $institution_id);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    // We map the results to the keys expected by your Android App
    $response['total_students'] = $row['student_count'] ? (int)$row['student_count'] : 0;
    $response['total_teachers'] = $row['teacher_count'] ? (int)$row['teacher_count'] : 0;
}

// 5. Return the clean JSON response
echo json_encode($response);

$stmt->close();
$conn->close();
?>